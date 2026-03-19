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

package com.isahl.chess.audience.queen.io.udp;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.net.udp.BaseUdpClient;
import com.isahl.chess.queen.io.core.net.udp.BaseUdpServer;
import com.isahl.chess.queen.io.core.net.udp.UdpPacket;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UdpCommunicationTest {

  private BaseUdpServer server;
  private BaseUdpClient client;

  @BeforeEach
  void setUp() throws Exception {
    server = new BaseUdpServer("127.0.0.1", 19999);
    server.startReceiveLoop();

    client = new BaseUdpClient("127.0.0.1", 19999);
    client.startReceiveLoop();

    Thread.sleep(100);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (client != null) {
      client.close();
    }
    if (server != null) {
      server.close();
    }
  }

  @Test
  void testUdpSendAndReceive() throws Exception {
    byte[] testData = "Hello UDP".getBytes();

    int sent = client.send(testData);
    assertEquals(testData.length, sent);

    Thread.sleep(100);

    UdpPacket received = server.receive();
    assertNotNull(received);
    assertEquals(testData.length, received.length());

    ByteBuf buffer = received.getBuffer();
    byte[] receivedData = new byte[buffer.readableBytes()];
    buffer.get(receivedData);

    assertArrayEquals(testData, receivedData);
  }

  @Test
  void testServerSendClientReceive() throws Exception {
    byte[] testData = "Server to Client".getBytes();

    Thread.sleep(50);

    InetSocketAddress clientAddress = client.localAddress();
    int sent = server.send(testData, clientAddress);

    Thread.sleep(200);

    UdpPacket received = client.receive();
    if (received != null) {
      ByteBuf buffer = received.getBuffer();
      byte[] receivedData = new byte[buffer.readableBytes()];
      buffer.get(receivedData);
      assertArrayEquals(testData, receivedData);
    }
  }

  @Test
  void testUdpChannelValid() {
    assertTrue(server.isValid());
    assertTrue(client.isValid());
  }

  @Test
  void testUdpChannelClose() throws Exception {
    client.close();
    Thread.sleep(50);
    assertFalse(client.isValid());
  }

  @Test
  void testLocalAddress() {
    assertNotNull(server.localAddress());
    assertNotNull(client.localAddress());
    assertEquals(19999, server.localAddress().getPort());
  }
}
