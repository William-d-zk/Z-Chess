/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.knight.io.ssl;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.isahl.chess.knight.cluster.config.SslSocketConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/** ZChat TLS 集成测试 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ZChatTlsIntegrationTest {

  private static final String TEST_PASSWORD = System.getProperty("tls.test.password", "test123");

  @TempDir static Path tempDir;

  private static SslSocketConfig sslConfig;
  private static boolean certificatesGenerated = false;

  @BeforeAll
  static void setUp() throws Exception {
    Path serverKeyStore = tempDir.resolve("server.p12");
    Path trustStore = tempDir.resolve("trust.p12");
    Path caCert = tempDir.resolve("ca.pem");
    Path serverKey = tempDir.resolve("server.key");

    ProcessBuilder pb =
        new ProcessBuilder(
            "openssl",
            "req",
            "-x509",
            "-newkey",
            "rsa:2048",
            "-keyout",
            serverKey.toString(),
            "-out",
            caCert.toString(),
            "-days",
            "1",
            "-nodes",
            "-subj",
            "/CN=zchat-test-server/O=Z-Chess/C=CN");
    pb.redirectErrorStream(true);
    Process p = pb.start();

    if (p.waitFor() != 0) {
      System.out.println("Warning: openssl not available, TLS integration tests will be skipped");
      return;
    }

    pb =
        new ProcessBuilder(
            "openssl",
            "pkcs12",
            "-export",
            "-in",
            caCert.toString(),
            "-inkey",
            serverKey.toString(),
            "-out",
            serverKeyStore.toString(),
            "-name",
            "server",
            "-password",
            "pass:" + TEST_PASSWORD);
    pb.redirectErrorStream(true);
    p = pb.start();
    p.waitFor();

    pb =
        new ProcessBuilder(
            "keytool",
            "-import",
            "-alias",
            "ca",
            "-file",
            caCert.toString(),
            "-keystore",
            trustStore.toString(),
            "-storetype",
            "PKCS12",
            "-storepass",
            TEST_PASSWORD,
            "-noprompt");
    pb.redirectErrorStream(true);
    p = pb.start();
    p.waitFor();

    if (Files.exists(serverKeyStore)) {
      certificatesGenerated = true;

      sslConfig = new SslSocketConfig();
      sslConfig.getProvider().setEnabled(true);
      sslConfig.getProvider().setKeyStorePath(serverKeyStore.toString());
      sslConfig.getProvider().setKeyStorePassword(TEST_PASSWORD);
      sslConfig.getProvider().setTrustStorePath(trustStore.toString());
      sslConfig.getProvider().setTrustStorePassword(TEST_PASSWORD);
      sslConfig.getProvider().setProtocol("TLSv1.2");
      sslConfig.init();
    }
  }

  @Test
  @Order(1)
  @DisplayName("测试 TLS 配置与 ZChat 集成")
  void testTlsConfigWithZChat() {
    assumeTrue(certificatesGenerated, "需要证书文件");

    assertNotNull(sslConfig, "SSL 配置应该存在");
    assertTrue(sslConfig.getProvider().isEnabled(), "服务端 TLS 应该启用");
    assertNotNull(sslConfig.getProviderSslContext(), "服务端 SSL 上下文应该存在");
    assertEquals("TLSv1.2", sslConfig.getProvider().getProtocol(), "应该配置为 TLSv1.2");
    assertTrue(sslConfig.getSslPacketBufferSize() > 0, "数据包缓冲区应该大于 0");
    assertTrue(sslConfig.getSslAppBufferSize() > 0, "应用缓冲区应该大于 0");
  }

  @Test
  @Order(2)
  @DisplayName("测试双向 TLS 配置")
  void testMutualTlsWithZChat() {
    assumeTrue(certificatesGenerated, "需要证书文件");

    sslConfig.getProvider().setClientAuth(true);

    assertTrue(sslConfig.getProvider().isClientAuth(), "应该需要客户端认证");
  }

  @Test
  @Order(3)
  @DisplayName("测试 TLS 会话配置")
  void testTlsSessionConfig() {
    assumeTrue(certificatesGenerated, "需要证书文件");

    assertTrue(sslConfig.isEnableSessionCreation(), "应该启用会话创建");
    assertEquals(300, sslConfig.getSessionTimeout(), "默认超时应该是 300 秒");

    sslConfig.setSessionTimeout(600);
    assertEquals(600, sslConfig.getSessionTimeout(), "超时应该可以修改");
  }

  @Test
  @Order(4)
  @DisplayName("测试集群 TLS 配置")
  void testClusterTlsConfig() {
    SslSocketConfig config = new SslSocketConfig();
    config.getCluster().setEnabled(true);
    config.getCluster().setClientAuth(true);
    config.getCluster().setKeyStorePath("cert/cluster.p12");
    config.getCluster().setKeyStorePassword("cluster123");

    assertTrue(config.getCluster().isEnabled(), "集群 TLS 应该启用");
    assertTrue(config.getCluster().isClientAuth(), "集群应该启用双向认证");
  }

  @Test
  @Order(5)
  @DisplayName("测试加密套件配置")
  void testCipherConfiguration() {
    SslSocketConfig.SslConfig providerConfig = new SslSocketConfig.SslConfig();

    java.util.List<String> ciphers =
        java.util.Arrays.asList(
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
    providerConfig.setCiphers(ciphers);

    assertNotNull(providerConfig.getCiphers(), "加密套件应该存在");
    assertEquals(2, providerConfig.getCiphers().size());
    assertTrue(providerConfig.getCiphers().contains("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"));
  }
}
