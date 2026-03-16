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

package com.isahl.chess.bishop.mqtt.v5;

import com.isahl.chess.bishop.protocol.mqtt.model.QttProperty;
import com.isahl.chess.bishop.protocol.mqtt.model.QttPropertySet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageExpiryHandlerTest {
    
    private MessageExpiryHandler handler;
    
    @BeforeEach
    void setUp() {
        handler = new MessageExpiryHandler();
    }
    
    @Test
    void testRegisterMessageWithExpiry() {
        QttPropertySet propertySet = mock(QttPropertySet.class);
        when(propertySet.getProperty(QttProperty.MESSAGE_EXPIRY_INTERVAL)).thenReturn(60L);
        
        AtomicBoolean expired = new AtomicBoolean(false);
        handler.registerMessage(propertySet, () -> expired.set(true));
        
        assertEquals(1, handler.getPendingCount());
        assertFalse(expired.get());
    }
    
    @Test
    void testRegisterMessageWithoutExpiry() {
        QttPropertySet propertySet = mock(QttPropertySet.class);
        when(propertySet.getProperty(QttProperty.MESSAGE_EXPIRY_INTERVAL)).thenReturn(0L);
        
        handler.registerMessage(propertySet, () -> {});
        
        assertEquals(0, handler.getPendingCount());
    }
    
    @Test
    void testRegisterMessageWithNullPropertySet() {
        handler.registerMessage(null, () -> {});
        
        assertEquals(0, handler.getPendingCount());
    }
    
    @Test
    void testRegisterNullMessage() {
        handler.registerMessage(null, () -> {});
        
        assertEquals(0, handler.getPendingCount());
    }
    
    @Test
    void testProcessExpiredMessages() throws InterruptedException {
        QttPropertySet propertySet = mock(QttPropertySet.class);
        when(propertySet.getProperty(QttProperty.MESSAGE_EXPIRY_INTERVAL)).thenReturn(1L);
        
        AtomicBoolean expired = new AtomicBoolean(false);
        handler.registerMessage(propertySet, () -> expired.set(true));
        
        Thread.sleep(1100);
        
        handler.processExpiredMessages();
        
        assertTrue(expired.get());
        assertEquals(0, handler.getPendingCount());
    }
    
    @Test
    void testCleanup() {
        QttPropertySet propertySet = mock(QttPropertySet.class);
        when(propertySet.getProperty(QttProperty.MESSAGE_EXPIRY_INTERVAL)).thenReturn(60L);
        
        handler.registerMessage(propertySet, () -> {});
        assertEquals(1, handler.getPendingCount());
        
        handler.cleanup();
        assertEquals(0, handler.getPendingCount());
    }
}
