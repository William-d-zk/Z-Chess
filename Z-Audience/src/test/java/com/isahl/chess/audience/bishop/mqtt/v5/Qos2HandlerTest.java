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

import com.isahl.chess.bishop.mqtt.v5.Qos2Handler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Qos2HandlerTest {
    
    private Qos2Handler handler;
    
    @BeforeEach
    void setUp() {
        handler = new Qos2Handler();
    }
    
    @Test
    void testHandlePublish() {
        byte[] payload = "test message".getBytes();
        int messageId = 123;
        
        int pubrecId = handler.handlePublish(messageId, payload);
        
        assertEquals(messageId, pubrecId);
        assertEquals(1, handler.getPendingCount());
    }
    
    @Test
    void testHandlePublishDuplicate() {
        byte[] payload = "test message".getBytes();
        int messageId = 123;
        
        handler.handlePublish(messageId, payload);
        int pubrecId2 = handler.handlePublish(messageId, payload);
        
        assertEquals(messageId, pubrecId2);
        assertEquals(1, handler.getPendingCount());
    }
    
    @Test
    void testHandlePubrec() {
        byte[] payload = "test message".getBytes();
        int messageId = 123;
        
        handler.handlePublish(messageId, payload);
        boolean shouldSendPubrel = handler.handlePubrec(messageId);
        
        assertTrue(shouldSendPubrel);
        assertEquals(1, handler.getPendingCount());
    }
    
    @Test
    void testHandlePubrel() {
        byte[] payload = "test message".getBytes();
        int messageId = 123;
        
        handler.handlePublish(messageId, payload);
        handler.handlePubrec(messageId);
        boolean shouldSendPubcomp = handler.handlePubrel(messageId);
        
        assertTrue(shouldSendPubcomp);
        assertEquals(1, handler.getPendingCount());
    }
    
    @Test
    void testHandlePubcomp() {
        byte[] payload = "test message".getBytes();
        int messageId = 123;
        
        handler.handlePublish(messageId, payload);
        handler.handlePubrec(messageId);
        handler.handlePubrel(messageId);
        
        Qos2Handler.Qos2Message completed = handler.handlePubcomp(messageId);
        
        assertNotNull(completed);
        assertEquals(messageId, completed.getMessageId());
        assertArrayEquals(payload, completed.getPayload());
        assertEquals(0, handler.getPendingCount());
    }
    
    @Test
    void testFullQos2Flow() {
        byte[] payload = "QoS 2 message".getBytes();
        int messageId = 456;
        
        // Step 1: PUBLISH -> PUBREC
        int pubrecId = handler.handlePublish(messageId, payload);
        assertEquals(messageId, pubrecId);
        
        // Step 2: PUBREC -> PUBREL
        boolean sendPubrel = handler.handlePubrec(messageId);
        assertTrue(sendPubrel);
        
        // Step 3: PUBREL -> PUBCOMP
        boolean sendPubcomp = handler.handlePubrel(messageId);
        assertTrue(sendPubcomp);
        
        // Step 4: PUBCOMP -> Complete
        Qos2Handler.Qos2Message completed = handler.handlePubcomp(messageId);
        assertNotNull(completed);
        assertEquals(0, handler.getPendingCount());
    }
    
    @Test
    void testInitiateQos2Publish() {
        byte[] payload = "outbound message".getBytes();
        int messageId = 789;
        
        handler.initiateQos2Publish(messageId, payload);
        
        assertEquals(1, handler.getPendingCount());
    }
}
