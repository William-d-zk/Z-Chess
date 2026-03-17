/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.isahl.chess.audience.bishop.mqtt.v5;

import com.isahl.chess.bishop.mqtt.v5.RetainedMessageStore;
import com.isahl.chess.bishop.mqtt.v5.RetainedMessageStore.RetainedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RetainedMessageStoreTest {
    
    private RetainedMessageStore store;
    
    @BeforeEach
    void setUp() {
        store = new RetainedMessageStore();
    }
    
    @Test
    void testStoreAndGet() {
        byte[] payload = "test message".getBytes();
        store.store("sensors/temp", payload, 1);
        
        RetainedMessage msg = store.get("sensors/temp");
        assertNotNull(msg);
        assertArrayEquals(payload, msg.getPayload());
        assertEquals(1, msg.getQos());
    }
    
    @Test
    void testStoreEmptyDeletes() {
        store.store("sensors/temp", "test".getBytes(), 1);
        assertEquals(1, store.size());
        
        store.store("sensors/temp", null, 1);
        assertEquals(0, store.size());
        assertNull(store.get("sensors/temp"));
    }
    
    @Test
    void testGetMatchingExact() {
        store.store("sensors/temp", "temp".getBytes(), 1);
        store.store("sensors/humidity", "humidity".getBytes(), 1);
        
        List<RetainedMessage> matching = store.getMatching("sensors/temp");
        assertEquals(1, matching.size());
        assertEquals("sensors/temp", matching.get(0).getTopic());
    }
    
    @Test
    void testGetMatchingWildcardPlus() {
        store.store("sensors/temp", "temp".getBytes(), 1);
        store.store("sensors/humidity", "humidity".getBytes(), 1);
        store.store("actuators/led", "led".getBytes(), 1);
        
        List<RetainedMessage> matching = store.getMatching("sensors/+");
        assertEquals(2, matching.size());
    }
    
    @Test
    void testGetMatchingWildcardHash() {
        store.store("sensors/temp/room1", "temp".getBytes(), 1);
        store.store("sensors/temp/room2", "temp2".getBytes(), 1);
        store.store("sensors/humidity", "humidity".getBytes(), 1);
        
        List<RetainedMessage> matching = store.getMatching("sensors/#");
        assertEquals(3, matching.size());
    }
    
    @Test
    void testClear() {
        store.store("sensors/temp", "temp".getBytes(), 1);
        store.store("sensors/humidity", "humidity".getBytes(), 1);
        
        store.clear();
        assertEquals(0, store.size());
    }
}
