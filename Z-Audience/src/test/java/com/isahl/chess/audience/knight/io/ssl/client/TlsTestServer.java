/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 *
 * TLS 测试服务端
 */

package com.isahl.chess.knight.io.ssl.client;

import java.io.*;
import java.security.KeyStore;
import javax.net.ssl.*;

/**
 * TLS 测试服务端
 *
 * <p>用法: java TlsTestServer --port 28443 --keystore cert/server.p12 --password changeit
 */
public class TlsTestServer {

  private int port = 28443;
  private String keyStorePath;
  private String password = System.getProperty("tls.test.keystore.password", "changeit");
  private boolean needClientAuth = false;

  public static void main(String[] args) {
    TlsTestServer server = new TlsTestServer();

    if (!server.parseArgs(args)) {
      printUsage();
      System.exit(1);
    }

    try {
      server.start();
    } catch (Exception e) {
      System.err.println("服务端错误: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void printUsage() {
    System.out.println("TLS 测试服务端");
    System.out.println("用法: java TlsTestServer [选项]");
    System.out.println();
    System.out.println("选项:");
    System.out.println("  --port <port>           监听端口 (默认: 28443)");
    System.out.println("  --keystore <path>       密钥库路径 (PKCS12)");
    System.out.println("  --password <password>   密钥库密码 (默认: changeit)");
    System.out.println("  --client-auth           要求客户端证书");
  }

  private boolean parseArgs(String[] args) {
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--port":
          port = Integer.parseInt(args[++i]);
          break;
        case "--keystore":
          keyStorePath = args[++i];
          break;
        case "--password":
          password = args[++i];
          break;
        case "--client-auth":
          needClientAuth = true;
          break;
        default:
          System.err.println("未知选项: " + args[i]);
          return false;
      }
    }

    if (keyStorePath == null) {
      System.err.println("错误: 必须指定密钥库路径");
      return false;
    }

    return true;
  }

  private void start() throws Exception {
    System.out.println("========================================");
    System.out.println("Z-Chess TLS 测试服务端");
    System.out.println("========================================");
    System.out.println("端口: " + port);
    System.out.println("密钥库: " + keyStorePath);
    System.out.println("客户端认证: " + (needClientAuth ? "是" : "否"));
    System.out.println("----------------------------------------");

    // 加载密钥库
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    try (FileInputStream fis = new FileInputStream(keyStorePath)) {
      keyStore.load(fis, password.toCharArray());
    }

    // 创建密钥管理器
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, password.toCharArray());

    // 创建 SSL 上下文
    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(kmf.getKeyManagers(), null, null);

    // 创建服务端 Socket
    SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
    SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port);
    serverSocket.setNeedClientAuth(needClientAuth);

    System.out.println("服务端已启动，等待连接...");
    System.out.println("按 Ctrl+C 停止");
    System.out.println();

    // 设置超时，以便可以优雅地退出
    serverSocket.setSoTimeout(30000); // 30秒超时

    while (true) {
      try {
        SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
        System.out.println("客户端连接: " + clientSocket.getInetAddress());

        // 执行握手
        clientSocket.startHandshake();
        System.out.println("TLS 握手成功");

        // 打印会话信息
        SSLSession session = clientSocket.getSession();
        System.out.println("  协议: " + session.getProtocol());
        System.out.println("  加密套件: " + session.getCipherSuite());

        // 简单 echo 服务
        handleClient(clientSocket);

      } catch (java.net.SocketTimeoutException e) {
        // 超时，继续循环
        System.out.println("等待连接超时...");
      }
    }
  }

  private void handleClient(SSLSocket socket) {
    try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println("收到: " + line);
        writer.println("ECHO: " + line);

        if ("quit".equalsIgnoreCase(line)) {
          break;
        }
      }
    } catch (IOException e) {
      System.out.println("客户端断开: " + e.getMessage());
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        // 忽略
      }
    }
  }
}
