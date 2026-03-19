/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.tag;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** Modbus 数据类型转换器 支持寄存器数组与 Java 类型之间的转换 */
public class DataTypeConverter {

  private DataTypeConverter() {}

  /** 将寄存器数组转换为 Java 对象 */
  public static Object toJavaValue(int[] registers, DataType dataType, ByteOrder byteOrder) {
    switch (dataType) {
      case BOOLEAN:
        return registers.length > 0 && registers[0] != 0;
      case INT8:
        return (byte) (registers[0] >> 8);
      case UINT8:
        return (short) ((registers[0] >> 8) & 0xFF);
      case INT16:
        return (short) registers[0];
      case UINT16:
        return registers[0] & 0xFFFF;
      case INT32:
        return toInt32(registers, byteOrder);
      case UINT32:
        return toLong32(registers, byteOrder);
      case INT64:
        return toInt64(registers, byteOrder);
      case FLOAT32:
        return toFloat32(registers, byteOrder);
      case FLOAT64:
        return toFloat64(registers, byteOrder);
      case STRING:
        return toString(registers);
      default:
        throw new IllegalArgumentException("Unsupported data type: " + dataType);
    }
  }

  /** 将 Java 对象转换为寄存器数组 */
  public static int[] fromJavaValue(Object value, DataType dataType, ByteOrder byteOrder) {
    if (value == null) {
      throw new IllegalArgumentException("Value cannot be null");
    }

    switch (dataType) {
      case BOOLEAN:
        return new int[] {(Boolean) value ? 0xFF00 : 0};
      case INT8:
        return new int[] {((Number) value).byteValue() << 8};
      case UINT8:
        return new int[] {((Number) value).shortValue() << 8};
      case INT16:
        return new int[] {((Number) value).shortValue()};
      case UINT16:
        return new int[] {((Number) value).intValue() & 0xFFFF};
      case INT32:
        return fromInt32(((Number) value).intValue(), byteOrder);
      case UINT32:
        return fromLong32(((Number) value).longValue(), byteOrder);
      case INT64:
        return fromInt64(((Number) value).longValue(), byteOrder);
      case FLOAT32:
        return fromFloat32(((Number) value).floatValue(), byteOrder);
      case FLOAT64:
        return fromFloat64(((Number) value).doubleValue(), byteOrder);
      case STRING:
        return fromString((String) value);
      default:
        throw new IllegalArgumentException("Unsupported data type: " + dataType);
    }
  }

  // ========== 32 位整数转换 ==========

  private static int toInt32(int[] registers, ByteOrder byteOrder) {
    if (registers.length < 2) {
      throw new IllegalArgumentException("INT32 requires 2 registers");
    }

    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(toNioByteOrder(byteOrder));

    switch (byteOrder) {
      case ABCD: // Big Endian
        buffer.putInt((registers[0] << 16) | (registers[1] & 0xFFFF));
        break;
      case CDAB: // Little Endian
        buffer.putInt((registers[1] << 16) | (registers[0] & 0xFFFF));
        break;
      case BADC: // Byte Swap
        buffer.putInt((swapBytes(registers[0]) << 16) | swapBytes(registers[1]));
        break;
      case DCBA: // Full Swap
        buffer.putInt((swapBytes(registers[1]) << 16) | swapBytes(registers[0]));
        break;
    }

    buffer.flip();
    return buffer.getInt();
  }

  private static long toLong32(int[] registers, ByteOrder byteOrder) {
    return toInt32(registers, byteOrder) & 0xFFFFFFFFL;
  }

  private static int[] fromInt32(int value, ByteOrder byteOrder) {
    int[] registers = new int[2];

    switch (byteOrder) {
      case ABCD: // Big Endian
        registers[0] = (value >> 16) & 0xFFFF;
        registers[1] = value & 0xFFFF;
        break;
      case CDAB: // Little Endian
        registers[0] = value & 0xFFFF;
        registers[1] = (value >> 16) & 0xFFFF;
        break;
      case BADC: // Byte Swap
        registers[0] = swapBytes((value >> 16) & 0xFFFF);
        registers[1] = swapBytes(value & 0xFFFF);
        break;
      case DCBA: // Full Swap
        registers[0] = swapBytes(value & 0xFFFF);
        registers[1] = swapBytes((value >> 16) & 0xFFFF);
        break;
    }

    return registers;
  }

  private static int[] fromLong32(long value, ByteOrder byteOrder) {
    return fromInt32((int) value, byteOrder);
  }

  // ========== 64 位整数转换 ==========

  private static long toInt64(int[] registers, ByteOrder byteOrder) {
    if (registers.length < 4) {
      throw new IllegalArgumentException("INT64 requires 4 registers");
    }

    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.order(toNioByteOrder(byteOrder));

    switch (byteOrder) {
      case ABCD:
        buffer.putLong(
            ((long) registers[0] << 48)
                | ((long) (registers[1] & 0xFFFF) << 32)
                | ((long) (registers[2] & 0xFFFF) << 16)
                | (registers[3] & 0xFFFF));
        break;
      case CDAB:
        buffer.putLong(
            ((long) registers[3] << 48)
                | ((long) (registers[2] & 0xFFFF) << 32)
                | ((long) (registers[1] & 0xFFFF) << 16)
                | (registers[0] & 0xFFFF));
        break;
      default:
        throw new IllegalArgumentException("Unsupported byte order for INT64: " + byteOrder);
    }

    buffer.flip();
    return buffer.getLong();
  }

