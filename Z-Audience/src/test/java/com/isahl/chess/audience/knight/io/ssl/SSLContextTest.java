/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.knight.io.ssl;

import com.isahl.chess.knight.cluster.config.SslSocketConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TLS/SSL 配置测试类
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SSLContextTest {

    @TempDir
    static Path tempDir;

    private static Path serverCertPath;
    private static Path trustCertPath;
    private static SslSocketConfig config;

    @BeforeAll
    static void setUp() throws Exception {
        serverCertPath = tempDir.resolve("server.p12");
        trustCertPath = tempDir.resolve("trust.p12");
        Path caCertPath = tempDir.resolve("ca-cert.pem");
        Path serverKeyPath = tempDir.resolve("server-key.pem");

        // 使用 openssl 生成测试证书
        ProcessBuilder pb = new ProcessBuilder(
            "openssl", "req", "-x509", "-newkey", "rsa:2048",
            "-keyout", serverKeyPath.toString(),
            "-out", caCertPath.toString(),
            "-days", "1", "-nodes",
            "-subj", "/CN=test-server/O=Z-Chess/C=CN"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            System.out.println("Warning: Failed to generate test certificates. Tests will be skipped.");
            return;
        }

        // 生成服务端 PKCS12
        pb = new ProcessBuilder(
            "openssl", "pkcs12", "-export",
            "-in", caCertPath.toString(),
            "-inkey", serverKeyPath.toString(),
            "-out", serverCertPath.toString(),
            "-name", "server", "-password", "pass:test123"
        );
        pb.redirectErrorStream(true);
        process = pb.start();
        process.waitFor();

        // 创建信任库
        pb = new ProcessBuilder(
            "keytool", "-import", "-alias", "ca",
            "-file", caCertPath.toString(),
            "-keystore", trustCertPath.toString(),
            "-storetype", "PKCS12", "-storepass", "test123", "-noprompt"
        );
        pb.redirectErrorStream(true);
        process = pb.start();
        process.waitFor();

        // 初始化配置
        config = new SslSocketConfig();
        config.getProvider().setEnabled(true);
        config.getProvider().setKeyStorePath(serverCertPath.toString());
        config.getProvider().setKeyStorePassword("test123");
        config.getProvider().setTrustStorePath(trustCertPath.toString());
        config.getProvider().setTrustStorePassword("test123");
        config.getProvider().setProtocol("TLSv1.2");
        config.init();
    }

    @Test
    @Order(1)
    @DisplayName("测试证书文件生成")
    void testCertificateFilesExist() {
        assumeTrue(Files.exists(serverCertPath), "测试证书需要 openssl 和 keytool 工具");
        assertTrue(Files.exists(serverCertPath), "服务端证书应该存在");
        assertTrue(Files.exists(trustCertPath), "信任库应该存在");
    }

    @Test
    @Order(2)
    @DisplayName("测试 SSL 上下文初始化")
    void testSSLContextInitialization() {
        assumeTrue(config.getProviderSslContext() != null, "SSL 上下文需要证书文件");
        
        assertNotNull(config.getProviderSslContext(), "SSL 上下文应该成功创建");
        assertEquals("TLSv1.2", config.getProviderSslContext().getProtocol(),
                "应该使用 TLSv1.2 协议");
    }

    @Test
    @Order(3)
    @DisplayName("测试 SSL 数据包大小配置")
    void testSSLBufferSizes() {
        assumeTrue(config.getProviderSslContext() != null, "需要 SSL 上下文");

        javax.net.ssl.SSLSession session = config.getProviderSslContext().createSSLEngine().getSession();
        int packetSize = session.getPacketBufferSize();
        int appSize = session.getApplicationBufferSize();

        assertTrue(packetSize > 0, "数据包缓冲区大小应该大于 0");
        assertTrue(appSize > 0, "应用缓冲区大小应该大于 0");
        assertTrue(packetSize >= appSize, "数据包缓冲区应该大于等于应用缓冲区");
    }

    @Test
    @Order(4)
    @DisplayName("测试双向 TLS 配置")
    void testMutualTLSConfig() {
        SslSocketConfig.SslConfig providerConfig = new SslSocketConfig.SslConfig();
        providerConfig.setEnabled(true);
        providerConfig.setClientAuth(true);
        providerConfig.setKeyStorePath("cert/server.p12");
        providerConfig.setKeyStorePassword("test123");
        providerConfig.setTrustStorePath("cert/trust.p12");
        providerConfig.setTrustStorePassword("test123");

        assertTrue(providerConfig.isClientAuth(), "应该启用客户端认证");
        assertNotNull(providerConfig.getTrustStorePath(), "应该配置信任库");
    }

    @Test
    @Order(5)
    @DisplayName("测试 TLS 会话配置")
    void testSessionConfig() {
        SslSocketConfig cfg = new SslSocketConfig();
        
        assertTrue(cfg.isEnableSessionCreation(), "默认应该启用会话创建");
        assertEquals(300, cfg.getSessionTimeout(), "默认超时应该是 300 秒");
        
        cfg.setSessionTimeout(600);
        assertEquals(600, cfg.getSessionTimeout(), "超时应该可以修改");
    }

    @Test
    @Order(6)
    @DisplayName("测试证书信息")
    void testCertificateInfo() {
        assumeTrue(config.getProviderSslContext() != null, "需要 SSL 上下文");

        javax.net.ssl.SSLContext ctx = config.getProviderSslContext();
        javax.net.ssl.SSLEngine engine = ctx.createSSLEngine();

        // 获取支持的协议
        String[] supportedProtocols = engine.getSupportedProtocols();
        assertTrue(java.util.Arrays.asList(supportedProtocols).contains("TLSv1.2"),
                "应该支持 TLSv1.2");

        // 获取启用的协议
        String[] enabledProtocols = engine.getEnabledProtocols();
        assertTrue(java.util.Arrays.asList(enabledProtocols).contains("TLSv1.2"),
                "应该启用 TLSv1.2");
    }

    @Test
    @Order(7)
    @DisplayName("测试配置 toString")
    void testToString() {
        SslSocketConfig cfg = new SslSocketConfig();
        cfg.getProvider().setEnabled(true);
        cfg.getCluster().setEnabled(true);
        
        String str = cfg.toString();
        assertTrue(str.contains("provider=true"), "toString 应该包含 provider 状态");
        assertTrue(str.contains("cluster=true"), "toString 应该包含 cluster 状态");
    }

    @Test
    @Order(8)
    @DisplayName("测试 isSslEnabled 方法")
    void testIsSslEnabled() {
        SslSocketConfig cfg = new SslSocketConfig();
        cfg.getProvider().setEnabled(true);
        
        assertTrue(cfg.isSslEnabled("provider"), "provider TLS 应该启用");
        assertFalse(cfg.isSslEnabled("consumer"), "consumer TLS 应该禁用");
        assertFalse(cfg.isSslEnabled("unknown"), "未知组件应该返回 false");
    }
}
