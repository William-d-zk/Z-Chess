/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.function;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusExtendedFunctions;
import com.isahl.chess.bishop.protocol.modbus.function.ModbusExtendedFunctions.DiagnosticsSubfunction;
import org.junit.jupiter.api.Test;

class ModbusExtendedFunctionsTest {

  @Test
  void testReadExceptionStatus() {
    byte status = ModbusExtendedFunctions.readExceptionStatus(1);
    assertEquals(0x00, status); // No exception
  }

  @Test
  void testDiagnosticsSubfunction_fromCode() {
    assertEquals(DiagnosticsSubfunction.QUERY_DATA, DiagnosticsSubfunction.fromCode(0x0000));
    assertEquals(
        DiagnosticsSubfunction.RESTART_COMMUNICATIONS, DiagnosticsSubfunction.fromCode(0x0001));
    assertEquals(
        DiagnosticsSubfunction.RETURN_DIAGNOSTIC_REGISTER, DiagnosticsSubfunction.fromCode(0x0002));
  }

  @Test
  void testDiagnostics_queryData() {
    byte[] inputData = new byte[] {0x12, 0x34};
    byte[] result =
        ModbusExtendedFunctions.diagnostics(DiagnosticsSubfunction.QUERY_DATA, inputData);

    assertArrayEquals(inputData, result);
  }

  @Test
  void testDiagnostics_restartCommunications() {
    byte[] result =
        ModbusExtendedFunctions.diagnostics(
            DiagnosticsSubfunction.RESTART_COMMUNICATIONS, new byte[0]);

    assertArrayEquals(new byte[] {0x00, 0x00}, result);
  }

  @Test
  void testDiagnostics_unknownSubfunction() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          DiagnosticsSubfunction.fromCode(0xFFFF);
        });
  }

  @Test
  void testReportServerId() {
    byte[] serverId = ModbusExtendedFunctions.reportServerId(1);

    assertNotNull(serverId);
    assertTrue(serverId.length >= 3);
    assertEquals(1, serverId[0] & 0xFF); // Server ID
    assertEquals((byte) 0xFF, serverId[1]); // Running status
  }

  @Test
  void testMaskWriteRegister() {
    int currentValue = 0xABCD;
    int andMask = 0xFF00;
    int orMask = 0x000F;

    // 新值 = (0xABCD & 0xFF00) | (0x000F & ~0xFF00)
    //     = 0xAB00 | 0x000F
    //     = 0xAB0F
    int result = ModbusExtendedFunctions.maskWriteRegister(currentValue, andMask, orMask);

    assertEquals(0xAB0F, result);
  }

  @Test
  void testMaskWriteRegister_clearBit() {
    int currentValue = 0xFFFF;
    int andMask = 0xFFF0; // Clear lower 4 bits
    int orMask = 0x0000;

    int result = ModbusExtendedFunctions.maskWriteRegister(currentValue, andMask, orMask);

    assertEquals(0xFFF0, result);
  }

  @Test
  void testMaskWriteRegister_setBit() {
    int currentValue = 0x0000;
    int andMask = 0x0000;
    int orMask = 0x000F; // Set lower 4 bits

    int result = ModbusExtendedFunctions.maskWriteRegister(currentValue, andMask, orMask);

    assertEquals(0x000F, result);
  }

  @Test
  void testReadWriteMultipleRegisters() {
    int[] registers = new int[] {100, 200, 300, 400, 500};

    int[] writeValues = new int[] {111, 222};
    int[] result =
        ModbusExtendedFunctions.readWriteMultipleRegisters(
            registers,
            0,
            3, // Read 3 registers from address 0
            3, // Write to address 3
            writeValues);

    // After write: [100, 200, 300, 111, 222]
    // Read 3 from address 0: [100, 200, 300]
    assertArrayEquals(new int[] {100, 200, 300}, result);
    assertEquals(111, registers[3]);
    assertEquals(222, registers[4]);
  }

  @Test
  void testReadWriteMultipleRegisters_writeOnly() {
    int[] registers = new int[] {100, 200, 300};

    int[] writeValues = new int[] {111, 222, 333};
    int[] result =
        ModbusExtendedFunctions.readWriteMultipleRegisters(
            registers,
            0,
            3, // Read 3 registers from address 0
            0, // Write to address 0
            writeValues);

    // After write: [111, 222, 333]
    // Read 3 from address 0: [111, 222, 333]
    assertArrayEquals(new int[] {111, 222, 333}, result);
  }
}
