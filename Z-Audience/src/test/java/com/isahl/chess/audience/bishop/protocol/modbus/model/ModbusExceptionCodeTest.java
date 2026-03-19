/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.model;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.protocol.modbus.model.ModbusExceptionCode;
import org.junit.jupiter.api.Test;

class ModbusExceptionCodeTest {

  @Test
  void testFromCode_validCodes() {
    assertEquals(ModbusExceptionCode.ILLEGAL_FUNCTION, ModbusExceptionCode.fromCode(0x01));
    assertEquals(ModbusExceptionCode.ILLEGAL_DATA_ADDRESS, ModbusExceptionCode.fromCode(0x02));
    assertEquals(ModbusExceptionCode.ILLEGAL_DATA_VALUE, ModbusExceptionCode.fromCode(0x03));
    assertEquals(ModbusExceptionCode.SERVER_DEVICE_FAILURE, ModbusExceptionCode.fromCode(0x04));
  }

  @Test
  void testFromCode_invalidCode_throwsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          ModbusExceptionCode.fromCode(0x99);
        });
  }

  @Test
  void testGetCode_and_getDescription() {
    ModbusExceptionCode code = ModbusExceptionCode.SERVER_DEVICE_BUSY;
    assertEquals(0x06, code.getCode());
    assertEquals("Server Device Busy", code.getDescription());
  }
}
