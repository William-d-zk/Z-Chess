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

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * TLS 测试客户端
 * 
 * 用法:
 * 1. 单向 TLS: java TlsTestClient --host localhost --port 1883 --trust-store cert/trust.p12 --password <PASSWORD>
 * 2. 双向 TLS: java TlsTestClient --host localhost --port 1883 --trust-store cert/trust.p12 --key-store cert/client.p12 --password <PASSWORD>
 * 
 * @author william.d.zk
 */
public class TlsTestClient {

    private String host;
    private int port;
    private String trustStorePath;
    private String keyStorePath;
    private String password;
    private boolean verbose = false;
    private String protocol = "TLSv1.2";

    public static void main(String[] args) {
        TlsTestClient client = new TlsTestClient();
        
        if (!client.parseArgs(args)) {
            printUsage();
            System.exit(1);
        }

        try {
            client.connect();
        } catch (Exception e) {
            System.err.println("连接失败: " + e.getMessage());
            if (client.verbose) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Z-Chess TLS 测试客户端");
        System.out.println("用法: java TlsTestClient [选项]");
        System.out.println();
        System.out.println("选项:");
        System.out.println("  --host <hostname>         服务器地址 (默认: localhost)");
        System.out.println("  --port <port>             服务器端口 (默认: 1883)");
        System.out.println("  --trust-store <path>      信任库路径 (PKCS12)");
        System.out.println("  --key-store <path>        密钥库路径 (PKCS12, 双向 TLS 需要)");
        System.out.println("  --password <password>     密钥库密码");
        System.out.println("  --protocol <protocol>     TLS 协议版本 (默认: TLSv1.2)");
        System.out.println("  -v, --verbose             详细输出");
        System.out.println("  -h, --help                显示帮助");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  # 单向 TLS");
        System.out.println("  java TlsTestClient --host localhost --port 1883 \\\n");
        System.out.println("    --trust-store cert/trust.p12 --password <PASSWORD>");
        System.out.println();
        System.out.println("  # 双向 TLS");
        System.out.println("  java TlsTestClient --host localhost --port 1883 \\\n");
        System.out.println("    --trust-store cert/trust.p12 \\\n");
        System.out.println("    --key-store cert/client.p12 \\\n");
        System.out.println("    --password <PASSWORD>");
    }

    private boolean parseArgs(String[] args) {
        // 默认值
        host = "localhost";
        port = 1883;
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h", "--help" -> {
                    return false;
                }
                case "--host" -> host = args[++i];
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--trust-store" -> trustStorePath = args[++i];
                case "--key-store" -> keyStorePath = args[++i];
                case "--password" -> password = args[++i];
                case "--protocol" -> protocol = args[++i];
                case "-v", "--verbose" -> verbose = true;
                default -> {
                    System.err.println("未知选项: " + args[i]);
                    return false;
                }
            }
        }

        if (trustStorePath == null || password == null) {
            System.err.println("错误: 必须指定信任库路径和密码");
            return false;
        }

        return true;
    }

    private void connect() throws Exception {
        println("========================================");
        println("Z-Chess TLS Test Client");
        println("========================================");
        println("服务器: " + host + ":" + port);
        println("协议: " + protocol);
        println("信任库: " + trustStorePath);
        if (keyStorePath != null) {
            println("密钥库: " + keyStorePath + " (双向 TLS)");
        } else {
            println("认证方式: 单向 TLS");
        }
        println("----------------------------------------");

        // 创建 SSL 上下文
        SSLContext sslContext = createSSLContext();
        SSLSocketFactory factory = sslContext.getSocketFactory();

        // 创建连接
        println("正在连接...");
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            socket.setSoTimeout(10000);
            
            // 设置启用的协议
            socket.setEnabledProtocols(new String[]{protocol});
            
            // 执行握手
            println("执行 TLS 握手...");
            socket.startHandshake();
            
            // 连接成功
            println("✓ TLS 连接建立成功!");
            println("");
            
            // 打印会话信息
            SSLSession session = socket.getSession();
            printSessionInfo(session);
            
            // 交互模式
            interactiveMode(socket);
        }
    }

    private SSLContext createSSLContext() throws Exception {
        // 加载信任库
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(trustStorePath)) {
            trustStore.load(fis, password.toCharArray());
        }
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 加载密钥库（双向 TLS）
        KeyManager[] keyManagers = null;
        if (keyStorePath != null) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keyStorePath)) {
                keyStore.load(fis, password.toCharArray());
            }
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, password.toCharArray());
            keyManagers = kmf.getKeyManagers();
            
            if (verbose) {
                println("已加载客户端证书");
            }
        }

        // 创建 SSL 上下文
        SSLContext context = SSLContext.getInstance(protocol);
        context.init(keyManagers, tmf.getTrustManagers(), null);
        
        return context;
    }

    private void printSessionInfo(SSLSession session) {
        println("TLS 会话信息:");
        println("  协议版本: " + session.getProtocol());
        println("  加密套件: " + session.getCipherSuite());
        
        try {
            X509Certificate[] certs = (X509Certificate[]) session.getPeerCertificates();
            if (certs != null && certs.length > 0) {
                println("  服务端证书:");
                X509Certificate serverCert = certs[0];
                println("    主题: " + serverCert.getSubjectX500Principal());
                println("    颁发者: " + serverCert.getIssuerX500Principal());
                println("    有效期: " + serverCert.getNotBefore() + " ~ " + serverCert.getNotAfter());
                println("    序列号: " + serverCert.getSerialNumber());
            }
        } catch (SSLPeerUnverifiedException e) {
            println("  服务端证书: 未验证");
        }

        if (session.getLocalCertificates() != null) {
            println("  客户端证书: 已发送 (" + session.getLocalCertificates().length + " 张)");
        }
        
        println("");
    }

    private void interactiveMode(SSLSocket socket) {
        println("进入交互模式 (输入 'quit' 退出):");
        println("----------------------------------------");
        
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream()
        ) {
            while (true) {
                System.out.print("> ");
                String line = reader.readLine();
                
                if (line == null || "quit".equalsIgnoreCase(line)) {
                    println("断开连接...");
                    break;
                }
                
                // 发送数据
                out.write(line.getBytes());
                out.write('\n');
                out.flush();
                
                // 读取响应
                byte[] buffer = new byte[4096];
                int read = in.read(buffer);
                if (read > 0) {
                    String response = new String(buffer, 0, read);
                    println("< " + response.trim());
                } else {
                    println("< 无响应");
                }
            }
        } catch (IOException e) {
            println("连接错误: " + e.getMessage());
        }
    }

    private void println(String message) {
        System.out.println(message);
    }
}
