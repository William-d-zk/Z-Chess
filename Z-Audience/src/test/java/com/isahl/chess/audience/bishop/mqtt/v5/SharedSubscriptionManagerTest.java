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

import com.isahl.chess.queen.io.core.net.socket.AioSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SharedSubscriptionManagerTest {

    private SharedSubscriptionManager manager;
    private AioSession<?> session1;
    private AioSession<?> session2;

    @BeforeEach
    void setUp() {
        manager = new SharedSubscriptionManagerImpl();
        session1 = mock(AioSession.class);
        session2 = mock(AioSession.class);
        when(session1.index()).thenReturn(1L);
        when(session2.index()).thenReturn(2L);
    }

    @Test
    void testAddSubscription() {
        SharedSubscription sub = new SharedSubscription("group1", "sensors/temp", 1);
        manager.addSubscription("group1", sub, session1);

        assertEquals(1, manager.getConsumers("group1").size());
        assertTrue(manager.getConsumers("group1").contains(session1));
    }

    @Test
    void testSelectConsumerRoundRobin() {
        SharedSubscription sub = new SharedSubscription("group1", "sensors/temp", 1);
        manager.addSubscription("group1", sub, session1);
        manager.addSubscription("group1", sub, session2);

        AioSession<?> selected1 = manager.selectConsumer("group1");
        AioSession<?> selected2 = manager.selectConsumer("group1");

        assertNotNull(selected1);
        assertNotNull(selected2);
        assertNotSame(selected1, selected2);
    }

    @Test
    void testRemoveSubscription() {
        SharedSubscription sub = new SharedSubscription("group1", "sensors/temp", 1);
        manager.addSubscription("group1", sub, session1);
        manager.removeSubscription("group1", "sensors/temp", session1);

        assertEquals(0, manager.getConsumers("group1").size());
    }

    @Test
    void testCleanupSession() {
        SharedSubscription sub1 = new SharedSubscription("group1", "sensors/temp", 1);
        SharedSubscription sub2 = new SharedSubscription("group2", "sensors/humidity", 1);
        manager.addSubscription("group1", sub1, session1);
        manager.addSubscription("group2", sub2, session1);

        manager.cleanupSession(session1);

        assertEquals(0, manager.getConsumers("group1").size());
        assertEquals(0, manager.getConsumers("group2").size());
    }

    @Test
    void testSharedSubscriptionValidation() {
        assertThrows(IllegalArgumentException.class, () ->
            new SharedSubscription("", "topic", 1));

        assertThrows(IllegalArgumentException.class, () ->
            new SharedSubscription("group", "", 1));

        assertThrows(IllegalArgumentException.class, () ->
            new SharedSubscription(null, "topic", 1));

        assertThrows(IllegalArgumentException.class, () ->
            new SharedSubscription("group", null, 1));
    }

    @Test
    void testSharedSubscriptionGetFullTopic() {
        SharedSubscription sub = new SharedSubscription("group1", "sensors/temp", 1);
        assertEquals("$share/group1/sensors/temp", sub.getFullTopic());
    }

    @Test
    void testSharedSubscriptionEqualsAndHashCode() {
        SharedSubscription sub1 = new SharedSubscription("group1", "topic", 1);
        SharedSubscription sub2 = new SharedSubscription("group1", "topic", 2);

        assertEquals(sub1, sub2);
        assertEquals(sub1.hashCode(), sub2.hashCode());
    }
}
