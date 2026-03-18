/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus;

import com.isahl.chess.bishop.protocol.modbus.master.ModbusMaster;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusResponse;
import com.isahl.chess.bishop.protocol.modbus.retry.ExponentialBackoffRetry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Modbus 集成测试
 * 
 * 注意：此测试需要真实的 Modbus 从站设备或模拟器
 * 运行前请确保启动 Modbus 从站模拟器在 localhost:502
 */
@Disabled("Requires Modbus slave simulator")
class ModbusIntegrationTest {
    
    @Test
    void testMasterReadHoldingRegisters() throws Exception {
        ModbusMaster master = ModbusMaster.builder()
            .host("localhost")
            .port(502)
            .timeout(3000)
            .retryPolicy(new ExponentialBackoffRetry(3, 1000))
            .build();
        
        master.connect();
        
        ModbusResponse response = master.sendRequest(
            master.createReadHoldingRegisters(1, 0, 10)
        );
        
        assertNotNull(response);
        assertEquals(1, response.getUnitId());
        
        master.close();
    }
}
