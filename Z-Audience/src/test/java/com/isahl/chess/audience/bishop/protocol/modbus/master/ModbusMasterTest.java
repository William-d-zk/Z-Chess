/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.master;

import com.isahl.chess.bishop.protocol.modbus.master.ModbusMaster;
import com.isahl.chess.bishop.protocol.modbus.retry.ExponentialBackoffRetry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModbusMasterTest {
    
    @Test
    void testBuilder() {
        ModbusMaster master = ModbusMaster.builder()
            .host("localhost")
            .port(502)
            .timeout(3000)
            .retryPolicy(new ExponentialBackoffRetry(3, 1000))
            .build();
        
        assertNotNull(master);
        assertFalse(master.isConnected());
    }
    
    @Test
    void testCreateReadHoldingRegistersRequest() {
        ModbusMaster master = ModbusMaster.builder().host("localhost").port(502).build();
        var request = master.createReadHoldingRegisters(1, 0, 10);
        
        assertEquals(1, request.getUnitId());
        assertEquals(4, request.getData().length);
    }
}
