/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.metrics;

import com.isahl.chess.bishop.protocol.modbus.metrics.ModbusMetrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModbusMetricsTest {
    
    @Test
    void testRecordRequest() {
        ModbusMetrics metrics = new ModbusMetrics();
        
        metrics.recordRequest(0x03, 100, true);
        
        assertEquals(1, metrics.getRequestCount());
        assertEquals(1, metrics.getSuccessCount());
        assertEquals(0, metrics.getFailureCount());
    }
    
    @Test
    void testRecordError() {
        ModbusMetrics metrics = new ModbusMetrics();
        
        metrics.recordError("timeout");
        
        assertEquals(1, metrics.getErrorCount());
    }
    
    @Test
    void testRecordConnection() {
        ModbusMetrics metrics = new ModbusMetrics();
        
        metrics.recordConnection();
        metrics.recordDisconnection();
        
        assertEquals(1, metrics.getTotalConnections());
        assertEquals(0, metrics.getActiveConnections());
    }
}
