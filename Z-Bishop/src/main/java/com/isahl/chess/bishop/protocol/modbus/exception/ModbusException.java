/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.exception;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusExceptionCode;

/** Modbus 协议异常 */
public class ModbusException extends RuntimeException {

  private final ModbusFunction function;
  private final ModbusExceptionCode exceptionCode;

  public ModbusException(ModbusFunction function, ModbusExceptionCode exceptionCode) {
    super(
        String.format(
            "Modbus exception: function=0x%02X, exception=0x%02X (%s)",
            function.getCode(), exceptionCode.getCode(), exceptionCode.getDescription()));
    this.function = function;
    this.exceptionCode = exceptionCode;
  }

  public ModbusException(String message) {
    super(message);
    this.function = null;
    this.exceptionCode = null;
  }

  public ModbusFunction getFunction() {
    return function;
  }

  public ModbusExceptionCode getExceptionCode() {
    return exceptionCode;
  }

  public int getFunctionCode() {
    return function != null ? function.getCode() : -1;
  }

  public int getExceptionCodeValue() {
    return exceptionCode != null ? exceptionCode.getCode() : -1;
  }

  public boolean isExceptionResponse() {
    return true;
  }
}
