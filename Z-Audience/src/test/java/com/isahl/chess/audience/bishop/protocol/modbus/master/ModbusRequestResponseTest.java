/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.master;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusRequest;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusResponse;
import org.junit.jupiter.api.Test;

class ModbusRequestResponseTest {

  @Test
  void testModbusRequest() {
    ModbusRequest request =
        new ModbusRequest(1, ModbusFunction.READ_HOLDING_REGISTERS, new byte[] {0, 0, 0, 10});

    assertEquals(1, request.getUnitId());
    assertEquals(ModbusFunction.READ_HOLDING_REGISTERS, request.getFunction());
    assertEquals(4, request.getData().length);
  }

  @Test
  void testModbusResponse() {
    ModbusResponse response =
        new ModbusResponse(
            1, ModbusFunction.READ_HOLDING_REGISTERS, new byte[] {0, 4, 0, 10, 0, 20});

    assertEquals(1, response.getUnitId());
    assertEquals(ModbusFunction.READ_HOLDING_REGISTERS, response.getFunction());
    assertEquals(6, response.getData().length);
    assertFalse(response.isException());
  }

  @Test
  void testModbusResponse_exception() {
    ModbusResponse response =
        ModbusResponse.exception(1, ModbusFunction.READ_HOLDING_REGISTERS, 0x02);

    assertTrue(response.isException());
    assertEquals(0x83, response.getFunctionCode());
    assertEquals(0x02, response.getExceptionCode());
  }
}
