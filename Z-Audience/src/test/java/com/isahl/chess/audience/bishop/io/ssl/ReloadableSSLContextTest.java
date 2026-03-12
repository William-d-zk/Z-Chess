/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.io.ssl;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReloadableSSLContext 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReloadableSSLContextTest {

    @TempDir
    Path tempDir;

    private Path keyStorePath;
    private Path trustStorePath;
    private String keyStorePassword = "testpass";

    @BeforeEach
    void setUp() throws Exception {
        keyStorePath = tempDir.resolve("test-keystore.p12");
        trustStorePath = tempDir.resolve("test-truststore.p12");
        
        // 创建测试证书
        createTestKeyStore(keyStorePath, "server");
        createTestKeyStore(trustStorePath, "ca");
    }

    private void createTestKeyStore(Path path, String alias) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        
        // 生成自签名证书
        java.security.KeyPairGenerator keyGen = 
            java.security.KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        java.security.KeyPair keyPair = keyGen.generateKeyPair();
        
        // 创建证书
        X509Certificate cert = generateSelfSignedCert(keyPair, alias);
        
        // 存储到 KeyStore
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), 
                            keyStorePassword.toCharArray(), 
                            new Certificate[]{cert});
        
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            keyStore.store(fos, keyStorePassword.toCharArray());
        }
    }

    private X509Certificate generateSelfSignedCert(java.security.KeyPair keyPair, 
                                                   String cn) throws Exception {
        // 简化版证书生成（实际测试中使用）
        // 注意：这里使用 BouncyCastle 或类似库会更完整
        // 为测试目的，我们创建一个基本的证书结构
        
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        Date endDate = new Date(now + 365 * 24 * 60 * 60 * 1000L);
        
        // 使用 BouncyCastle 的 X509v3CertificateBuilder
        // 这里简化处理
        return null;  // 实际实现需要使用 BouncyCastle
    }

    @Test
    @Order(1)
    @DisplayName("测试创建 ReloadableSSLContext (TLS 1.2)")
    void testCreateContextTls12() throws Exception {
        ReloadableSSLContext context = new ReloadableSSLContext(
            "TLSv1.2",
            keyStorePath.toString(),
            keyStorePassword,
            trustStorePath.toString(),
            keyStorePassword,
            "PKCS12",
            false,
            5000
        );
        
        assertNotNull(context);
        assertNotNull(context.getSSLContext());
        assertFalse(context.isHotReloadEnabled());
        assertEquals(1, context.getVersion());
        
        context.stop();
    }

    @Test
    @Order(2)
    @DisplayName("测试创建 ReloadableSSLContext (TLS 1.3)")
    void testCreateContextTls13() throws Exception {
        Assumptions.assumeTrue(
            Runtime.version().feature() >= 11,
            "TLS 1.3 requires JDK 11+"
        );
        
        ReloadableSSLContext context = new ReloadableSSLContext(
            "TLSv1.3",
            keyStorePath.toString(),
            keyStorePassword,
            trustStorePath.toString(),
            keyStorePassword,
            "PKCS12",
            false,
            5000
        );
        
        assertNotNull(context);
        assertNotNull(context.getSSLContext());
        
        SSLContext sslContext = context.getSSLContext();
        SSLEngine engine = sslContext.createSSLEngine();
        
        // 检查是否支持 TLS 1.3
        boolean supportsTls13 = false;
        for (String protocol : engine.getSupportedProtocols()) {
            if ("TLSv1.3".equals(protocol)) {
                supportsTls13 = true;
                break;
            }
        }
        
        assertTrue(supportsTls13, "Should support TLS 1.3");
        context.stop();
    }

    @Test
    @Order(3)
    @DisplayName("测试创建 SSLEngine")
    void testCreateSSLEngine() throws Exception {
        ReloadableSSLContext context = new ReloadableSSLContext(
            "TLSv1.2",
            keyStorePath.toString(),
            keyStorePassword,
            trustStorePath.toString(),
            keyStorePassword
        );
        
        SSLEngine engine = context.createSSLEngine();
        assertNotNull(engine);
        
        SSLEngine engineWithHost = context.createSSLEngine("localhost", 8080);
        assertNotNull(engineWithHost);
        
        context.stop();
    }

    @Test
    @Order(4)
    @DisplayName("测试版本号递增")
    void testVersionIncrement() throws Exception {
        ReloadableSSLContext context = new ReloadableSSLContext(
            "TLSv1.2",
            keyStorePath.toString(),
            keyStorePassword,
            trustStorePath.toString(),
            keyStorePassword,
            "PKCS12",
            false,
            5000
        );
        
        long version1 = context.getVersion();
        assertEquals(1, version1);
        
        // 重新加载
        boolean success = context.reload();
        assertTrue(success);
        
        long version2 = context.getVersion();
        assertEquals(2, version2);
        assertTrue(version2 > version1);
        
        context.stop();
    }

    @Test
    @Order(5)
    @DisplayName("测试热更新启用状态")
    void testHotReloadEnabled() throws Exception {
        ReloadableSSLContext context = new ReloadableSSLContext(
            "TLSv1.2",
            keyStorePath.toString(),
            keyStorePassword,
            null,
            null,
            "PKCS12",
            true,
            100
        );
        
        assertTrue(context.isHotReloadEnabled());
        
        // 给监视器一点时间启动
        Thread.sleep(200);
        
        context.stop();
    }

    @Test
    @Order(6)
    @DisplayName("测试获取 SSLContext")
    void testGetSSLContext() throws Exception {
        ReloadableSSLContext context = new ReloadableSSLContext(
            "TLSv1.2",
            keyStorePath.toString(),
            keyStorePassword,
            trustStorePath.toString(),
            keyStorePassword
        );
        
        SSLContext sslContext = context.getSSLContext();
        assertNotNull(sslContext);
        assertEquals("TLSv1.2", sslContext.getProtocol());
        
        context.stop();
    }

    @Test
    @Order(7)
    @DisplayName("测试创建时间")
    void testCreateTime() throws Exception {
        long before = System.currentTimeMillis();
        
        ReloadableSSLContext context = new ReloadableSSLContext(
            "TLSv1.2",
            keyStorePath.toString(),
            keyStorePassword,
            trustStorePath.toString(),
            keyStorePassword
        );
        
        long after = System.currentTimeMillis();
        long createTime = context.getCreateTime();
        
        assertTrue(createTime >= before && createTime <= after,
                  "Create time should be between before and after");
        
        context.stop();
    }
}
