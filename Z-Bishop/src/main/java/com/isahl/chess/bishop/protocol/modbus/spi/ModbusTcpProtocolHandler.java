/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.spi;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;
import com.isahl.chess.bishop.protocol.modbus.tcp.ModbusTcpCodec;
import com.isahl.chess.bishop.protocol.modbus.tcp.ModbusTcpCodec.ModbusTcpMessage;
import com.isahl.chess.bishop.protocol.spi.ProtocolContext;
import com.isahl.chess.bishop.protocol.spi.ProtocolHandler;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;

/**
 * Modbus TCP 协议处理器
 *
 * @author william.d.zk
 * @since 1.1.2
 */
public class ModbusTcpProtocolHandler implements ProtocolHandler {

  private static final byte[] MODBUS_SIGNATURE = {0x00, 0x00};
  private int _transactionId = 0;

  @Override
  public boolean supports(IProtocol message) {
    // Modbus TCP 消息以 0x00 0x00 开头（事务标识符）
    byte[] payload = message.payload();
    if (payload == null || payload.length < 2) {
      return false;
    }
    // 简单的启发式检查：前两个字节通常是事务标识符，MBAP 头长度为 7
    if (payload.length < 7) {
      return false;
    }
    // 检查协议标识符为 0x0000（Modbus）
    return payload[2] == 0x00 && payload[3] == 0x00;
  }

  @Override
  public void handle(IProtocol message, ProtocolContext context) {
    ByteBuf buffer = ByteBuf.wrap(message.payload());
    Object decoded = decode(buffer);
    if (decoded != null) {
      context.setAttribute("modbusMessage", decoded);
    }
  }

  @Override
  public String getName() {
    return "ModbusTCP";
  }

  @Override
  public String getDescription() {
    return "Modbus TCP protocol handler";
  }

  @Override
  public int getPriority() {
    return 500;
  }

  // ==================== 编码解码方法 ====================

  public byte[] getProtocolSignature() {
    return MODBUS_SIGNATURE;
  }

  /** 解码消息 */
  public Object decode(ByteBuf buffer) {
    if (buffer.readableBytes() < 6) {
      return null;
    }

    int frameLength = ModbusTcpCodec.getFrameLength(buffer);
    if (frameLength < 0 || buffer.readableBytes() < frameLength) {
      return null;
    }

    return ModbusTcpCodec.decode(buffer);
  }

  /** 编码消息 */
  public byte[] encode(Object message) {
    if (!(message instanceof ModbusMessage)) {
      throw new IllegalArgumentException("Message must be a ModbusMessage");
    }

    ModbusMessage modbusMessage = (ModbusMessage) message;
    int transactionId = (_transactionId++) & 0xFFFF;

    if (message instanceof ModbusTcpMessage) {
      transactionId = ((ModbusTcpMessage) message).getTransactionId();
    }

    ByteBuf buffer = ModbusTcpCodec.encode(modbusMessage, transactionId);

    byte[] result = new byte[buffer.readableBytes()];
    buffer.get(result);

    return result;
  }

  // ==================== 快捷方法 ====================

  public ModbusMessage createReadHoldingRegisters(int unitId, int startAddress, int quantity) {
    byte[] data = new byte[4];
    data[0] = (byte) ((startAddress >> 8) & 0xFF);
    data[1] = (byte) (startAddress & 0xFF);
    data[2] = (byte) ((quantity >> 8) & 0xFF);
    data[3] = (byte) (quantity & 0xFF);

    return new ModbusMessage(unitId, ModbusFunction.READ_HOLDING_REGISTERS, data);
  }

  public ModbusMessage createWriteSingleRegister(int unitId, int address, int value) {
    byte[] data = new byte[4];
    data[0] = (byte) ((address >> 8) & 0xFF);
    data[1] = (byte) (address & 0xFF);
    data[2] = (byte) ((value >> 8) & 0xFF);
    data[3] = (byte) (value & 0xFF);

    return new ModbusMessage(unitId, ModbusFunction.WRITE_SINGLE_REGISTER, data);
  }

  public ModbusMessage createWriteMultipleRegisters(int unitId, int startAddress, int[] values) {
    byte[] data = new byte[5 + values.length * 2];
    data[0] = (byte) ((startAddress >> 8) & 0xFF);
    data[1] = (byte) (startAddress & 0xFF);
    data[2] = (byte) ((values.length >> 8) & 0xFF);
    data[3] = (byte) (values.length & 0xFF);
    data[4] = (byte) (values.length * 2);

    for (int i = 0; i < values.length; i++) {
      data[5 + i * 2] = (byte) ((values[i] >> 8) & 0xFF);
      data[5 + i * 2 + 1] = (byte) (values[i] & 0xFF);
    }

    return new ModbusMessage(unitId, ModbusFunction.WRITE_MULTIPLE_REGISTERS, data);
  }

  public ModbusMessage createReadCoils(int unitId, int startAddress, int quantity) {
    byte[] data = new byte[4];
    data[0] = (byte) ((startAddress >> 8) & 0xFF);
    data[1] = (byte) (startAddress & 0xFF);
    data[2] = (byte) ((quantity >> 8) & 0xFF);
    data[3] = (byte) (quantity & 0xFF);

    return new ModbusMessage(unitId, ModbusFunction.READ_COILS, data);
  }
}
