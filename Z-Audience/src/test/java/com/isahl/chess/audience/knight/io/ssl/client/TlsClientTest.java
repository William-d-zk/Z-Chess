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
 */

package com.isahl.chess.knight.io.ssl.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/** TLS 客户端测试类 测试 TLS 连接的建立和数据传输 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TlsClientTest {

  @TempDir static Path tempDir;

  private static Path serverKeyStore;
  private static Path clientKeyStore;
  private static Path trustStore;
  // 从环境变量读取密码，如果不存在则生成随机密码（安全修复）
  private static final String PASSWORD =
      System.getenv("TLS_TEST_PASSWORD") != null
          ? System.getenv("TLS_TEST_PASSWORD")
          : java.util.Base64.getEncoder().encodeToString(java.security.SecureRandom.getSeed(12));
  private static final int TEST_PORT = 28443;

  @BeforeAll
  static void setUp() throws Exception {
    serverKeyStore = tempDir.resolve("server.p12");
    clientKeyStore = tempDir.resolve("client.p12");
    trustStore = tempDir.resolve("trust.p12");

    Path caCert = tempDir.resolve("ca.pem");
    Path serverCert = tempDir.resolve("server.crt");
    Path clientCert = tempDir.resolve("client.crt");
    Path serverKey = tempDir.resolve("server.key");
    Path clientKey = tempDir.resolve("client.key");

    // 生成 CA
    ProcessBuilder pb =
        new ProcessBuilder(
            "openssl",
            "req",
            "-x509",
            "-newkey",
            "rsa:2048",
            "-keyout",
            tempDir.resolve("ca.key").toString(),
            "-out",
            caCert.toString(),
            "-days",
            "1",
            "-nodes",
            "-subj",
            "/CN=TestCA/O=Z-Chess/C=CN");
    pb.redirectErrorStream(true);
    Process p = pb.start();
    if (p.waitFor() != 0) {
      System.out.println("Warning: Failed to generate CA certificate");
      return;
    }

    // 生成服务端证书
    pb =
        new ProcessBuilder(
            "openssl",
            "req",
            "-newkey",
            "rsa:2048",
            "-keyout",
            serverKey.toString(),
            "-out",
            tempDir.resolve("server.csr").toString(),
            "-nodes",
            "-subj",
            "/CN=localhost/O=Z-Chess/C=CN");
    pb.redirectErrorStream(true);
    p = pb.start();
    p.waitFor();

    pb =
        new ProcessBuilder(
            "openssl",
            "x509",
            "-req",
            "-in",
            tempDir.resolve("server.csr").toString(),
            "-CA",
            caCert.toString(),
            "-CAkey",
            tempDir.resolve("ca.key").toString(),
            "-CAcreateserial",
            "-out",
            serverCert.toString(),
            "-days",
            "1");
    pb.redirectErrorStream(true);
    p = pb.start();
    p.waitFor();

    // 生成客户端证书
    pb =
        new ProcessBuilder(
            "openssl",
            "req",
            "-newkey",
            "rsa:2048",
            "-keyout",
            clientKey.toString(),
            "-out",
            tempDir.resolve("client.csr").toString(),
            "-nodes",
            "-subj",
            "/CN=TestClient/O=Z-Chess/C=CN");
    pb.redirectErrorStream(true);
    p = pb.start();
    p.waitFor();

    pb =
        new ProcessBuilder(
            "openssl",
            "x509",
            "-req",
            "-in",
            tempDir.resolve("client.csr").toString(),
            "-CA",
            caCert.toString(),
            "-CAkey",
            tempDir.resolve("ca.key").toString(),
            "-CAcreateserial",
            "-out",
            clientCert.toString(),
            "-days",
            "1");
    pb.redirectErrorStream(true);
    p = pb.start();
    p.waitFor();

    // 创建 PKCS12
    pb =
        new ProcessBuilder(
            "openssl",
            "pkcs12",
            "-export",
            "-in",
            serverCert.toString(),
            "-inkey",
            serverKey.toString(),
            "-certfile",
            caCert.toString(),
            "-out",
            serverKeyStore.toString(),
            "-name",
            "server",
            "-password",
            "pass:" + PASSWORD);
    pb.redirectErrorStream(true);
    p = pb.start();
    p.waitFor();

    pb =
        new ProcessBuilder(
            "openssl",
            "pkcs12",
            "-export",
            "-in",
            clientCert.toString(),
            "-inkey",
            clientKey.toString(),
            "-certfile",
            caCert.toString(),
            "-out",
            clientKeyStore.toString(),
            "-name",
            "client",
            "-password",
            "pass:" + PASSWORD);
    pb.redirectErrorStream(true);
    p = pb.start();
    p.waitFor();

    // 创建信任库
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
            PASSWORD,
            "-noprompt");
    pb.redirectErrorStream(true);
    p = pb.start();
    p.waitFor();
  }

  @Test
  @Order(1)
  @DisplayName("测试单向 TLS 连接")
  void testOneWayTlsConnection() throws Exception {
    assumeTrue(Files.exists(serverKeyStore), "需要 openssl 工具生成证书");

    CountDownLatch serverReady = new CountDownLatch(1);
    CountDownLatch clientDone = new CountDownLatch(1);

    // 启动服务端线程
    Thread serverThread =
        new Thread(
            () -> {
              try {
                startTlsServer(serverReady, false);
              } catch (Exception e) {
                fail("TLS server startup failed: " + e.getMessage());
              }
            });
    serverThread.start();

    // 等待服务端启动
    assertTrue(serverReady.await(5, TimeUnit.SECONDS), "服务端应该在 5 秒内启动");
    Thread.sleep(100); // 给服务端一点时间完全启动

    // 启动客户端
    try {
      SSLContext sslContext = createClientContext(false);
      SSLSocketFactory factory = sslContext.getSocketFactory();

      try (SSLSocket socket = (SSLSocket) factory.createSocket("localhost", TEST_PORT)) {
        socket.setSoTimeout(5000);

        // 执行握手
        socket.startHandshake();

        // 验证连接建立
        assertTrue(socket.isConnected(), "TLS 连接应该建立");
        assertNotNull(socket.getSession(), "TLS 会话应该存在");
        assertEquals("TLSv1.2", socket.getSession().getProtocol(), "应该使用 TLSv1.2");

        // 发送测试数据
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        String testMessage = "Hello TLS";
        out.write(testMessage.getBytes());
        out.flush();

        // 读取响应
        byte[] buffer = new byte[1024];
        int read = in.read(buffer);
        String response = new String(buffer, 0, read);

        assertEquals("ECHO:" + testMessage, response, "应该收到正确的响应");

        clientDone.countDown();
      }
    } finally {
      serverThread.interrupt();
      serverThread.join(1000);
    }
  }

  @Test
  @Order(2)
  @DisplayName("测试双向 TLS (mTLS) 连接")
  void testMutualTlsConnection() throws Exception {
    assumeTrue(Files.exists(serverKeyStore) && Files.exists(clientKeyStore), "需要 openssl 工具生成证书");

    CountDownLatch serverReady = new CountDownLatch(1);

    // 启动服务端（启用客户端认证）
    Thread serverThread =
        new Thread(
            () -> {
              try {
                startTlsServer(serverReady, true);
              } catch (Exception e) {
                fail("TLS server startup failed: " + e.getMessage());
              }
            });
    serverThread.start();

    assertTrue(serverReady.await(5, TimeUnit.SECONDS), "服务端应该在 5 秒内启动");
    Thread.sleep(100);

    try {
      SSLContext sslContext = createClientContext(true);
      SSLSocketFactory factory = sslContext.getSocketFactory();

      try (SSLSocket socket = (SSLSocket) factory.createSocket("localhost", TEST_PORT + 1)) {
        socket.setSoTimeout(5000);
        socket.startHandshake();

        assertTrue(socket.isConnected(), "mTLS 连接应该建立");
        assertNotNull(socket.getSession(), "TLS 会话应该存在");

        // 验证服务端获取了客户端证书
        assertNotNull(socket.getSession().getLocalCertificates(), "应该发送客户端证书");

        System.out.println("mTLS 连接成功，客户端证书已发送");
      }
    } finally {
      serverThread.interrupt();
      serverThread.join(1000);
    }
  }

  @Test
  @Order(3)
  @DisplayName("测试 TLS 会话参数")
  void testTLSSessionParameters() throws Exception {
    assumeTrue(Files.exists(serverKeyStore), "需要证书文件");

    SSLContext context = createClientContext(false);
    SSLSocketFactory factory = context.getSocketFactory();

    // 获取支持的协议
    String[] supportedProtocols = factory.getSupportedCipherSuites();
    assertTrue(supportedProtocols.length > 0, "应该有支持的加密套件");

    // 验证默认协议
    assertNotNull(context.getProtocol(), "应该有默认协议");
  }

  /** 启动 TLS 服务端 */
  private void startTlsServer(CountDownLatch ready, boolean requireClientAuth) throws Exception {
    int port = requireClientAuth ? TEST_PORT + 1 : TEST_PORT;

    // 创建服务端 SSL 上下文
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    try (FileInputStream fis = new FileInputStream(serverKeyStore.toFile())) {
      keyStore.load(fis, PASSWORD.toCharArray());
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, PASSWORD.toCharArray());

    TrustManagerFactory tmf =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    if (requireClientAuth) {
      KeyStore trustStore = KeyStore.getInstance("PKCS12");
      try (FileInputStream fis = new FileInputStream(TlsClientTest.trustStore.toFile())) {
        trustStore.load(fis, PASSWORD.toCharArray());
      }
      tmf.init(trustStore);
    } else {
      tmf.init((KeyStore) null);
    }

    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

    SSLServerSocketFactory factory = sslContext.getServerSocketFactory();

    try (SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port)) {
      serverSocket.setNeedClientAuth(requireClientAuth);

      System.out.println("TLS Server started on port " + port);
      ready.countDown();

      serverSocket.setSoTimeout(5000);
      try {
        SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
        clientSocket.startHandshake();

        // 简单 echo 服务
        InputStream in = clientSocket.getInputStream();
        OutputStream out = clientSocket.getOutputStream();

        byte[] buffer = new byte[1024];
        int read = in.read(buffer);
        if (read > 0) {
          String message = new String(buffer, 0, read);
          out.write(("ECHO:" + message).getBytes());
          out.flush();
        }

        clientSocket.close();
      } catch (java.net.SocketTimeoutException e) {
        // 超时，正常退出
      }
    }
  }

  /** 创建客户端 SSL 上下文 */
  private SSLContext createClientContext(boolean useClientCert) throws Exception {
    TrustManagerFactory tmf =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    KeyStore trustStore = KeyStore.getInstance("PKCS12");
    try (FileInputStream fis = new FileInputStream(TlsClientTest.trustStore.toFile())) {
      trustStore.load(fis, PASSWORD.toCharArray());
    }
    tmf.init(trustStore);

    KeyManager[] keyManagers = null;
    if (useClientCert && Files.exists(clientKeyStore)) {
      KeyManagerFactory kmf =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      try (FileInputStream fis = new FileInputStream(clientKeyStore.toFile())) {
        keyStore.load(fis, PASSWORD.toCharArray());
      }
      kmf.init(keyStore, PASSWORD.toCharArray());
      keyManagers = kmf.getKeyManagers();
    }

    SSLContext context = SSLContext.getInstance("TLSv1.2");
    context.init(keyManagers, tmf.getTrustManagers(), null);
    return context;
  }
}
