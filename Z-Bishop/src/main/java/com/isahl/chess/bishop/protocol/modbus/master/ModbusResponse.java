/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.master;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;

/** Modbus 响应 */
public class ModbusResponse extends ModbusMessage {

  private final int exceptionCode;
  private final Integer functionCodeOverride;

  public ModbusResponse(int unitId, ModbusFunction function, byte[] data) {
    super(unitId, function, data);
    this.exceptionCode = -1;
    this.functionCodeOverride = null;
  }

  public ModbusResponse(int unitId, ModbusFunction function, byte[] data, int exceptionCode) {
    super(unitId, function, data);
    this.exceptionCode = exceptionCode;
    this.functionCodeOverride = null;
  }

  private ModbusResponse(
      int unitId,
      ModbusFunction function,
      byte[] data,
      int exceptionCode,
      int functionCodeOverride) {
    super(unitId, function, data);
    this.exceptionCode = exceptionCode;
    this.functionCodeOverride = functionCodeOverride;
  }

  public int getExceptionCode() {
    return exceptionCode;
  }

  public int getFunctionCode() {
    return functionCodeOverride != null ? functionCodeOverride : getFunction().getCode();
  }

  @Override
  public boolean isException() {
    return exceptionCode >= 0 || ModbusFunction.isException(getFunctionCode());
  }

  /** 创建异常响应 */
  public static ModbusResponse exception(int unitId, ModbusFunction function, int exceptionCode) {
    int exceptionFunctionCode = function.getCode() | 0x80;
    return new ModbusResponse(
        unitId,
        ModbusFunction.EXCEPTION,
        new byte[] {(byte) exceptionCode},
        exceptionCode,
        exceptionFunctionCode);
  }
}
