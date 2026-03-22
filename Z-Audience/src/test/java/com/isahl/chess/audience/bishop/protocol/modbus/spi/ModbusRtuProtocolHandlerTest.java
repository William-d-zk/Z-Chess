/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.spi;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;
import com.isahl.chess.bishop.protocol.modbus.spi.ModbusRtuProtocolHandler;
import com.isahl.chess.king.base.content.ByteBuf;
import org.junit.jupiter.api.Test;

class ModbusRtuProtocolHandlerTest {

  private final ModbusRtuProtocolHandler handler = new ModbusRtuProtocolHandler();

  @Test
  void testGetProtocolName() {
    assertEquals("ModbusRTU", handler.getName());
  }

  @Test
  void testGetProtocolSignature() {
    assertNull(handler.getProtocolSignature()); // RTU has no fixed signature
  }

  @Test
  void testDecode_and_Encode() {
    ModbusMessage message =
        new ModbusMessage(1, ModbusFunction.READ_HOLDING_REGISTERS, new byte[] {0, 0, 0, 10});
    byte[] encoded = handler.encode(message);

    ByteBuf buffer = ByteBuf.allocate(encoded.length);
    buffer.put(encoded);

    Object decoded = handler.decode(buffer);
    assertNotNull(decoded);
    assertTrue(decoded instanceof ModbusMessage);

    ModbusMessage decodedMessage = (ModbusMessage) decoded;
    assertEquals(1, decodedMessage.getUnitId());
    assertEquals(ModbusFunction.READ_HOLDING_REGISTERS, decodedMessage.getFunction());
  }
}
