/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.slave;

import com.isahl.chess.bishop.protocol.modbus.slave.DataModel;
import com.isahl.chess.bishop.protocol.modbus.slave.ModbusSlave;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Modbus 从站集成测试
 * 
 * 注意：此测试使用本地端口 15020，确保该端口未被占用
 */
class ModbusSlaveIntegrationTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(ModbusSlaveIntegrationTest.class);
    
    private ModbusSlave slave;
    private ModbusSlave slave2;
    
    @BeforeEach
    void setUp() throws Exception {
        slave = ModbusSlave.builder()
            .port(15020)
            .unitId(1)
            .build();
        slave.start();
        
        // Wait for server to start
        Thread.sleep(500);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (slave != null) {
            try {
                slave.stop();
            } catch (Exception e) {
                LOG.warn("Error stopping slave", e);
            }
        }
        if (slave2 != null) {
            try {
                slave2.stop();
            } catch (Exception e) {
                LOG.warn("Error stopping slave2", e);
            }
        }
    }
    
    @Test
    void testSlaveStartsSuccessfully() {
        assertTrue(slave.isRunning());
        assertNotNull(slave.getDataModel());
    }
    
    @Test
    void testDataModelOperations() {
        DataModel model = slave.getDataModel();
        
        model.setHoldingRegister(0, 100);
        model.setHoldingRegister(1, 200);
        
        assertEquals(100, model.getHoldingRegister(0));
        assertEquals(200, model.getHoldingRegister(1));
        
        model.setCoil(0, true);
        model.setCoil(1, false);
        
        assertTrue(model.getCoil(0));
        assertFalse(model.getCoil(1));
    }
    
    @Test
    void testMultipleSlavesOnDifferentPorts() throws Exception {
        slave2 = ModbusSlave.builder()
            .port(15021)
            .unitId(2)
            .build();
        slave2.start();
        
        Thread.sleep(100);
        
        assertTrue(slave.isRunning());
        assertTrue(slave2.isRunning());
        assertEquals(1, slave.getConfig().getUnitId());
        assertEquals(2, slave2.getConfig().getUnitId());
    }
}
