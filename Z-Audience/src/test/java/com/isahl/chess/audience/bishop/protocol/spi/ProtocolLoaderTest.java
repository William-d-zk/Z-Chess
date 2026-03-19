/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.spi;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.protocol.spi.ProtocolHandler;
import com.isahl.chess.bishop.protocol.spi.ProtocolLoader;
import com.isahl.chess.bishop.protocol.spi.example.ExampleTextProtocolHandler;
import com.isahl.chess.king.base.content.ByteBuf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProtocolLoaderTest {

  private ProtocolLoader loader;

  @BeforeEach
  void setUp() {
    loader = ProtocolLoader.getInstance();
    loader.clear();
  }

  @Test
  void testRegisterHandler() {
    ProtocolHandler handler = new ExampleTextProtocolHandler();
    loader.register(handler);

    ProtocolHandler retrieved = loader.getHandler("ExampleText");
    assertNotNull(retrieved);
    assertEquals("ExampleText", retrieved.getProtocolName());
    assertEquals("1.0.0", retrieved.getProtocolVersion());
  }

  @Test
  void testEncodeDecode() {
    ProtocolHandler handler = new ExampleTextProtocolHandler();

    String original = "Hello, Protocol SPI!";
    byte[] encoded = handler.encode(original);

    ByteBuf buffer = ByteBuf.allocate(encoded.length);
    buffer.put(encoded);

    Object decoded = handler.decode(buffer);

    assertNotNull(decoded);
    assertEquals(original, decoded);
  }

  @Test
  void testIdentifyBySignature() {
    ProtocolHandler handler = new ExampleTextProtocolHandler();
    loader.register(handler);

    byte[] signature = handler.getProtocolSignature();
    ProtocolHandler identified = loader.identify(signature);

    assertNotNull(identified);
    assertEquals("ExampleText", identified.getProtocolName());
  }

  @Test
  void testIdentifyUnknownSignature() {
    ProtocolHandler handler = new ExampleTextProtocolHandler();
    loader.register(handler);

    byte[] unknownSignature = {'U', 'N', 'K', 'N'};
    ProtocolHandler identified = loader.identify(unknownSignature);

    assertNull(identified);
  }

  @Test
  void testUnregisterHandler() {
    ProtocolHandler handler = new ExampleTextProtocolHandler();
    loader.register(handler);

    loader.unregister("ExampleText");

    ProtocolHandler retrieved = loader.getHandler("ExampleText");
    assertNull(retrieved);
  }

  @Test
  void testGetAllHandlers() {
    loader.register(new ExampleTextProtocolHandler());

    var handlers = loader.getAllHandlers();
    assertEquals(1, handlers.size());
  }

  @Test
  void testDecodeIncompleteData() {
    ProtocolHandler handler = new ExampleTextProtocolHandler();

    ByteBuf buffer = ByteBuf.allocate(1);
    buffer.put((byte) 0x00);

    Object decoded = handler.decode(buffer);
    assertNull(decoded);
  }
}
