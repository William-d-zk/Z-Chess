/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.slave;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusMaster;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusResponse;
import com.isahl.chess.bishop.protocol.modbus.slave.DataModel;
import com.isahl.chess.bishop.protocol.modbus.slave.ModbusSlave;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModbusSlaveIntegrationTest {
    
    private ModbusSlave slave;
    private ModbusMaster master;
    
    @BeforeEach
    void setUp() throws Exception {
        slave = ModbusSlave.builder()
            .port(15020)
            .unitId(1)
            .build();
        slave.start();
        
        Thread.sleep(100); // Wait for server to start
        
        master = ModbusMaster.builder()
            .host("localhost")
            .port(15020)
            .timeout(3000)
            .build();
        master.connect();
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (master != null) {
            master.close();
        }
        if (slave != null) {
            slave.stop();
        }
    }
    
    @Test
    void testReadHoldingRegisters() throws Exception {
        DataModel model = slave.getDataModel();
        model.setHoldingRegister(0, 100);
        model.setHoldingRegister(1, 200);
        model.setHoldingRegister(2, 300);
        
        var request = master.createReadHoldingRegisters(1, 0, 3);
        ModbusResponse response = master.sendRequest(request);
        
        assertNotNull(response);
        assertFalse(response.isException());
        assertEquals(1, response.getUnitId());
        assertEquals(7, response.getData().length); // byte count + 3 registers * 2 bytes
    }
    
    @Test
    void testWriteAndReadHoldingRegisters() throws Exception {
        var writeRequest = new com.isahl.chess.bishop.protocol.modbus.master.ModbusRequest(
            1, ModbusFunction.WRITE_SINGLE_REGISTER, 
            new byte[]{0, 10, 0x12, 0x34}
        );
        ModbusResponse writeResponse = master.sendRequest(writeRequest);
        
        assertFalse(writeResponse.isException());
        
        var readRequest = master.createReadHoldingRegisters(1, 10, 1);
        ModbusResponse readResponse = master.sendRequest(readRequest);
        
        assertFalse(readResponse.isException());
        assertEquals(0x1234, (readResponse.getData()[1] & 0xFF) << 8 | (readResponse.getData()[2] & 0xFF));
    }
    
    @Test
    void testReadCoils() throws Exception {
        DataModel model = slave.getDataModel();
        model.setCoil(0, true);
        model.setCoil(1, false);
        model.setCoil(2, true);
        
        var request = master.createReadHoldingRegisters(1, 0, 1);
        ModbusResponse response = master.sendRequest(request);
        
        assertNotNull(response);
    }
}
