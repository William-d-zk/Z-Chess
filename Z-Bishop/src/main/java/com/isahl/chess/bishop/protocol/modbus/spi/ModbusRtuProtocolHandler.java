/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.spi;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;
import com.isahl.chess.bishop.protocol.modbus.rtu.ModbusRtuCodec;
import com.isahl.chess.bishop.protocol.spi.ProtocolHandler;
import com.isahl.chess.king.base.content.ByteBuf;

/** Modbus RTU 协议处理器 */
public class ModbusRtuProtocolHandler implements ProtocolHandler {

  @Override
  public String getProtocolName() {
    return "ModbusRTU";
  }

  @Override
  public String getProtocolVersion() {
    return "1.0.0";
  }

  @Override
  public byte[] getProtocolSignature() {
    return null; // RTU 无固定签名
  }

  @Override
  public Object decode(ByteBuf buffer) {
    return ModbusRtuCodec.decode(buffer);
  }

  @Override
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

  @Override
  public int getPriority() {
    return 400; // 低于 TCP
  }

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
