/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;
import com.isahl.chess.bishop.protocol.modbus.tcp.ModbusTcpCodec;
import com.isahl.chess.bishop.protocol.modbus.tcp.ModbusTcpCodec.ModbusTcpMessage;
import com.isahl.chess.king.base.content.ByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModbusTcpCodecTest {
    
    @Test
    void testEncodeReadHoldingRegisters() {
        ModbusMessage message = new ModbusMessage(1, ModbusFunction.READ_HOLDING_REGISTERS, new byte[]{0, 0, 0, 10});
        ByteBuf buffer = ModbusTcpCodec.encode(message, 1);
        
        assertEquals(12, buffer.readableBytes());
        assertEquals(0, ((buffer.peek(0) & 0xFF) << 8) | (buffer.peek(1) & 0xFF));
        assertEquals(0, ((buffer.peek(2) & 0xFF) << 8) | (buffer.peek(3) & 0xFF));
        assertEquals(5, ((buffer.peek(4) & 0xFF) << 8) | (buffer.peek(5) & 0xFF));
        assertEquals(1, buffer.peek(6) & 0xFF);
        assertEquals(0x03, buffer.peek(7) & 0xFF);
    }
    
    @Test
    void testDecodeReadHoldingRegisters() {
        byte[] data = {0x00, 0x01, 0x00, 0x00, 0x00, 0x05, 0x01, 0x03, 0x00, 0x00, 0x00, 0x0A};
        ByteBuf buffer = ByteBuf.allocate(data.length);
        buffer.put(data);
        
        ModbusTcpMessage message = ModbusTcpCodec.decode(buffer);
        
        assertNotNull(message);
        assertEquals(1, message.getTransactionId());
        assertEquals(1, message.getUnitId());
        assertEquals(ModbusFunction.READ_HOLDING_REGISTERS, message.getFunction());
        assertEquals(4, message.getData().length);
    }
    
    @Test
    void testDecodeIncompleteData() {
        byte[] data = {0x00, 0x01, 0x00};
        ByteBuf buffer = ByteBuf.allocate(data.length);
        buffer.put(data);
        
        assertNull(ModbusTcpCodec.decode(buffer));
    }
    
    @Test
    void testGetFrameLength() {
        byte[] data = {0x00, 0x01, 0x00, 0x00, 0x00, 0x05, 0x01, 0x03};
        ByteBuf buffer = ByteBuf.allocate(data.length);
        buffer.put(data);
        
        assertEquals(12, ModbusTcpCodec.getFrameLength(buffer));
    }
    
    @Test
    void testEncodeExceptionResponse() {
        byte[] data = ModbusTcpCodec.encodeException(1, 0x03, 
            com.isahl.chess.bishop.protocol.modbus.model.ModbusExceptionCode.ILLEGAL_DATA_ADDRESS, 1);
        
        assertEquals(10, data.length);
        assertEquals((byte) 0x83, data[7]);
        assertEquals((byte) 0x02, data[8]);
    }
}
