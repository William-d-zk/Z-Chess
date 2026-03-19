/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.tag;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.protocol.modbus.tag.ByteOrder;
import com.isahl.chess.bishop.protocol.modbus.tag.DataType;
import com.isahl.chess.bishop.protocol.modbus.tag.DataTypeConverter;
import org.junit.jupiter.api.Test;

class DataTypeConverterTest {

  @Test
  void testInt16Conversion() {
    int[] registers = {0x1234};
    Object value = DataTypeConverter.toJavaValue(registers, DataType.INT16, ByteOrder.ABCD);

    assertEquals((short) 0x1234, value);

    int[] back = DataTypeConverter.fromJavaValue(value, DataType.INT16, ByteOrder.ABCD);
    assertArrayEquals(registers, back);
  }

  @Test
  void testInt32Conversion_bigEndian() {
    int[] registers = {0x1234, 0x5678};
    Object value = DataTypeConverter.toJavaValue(registers, DataType.INT32, ByteOrder.ABCD);

    assertEquals(0x12345678, value);

    int[] back = DataTypeConverter.fromJavaValue(value, DataType.INT32, ByteOrder.ABCD);
    assertArrayEquals(registers, back);
  }

  @Test
  void testInt32Conversion_littleEndian() {
    int[] registers = {0x5678, 0x1234}; // Reversed for little endian
    Object value = DataTypeConverter.toJavaValue(registers, DataType.INT32, ByteOrder.CDAB);

    assertEquals(0x12345678, value);
  }

  @Test
  void testFloat32Conversion() {
    int[] registers = {0x4049, 0x0FDB}; // PI in IEEE754
    Object value = DataTypeConverter.toJavaValue(registers, DataType.FLOAT32, ByteOrder.ABCD);

    float pi = (Float) value;
    assertEquals(3.14159f, pi, 0.001);
  }

  @Test
  void testBooleanConversion() {
    int[] registersTrue = {0xFF00};
    int[] registersFalse = {0x0000};

    assertEquals(
        true, DataTypeConverter.toJavaValue(registersTrue, DataType.BOOLEAN, ByteOrder.ABCD));
    assertEquals(
        false, DataTypeConverter.toJavaValue(registersFalse, DataType.BOOLEAN, ByteOrder.ABCD));
  }
}