  private static int[] fromInt64(long value, ByteOrder byteOrder) {
    int[] registers = new int[4];

    switch (byteOrder) {
      case ABCD:
        registers[0] = (int) ((value >> 48) & 0xFFFF);
        registers[1] = (int) ((value >> 32) & 0xFFFF);
        registers[2] = (int) ((value >> 16) & 0xFFFF);
        registers[3] = (int) (value & 0xFFFF);
        break;
      case CDAB:
        registers[0] = (int) (value & 0xFFFF);
        registers[1] = (int) ((value >> 16) & 0xFFFF);
        registers[2] = (int) ((value >> 32) & 0xFFFF);
        registers[3] = (int) ((value >> 48) & 0xFFFF);
        break;
      default:
        throw new IllegalArgumentException("Unsupported byte order for INT64: " + byteOrder);
    }

    return registers;
  }

  // ========== 浮点数转换 ==========

  private static float toFloat32(int[] registers, ByteOrder byteOrder) {
    if (registers.length < 2) {
      throw new IllegalArgumentException("FLOAT32 requires 2 registers");
    }

    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(toNioByteOrder(byteOrder));

    switch (byteOrder) {
      case ABCD:
        buffer.putInt((registers[0] << 16) | (registers[1] & 0xFFFF));
        break;
      case CDAB:
        buffer.putInt((registers[1] << 16) | (registers[0] & 0xFFFF));
        break;
      case BADC:
        buffer.putInt((swapBytes(registers[0]) << 16) | swapBytes(registers[1]));
        break;
      case DCBA:
        buffer.putInt((swapBytes(registers[1]) << 16) | swapBytes(registers[0]));
        break;
    }

    buffer.flip();
    return buffer.getFloat();
  }

  private static double toFloat64(int[] registers, ByteOrder byteOrder) {
    if (registers.length < 4) {
      throw new IllegalArgumentException("FLOAT64 requires 4 registers");
    }

    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.order(toNioByteOrder(byteOrder));

    switch (byteOrder) {
      case ABCD:
        buffer.putLong(
            ((long) registers[0] << 48)
                | ((long) (registers[1] & 0xFFFF) << 32)
                | ((long) (registers[2] & 0xFFFF) << 16)
                | (registers[3] & 0xFFFF));
        break;
      case CDAB:
        buffer.putLong(
            ((long) registers[3] << 48)
                | ((long) (registers[2] & 0xFFFF) << 32)
                | ((long) (registers[1] & 0xFFFF) << 16)
                | (registers[0] & 0xFFFF));
        break;
      default:
        throw new IllegalArgumentException("Unsupported byte order for FLOAT64: " + byteOrder);
    }

    buffer.flip();
    return buffer.getDouble();
  }

  private static int[] fromFloat32(float value, ByteOrder byteOrder) {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.putFloat(value);
    buffer.flip();
    int intValue = buffer.getInt();

    return fromInt32(intValue, byteOrder);
  }

  private static int[] fromFloat64(double value, ByteOrder byteOrder) {
    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.putDouble(value);
    buffer.flip();
    long longValue = buffer.getLong();

    return fromInt64(longValue, byteOrder);
  }

  // ========== 字符串转换 ==========

  private static String toString(int[] registers) {
    ByteBuffer buffer = ByteBuffer.allocate(registers.length * 2);
    buffer.order(java.nio.ByteOrder.BIG_ENDIAN);

    for (int reg : registers) {
      buffer.putChar((char) (reg & 0xFFFF));
    }

    buffer.flip();
    return StandardCharsets.UTF_16BE.decode(buffer).toString().trim();
  }

  private static int[] fromString(String value) {
    ByteBuffer buffer = StandardCharsets.UTF_16BE.encode(value);
    int[] registers = new int[(buffer.remaining() + 1) / 2];

    int i = 0;
    while (buffer.hasRemaining()) {
      int high = buffer.get() & 0xFF;
      int low = buffer.hasRemaining() ? buffer.get() & 0xFF : 0;
      registers[i++] = (high << 8) | low;
    }

    return registers;
  }

  // ========== 辅助方法 ==========

  private static int swapBytes(int value) {
    return ((value & 0xFF) << 8) | ((value >> 8) & 0xFF);
  }

  private static java.nio.ByteOrder toNioByteOrder(ByteOrder byteOrder) {
    switch (byteOrder) {
      case ABCD:
      case BADC:
        return java.nio.ByteOrder.BIG_ENDIAN;
      case CDAB:
      case DCBA:
        return java.nio.ByteOrder.LITTLE_ENDIAN;
      default:
        return java.nio.ByteOrder.BIG_ENDIAN;
    }
  }
}
