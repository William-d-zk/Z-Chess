/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.tag;

import com.isahl.chess.bishop.protocol.modbus.tag.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TagManagerTest {
    
    @Test
    void testTagBuilder() {
        var tag = Tag.builder()
            .name("Temperature")
            .unitId(1)
            .address(0)
            .dataType(DataType.FLOAT32)
            .dataArea(DataArea.HOLDING_REGISTER)
            .scale(0.1)
            .offset(-40.0)
            .unit("°C")
            .build();
        
        assertEquals("Temperature", tag.getName());
        assertEquals(1, tag.getUnitId());
        assertEquals(0, tag.getAddress());
        assertEquals(DataType.FLOAT32, tag.getDataType());
        assertEquals(0.1, tag.getScale());
    }
    
    @Test
    void testTagManager_registerAndGet() {
        TagManager manager = new TagManager();
        var tag = Tag.builder()
            .name("Test")
            .unitId(1)
            .address(0)
            .dataType(DataType.INT16)
            .dataArea(DataArea.HOLDING_REGISTER)
            .build();
        
        manager.register(tag);
        var retrieved = manager.getTag("Test");
        
        assertNotNull(retrieved);
        assertEquals("Test", retrieved.getName());
    }
}
