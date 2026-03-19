/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.tag;

/** Modbus 数据类型 */
public enum DataType {
  BOOLEAN(0), // 1 位 (Coil/Discrete Input)
  INT8(1), // 8 位有符号
  UINT8(1), // 8 位无符号
  INT16(1), // 16 位有符号
  UINT16(1), // 16 位无符号
  INT32(2), // 32 位有符号
  UINT32(2), // 32 位无符号
  INT64(4), // 64 位有符号
  FLOAT32(2), // 32 位浮点
  FLOAT64(4), // 64 位浮点
  STRING(-1); // 字符串 (可变长度)

  private final int registers;

  DataType(int registers) {
    this.registers = registers;
  }

  public int getRegisters() {
    return registers;
  }
}
