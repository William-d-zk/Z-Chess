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

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.mqtt.v5.LastWillHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LastWillHandlerTest {

  private LastWillHandler handler;

  @BeforeEach
  void setUp() {
    handler = new LastWillHandler();
  }

  @Test
  void testRegisterLastWill() {
    handler.registerLastWill("client1", "status/offline", "offline".getBytes(), 1, true);

    assertEquals(1, handler.size());
  }

  @Test
  void testNormalDisconnectCancelsWill() {
    handler.registerLastWill("client1", "status/offline", "offline".getBytes(), 1, true);

    handler.onNormalDisconnect("client1");

    assertEquals(0, handler.size());
  }

  @Test
  void testAbnormalDisconnectPublishesWill() {
    AtomicBoolean published = new AtomicBoolean(false);
    handler.setPublishCallback(
        (topic, payload) -> {
          published.set(true);
          assertEquals("status/offline", topic);
        });

    handler.registerLastWill("client1", "status/offline", "offline".getBytes(), 1, true);

    handler.onAbnormalDisconnect("client1");

    assertTrue(published.get());
    assertEquals(0, handler.size());
  }

  @Test
  void testRemoveLastWill() {
    handler.registerLastWill("client1", "status/offline", "offline".getBytes(), 1, true);

    handler.removeLastWill("client1");

    assertEquals(0, handler.size());
  }

  @Test
  void testAbnormalDisconnectNoWill() {
    AtomicBoolean published = new AtomicBoolean(false);
    handler.setPublishCallback((topic, payload) -> published.set(true));

    handler.onAbnormalDisconnect("unknown_client");

    assertFalse(published.get());
  }
}
