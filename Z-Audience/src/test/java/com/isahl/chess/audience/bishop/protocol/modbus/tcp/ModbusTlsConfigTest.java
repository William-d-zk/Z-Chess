/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.tcp;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.protocol.modbus.tcp.ModbusTlsConfig;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;

class ModbusTlsConfigTest {

  @Test
  void testBuilder_defaults() {
    ModbusTlsConfig config = ModbusTlsConfig.builder().build();

    assertEquals("TLSv1.3", config.getProtocol());
    assertNull(config.getKeyStorePath());
    assertNull(config.getKeyStorePassword());
    assertArrayEquals(new String[] {"TLSv1.3", "TLSv1.2"}, config.getEnabledProtocols());
    assertFalse(config.isClientAuthRequired());
  }

  @Test
  void testBuilder_custom() {
    ModbusTlsConfig config =
        ModbusTlsConfig.builder()
            .protocol("TLSv1.2")
            .enabledProtocols("TLSv1.2", "TLSv1.3")
            .enabledCipherSuites("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384")
            .clientAuthRequired(true)
            .build();

    assertEquals("TLSv1.2", config.getProtocol());
    assertEquals(2, config.getEnabledProtocols().length);
    assertTrue(config.isClientAuthRequired());
  }

  @Test
  void testCreateSSLContext_withoutKeyStore() throws Exception {
    ModbusTlsConfig config = ModbusTlsConfig.builder().protocol("TLSv1.3").build();

    SSLContext sslContext = config.createSSLContext();

    assertNotNull(sslContext);
    assertEquals("TLSv1.3", sslContext.getProtocol());
  }

  @Test
  void testCreateSSLContext_withInvalidKeyStore() {
    ModbusTlsConfig config =
        ModbusTlsConfig.builder()
            .keyStore("/nonexistent/keystore.jks", "password".toCharArray())
            .build();

    assertThrows(
        Exception.class,
        () -> {
          config.createSSLContext();
        });
  }
}
