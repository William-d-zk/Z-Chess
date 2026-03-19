/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;
import com.isahl.chess.bishop.protocol.modbus.spi.ModbusTcpProtocolHandler;
import com.isahl.chess.bishop.protocol.modbus.tcp.ModbusTcpCodec.ModbusTcpMessage;
import com.isahl.chess.king.base.content.ByteBuf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModbusTcpProtocolHandlerTest {

  private ModbusTcpProtocolHandler handler;

  @BeforeEach
  void setUp() {
    handler = new ModbusTcpProtocolHandler();
  }

  @Test
  void testGetProtocolName() {
    assertEquals("ModbusTCP", handler.getProtocolName());
  }

  @Test
  void testGetProtocolVersion() {
    assertEquals("1.0.0", handler.getProtocolVersion());
  }

  @Test
  void testEncodeDecode() {
    ModbusMessage message = handler.createReadHoldingRegisters(1, 0, 10);
    byte[] encoded = handler.encode(message);

    ByteBuf buffer = ByteBuf.allocate(encoded.length);
    buffer.put(encoded);

    Object decoded = handler.decode(buffer);

    assertNotNull(decoded);
    assertTrue(decoded instanceof ModbusTcpMessage);

    ModbusTcpMessage modbusMessage = (ModbusTcpMessage) decoded;
    assertEquals(1, modbusMessage.getUnitId());
    assertEquals(ModbusFunction.READ_HOLDING_REGISTERS, modbusMessage.getFunction());
  }

  @Test
  void testCreateWriteSingleRegister() {
    ModbusMessage message = handler.createWriteSingleRegister(1, 100, 1234);

    assertNotNull(message);
    assertEquals(1, message.getUnitId());
    assertEquals(ModbusFunction.WRITE_SINGLE_REGISTER, message.getFunction());
    assertEquals(4, message.getData().length);
  }

  @Test
  void testCreateWriteMultipleRegisters() {
    int[] values = {100, 200, 300};
    ModbusMessage message = handler.createWriteMultipleRegisters(1, 0, values);

    assertNotNull(message);
    assertEquals(1, message.getUnitId());
    assertEquals(ModbusFunction.WRITE_MULTIPLE_REGISTERS, message.getFunction());
    assertEquals(5 + values.length * 2, message.getData().length);
  }

  @Test
  void testCreateReadCoils() {
    ModbusMessage message = handler.createReadCoils(1, 0, 16);

    assertNotNull(message);
    assertEquals(1, message.getUnitId());
    assertEquals(ModbusFunction.READ_COILS, message.getFunction());
  }

  @Test
  void testDecodeIncompleteData() {
    ByteBuf buffer = ByteBuf.allocate(3);
    buffer.put(new byte[] {0x00, 0x01, 0x00});

    Object decoded = handler.decode(buffer);

    assertNull(decoded);
  }

  @Test
  void testEncodeInvalidMessage() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          handler.encode("Invalid message");
        });
  }
}
