/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.model;

/**
 * Modbus 异常码
 *
 * @author william.d.zk
 */
public enum ModbusExceptionCode {
  ILLEGAL_FUNCTION(0x01, "Illegal Function"),
  ILLEGAL_DATA_ADDRESS(0x02, "Illegal Data Address"),
  ILLEGAL_DATA_VALUE(0x03, "Illegal Data Value"),
  SERVER_DEVICE_FAILURE(0x04, "Server Device Failure"),
  ACKNOWLEDGE(0x05, "Acknowledge"),
  SERVER_DEVICE_BUSY(0x06, "Server Device Busy"),
  NEGATIVE_ACKNOWLEDGEMENT(0x07, "Negative Acknowledgement"),
  MEMORY_PARITY_ERROR(0x08, "Memory Parity Error"),
  GATEWAY_PATH_UNAVAILABLE(0x0A, "Gateway Path Unavailable"),
  GATEWAY_TARGET_FAILED(0x0B, "Gateway Target Failed to Respond");

  private final int code;
  private final String description;

  ModbusExceptionCode(int code, String description) {
    this.code = code;
    this.description = description;
  }

  public int getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public static ModbusExceptionCode fromCode(int code) {
    for (ModbusExceptionCode e : values()) {
      if (e.code == code) {
        return e;
      }
    }
    throw new IllegalArgumentException("Unknown exception code: 0x" + Integer.toHexString(code));
  }
}
