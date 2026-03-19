/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.exception;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.protocol.modbus.exception.ModbusException;
import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusExceptionCode;
import org.junit.jupiter.api.Test;

class ModbusExceptionTest {

  @Test
  void testConstructor_withFunctionAndExceptionCode() {
    ModbusException ex =
        new ModbusException(
            ModbusFunction.READ_HOLDING_REGISTERS, ModbusExceptionCode.ILLEGAL_DATA_ADDRESS);

    assertEquals(ModbusFunction.READ_HOLDING_REGISTERS, ex.getFunction());
    assertEquals(ModbusExceptionCode.ILLEGAL_DATA_ADDRESS, ex.getExceptionCode());
    assertEquals(0x03, ex.getFunctionCode());
    assertEquals(0x02, ex.getExceptionCodeValue());
  }

  @Test
  void testConstructor_withMessage() {
    ModbusException ex = new ModbusException("Test message");
    assertEquals("Test message", ex.getMessage());
    assertNull(ex.getFunction());
    assertNull(ex.getExceptionCode());
  }

  @Test
  void testIsExceptionResponse() {
    ModbusException ex =
        new ModbusException(
            ModbusFunction.READ_HOLDING_REGISTERS, ModbusExceptionCode.ILLEGAL_DATA_ADDRESS);
    assertTrue(ex.isExceptionResponse());
  }
}
