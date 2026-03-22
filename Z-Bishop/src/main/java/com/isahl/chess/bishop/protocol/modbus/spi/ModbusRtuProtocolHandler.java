/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.spi;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;
import com.isahl.chess.bishop.protocol.modbus.rtu.ModbusRtuCodec;
import com.isahl.chess.bishop.protocol.spi.ProtocolContext;
import com.isahl.chess.bishop.protocol.spi.ProtocolHandler;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;

/**
 * Modbus RTU 协议处理器
 *
 * @author william.d.zk
 * @since 1.1.2
 */
public class ModbusRtuProtocolHandler implements ProtocolHandler {

  @Override
  public boolean supports(IProtocol message) {
    // RTU 消息以 CRC 校验结束，这里简单检查长度
    byte[] payload = message.payload();
    if (payload == null || payload.length < 4) {
      return false;
    }
    // RTU 帧最小长度为 4 字节（地址 + 功能码 + CRC）
    // 检查 CRC 位置（最后 2 字节）
    return true; // 简化处理，实际应该验证 CRC
  }

  @Override
  public void handle(IProtocol message, ProtocolContext context) {
    ByteBuf buffer = ByteBuf.wrap(message.payload());
    Object decoded = decode(buffer);
    if (decoded != null) {
      context.setAttribute("modbusRtuMessage", decoded);
    }
  }

  @Override
  public String getName() {
    return "ModbusRTU";
  }

  @Override
  public String getDescription() {
    return "Modbus RTU protocol handler";
  }

  @Override
  public int getPriority() {
    return 400; // 低于 TCP
  }

  // ==================== 编码解码方法 ====================

  public byte[] getProtocolSignature() {
    return null; // RTU 无固定签名
  }

  /** 解码消息 */
  public Object decode(ByteBuf buffer) {
    return ModbusRtuCodec.decode(buffer);
  }

  /** 编码消息 */
  public byte[] encode(Object message) {
    if (!(message instanceof ModbusMessage)) {
      throw new IllegalArgumentException("Message must be a ModbusMessage");
    }

    ModbusMessage modbusMessage = (ModbusMessage) message;
    ByteBuf buffer = ModbusRtuCodec.encode(modbusMessage);

    byte[] result = new byte[buffer.readableBytes()];
    buffer.get(result);

    return result;
  }

  // ==================== 快捷方法 ====================

  /** 创建读保持寄存器请求 */
  public ModbusMessage createReadHoldingRegisters(int unitId, int startAddress, int quantity) {
    byte[] data = new byte[4];
    data[0] = (byte) ((startAddress >> 8) & 0xFF);
    data[1] = (byte) (startAddress & 0xFF);
    data[2] = (byte) ((quantity >> 8) & 0xFF);
    data[3] = (byte) (quantity & 0xFF);

    return new ModbusMessage(unitId, ModbusFunction.READ_HOLDING_REGISTERS, data);
  }

  /** 创建写单个寄存器请求 */
  public ModbusMessage createWriteSingleRegister(int unitId, int address, int value) {
    byte[] data = new byte[4];
    data[0] = (byte) ((address >> 8) & 0xFF);
    data[1] = (byte) (address & 0xFF);
    data[2] = (byte) ((value >> 8) & 0xFF);
    data[3] = (byte) (value & 0xFF);

    return new ModbusMessage(unitId, ModbusFunction.WRITE_SINGLE_REGISTER, data);
  }
}
