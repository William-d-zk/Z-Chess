/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.tag;

import com.isahl.chess.bishop.protocol.modbus.tag.ByteOrder;
import com.isahl.chess.bishop.protocol.modbus.tag.DataArea;
import com.isahl.chess.bishop.protocol.modbus.tag.DataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataTypeTest {
    
    @Test
    void testDataType_registers() {
        assertEquals(0, DataType.BOOLEAN.getRegisters());
        assertEquals(1, DataType.INT16.getRegisters());
        assertEquals(2, DataType.FLOAT32.getRegisters());
        assertEquals(4, DataType.FLOAT64.getRegisters());
    }
    
    @Test
    void testDataArea() {
        assertEquals(0, DataArea.COIL.getAddressOffset());
        assertEquals(1, DataArea.DISCRETE_INPUT.getAddressOffset());
        assertEquals(3, DataArea.INPUT_REGISTER.getAddressOffset());
        assertEquals(4, DataArea.HOLDING_REGISTER.getAddressOffset());
    }
    
    @Test
    void testByteOrder() {
        assertEquals(ByteOrder.ABCD, ByteOrder.BIG_ENDIAN);
        assertEquals(ByteOrder.CDAB, ByteOrder.LITTLE_ENDIAN);
    }
}
