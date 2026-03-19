/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.rtu;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;
import com.isahl.chess.bishop.protocol.modbus.rtu.ModbusRtuCodec;
import com.isahl.chess.king.base.content.ByteBuf;
import org.junit.jupiter.api.Test;

class ModbusRtuCodecTest {

  @Test
  void testEncodeReadHoldingRegisters() {
    ModbusMessage message =
        new ModbusMessage(1, ModbusFunction.READ_HOLDING_REGISTERS, new byte[] {0, 0, 0, 10});
    ByteBuf buffer = ModbusRtuCodec.encode(message);

    assertEquals(8, buffer.readableBytes()); // 1 address + 1 function + 4 data + 2 CRC
    assertEquals(1, buffer.peek(0) & 0xFF); // Unit ID
    assertEquals(0x03, buffer.peek(1) & 0xFF); // Function code
  }

  @Test
  void testDecodeReadHoldingRegisters() {
    // First encode to get correct CRC
    ModbusMessage original =
        new ModbusMessage(1, ModbusFunction.READ_HOLDING_REGISTERS, new byte[] {0, 0, 0, 10});
    ByteBuf encoded = ModbusRtuCodec.encode(original);

    // Now decode
    ModbusMessage message = ModbusRtuCodec.decode(encoded);

    assertNotNull(message);
    assertEquals(1, message.getUnitId());
    assertEquals(ModbusFunction.READ_HOLDING_REGISTERS, message.getFunction());
    assertEquals(4, message.getData().length);
  }

  @Test
  void testDecodeIncompleteData() {
    byte[] data = {0x01, 0x03};
    ByteBuf buffer = ByteBuf.allocate(data.length);
    buffer.put(data);

    assertNull(ModbusRtuCodec.decode(buffer));
  }

  @Test
  void testCrcValidation() {
    // Create a valid message and encode it
    ModbusMessage original =
        new ModbusMessage(1, ModbusFunction.READ_HOLDING_REGISTERS, new byte[] {0, 0, 0, 10});
    ByteBuf encoded = ModbusRtuCodec.encode(original);

    // Decode should work with valid CRC
    ModbusMessage message = ModbusRtuCodec.decode(encoded);
    assertNotNull(message); // CRC should be valid
  }
}
