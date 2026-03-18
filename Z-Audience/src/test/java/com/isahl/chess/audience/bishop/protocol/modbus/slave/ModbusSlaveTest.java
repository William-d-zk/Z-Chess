/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.slave;

import com.isahl.chess.bishop.protocol.modbus.slave.ModbusSlave;
import com.isahl.chess.bishop.protocol.modbus.slave.ModbusSlaveConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModbusSlaveTest {
    
    @Test
    void testBuilder() {
        ModbusSlave slave = ModbusSlave.builder()
            .port(502)
            .unitId(1)
            .build();
        
        assertNotNull(slave);
        assertFalse(slave.isRunning());
    }
    
    @Test
    void testModbusSlaveConfig() {
        ModbusSlaveConfig config = ModbusSlaveConfig.builder()
            .port(502)
            .unitId(1)
            .timeout(3000)
            .build();
        
        assertEquals(502, config.getPort());
        assertEquals(1, config.getUnitId());
        assertEquals(3000, config.getTimeoutMs());
    }
}
