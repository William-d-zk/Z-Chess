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

package com.isahl.chess.knight.cluster.config;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import javax.net.ssl.SSLContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TLS 配置测试类
 */
class SslSocketConfigTest {

    @Test
    void testDefaultConfiguration() {
        SslSocketConfig config = new SslSocketConfig();
        
        // 验证默认值
        assertNotNull(config.getProvider());
        assertNotNull(config.getConsumer());
        assertNotNull(config.getCluster());
        assertNotNull(config.getInternal());
        
        // 默认禁用 TLS
        assertFalse(config.getProvider().isEnabled());
        assertFalse(config.getConsumer().isEnabled());
        assertFalse(config.getCluster().isEnabled());
        assertFalse(config.getInternal().isEnabled());
    }

    @Test
    void testSslBufferSizes() {
        SslSocketConfig config = new SslSocketConfig();
        
        // 验证默认缓冲区大小
        assertEquals(16 * 1024, config.getSslPacketBufferSize());
        assertEquals(16 * 1024, config.getSslAppBufferSize());
        
        // 验证设置新值
        config.setSslPacketBufferSize(DataSize.ofKilobytes(32));
        config.setSslAppBufferSize(DataSize.ofKilobytes(32));
        
        assertEquals(32 * 1024, config.getSslPacketBufferSize());
        assertEquals(32 * 1024, config.getSslAppBufferSize());
    }

    @Test
    void testProviderSslConfig() {
        SslSocketConfig.SslConfig provider = new SslSocketConfig.SslConfig();
        
        provider.setEnabled(true);
        provider.setKeyStorePath("cert/server.p12");
        provider.setKeyStorePassword("test123");
        provider.setTrustStorePath("cert/trust.p12");
        provider.setTrustStorePassword("test123");
        provider.setClientAuth(true);
        provider.setProtocol("TLSv1.2");
        
        assertTrue(provider.isEnabled());
        assertEquals("cert/server.p12", provider.getKeyStorePath());
        assertEquals("test123", provider.getKeyStorePassword());
        assertTrue(provider.isClientAuth());
        assertEquals("TLSv1.2", provider.getProtocol());
    }

    @Test
    void testClusterSslConfig() {
        SslSocketConfig.SslConfig cluster = new SslSocketConfig.SslConfig();
        
        cluster.setEnabled(true);
        cluster.setKeyStorePath("cert/cluster.p12");
        cluster.setKeyStorePassword("cluster123");
        cluster.setTrustStorePath("cert/cluster-trust.p12");
        cluster.setTrustStorePassword("cluster123");
        cluster.setClientAuth(true); // 集群必须启用双向认证
        
        assertTrue(cluster.isEnabled());
        assertTrue(cluster.isClientAuth());
    }

    @Test
    void testIsSslEnabled() {
        SslSocketConfig config = new SslSocketConfig();
        
        // 设置 provider 启用
        config.getProvider().setEnabled(true);
        
        assertTrue(config.isSslEnabled("provider"));
        assertFalse(config.isSslEnabled("consumer"));
        assertFalse(config.isSslEnabled("cluster"));
        assertFalse(config.isSslEnabled("unknown"));
    }

    @Test
    void testSessionConfiguration() {
        SslSocketConfig config = new SslSocketConfig();
        
        // 验证默认值
        assertTrue(config.isEnableSessionCreation());
        assertEquals(300, config.getSessionTimeout());
        
        // 修改配置
        config.setEnableSessionCreation(false);
        config.setSessionTimeout(600);
        
        assertFalse(config.isEnableSessionCreation());
        assertEquals(600, config.getSessionTimeout());
    }

    @Test
    void testCipherConfiguration() {
        SslSocketConfig.SslConfig provider = new SslSocketConfig.SslConfig();
        
        // 设置加密套件
        java.util.List<String> ciphers = java.util.Arrays.asList(
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
        );
        provider.setCiphers(ciphers);
        
        assertNotNull(provider.getCiphers());
        assertEquals(2, provider.getCiphers().size());
        assertTrue(provider.getCiphers().contains("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"));
    }

    @Test
    void testToString() {
        SslSocketConfig config = new SslSocketConfig();
        
        config.getProvider().setEnabled(true);
        config.getCluster().setEnabled(true);
        
        String str = config.toString();
        assertTrue(str.contains("provider=true"));
        assertTrue(str.contains("cluster=true"));
    }

    /**
     * 集成测试：需要实际证书文件
     * 运行前需执行 scripts/generate-ssl-certs.sh
     */
    @Test
    void testSslContextInitialization() {
        // 此测试需要实际证书文件，仅在证书存在时运行
        java.io.File certDir = new java.io.File("cert");
        if (!certDir.exists() || !new java.io.File("cert/server.p12").exists()) {
            System.out.println("Skipping SSL context test - certificates not found");
            return;
        }
        
        SslSocketConfig config = new SslSocketConfig();
        config.getProvider().setEnabled(true);
        config.getProvider().setKeyStorePath("cert/server.p12");
        config.getProvider().setKeyStorePassword("changeit");
        config.getProvider().setTrustStorePath("cert/trust.p12");
        config.getProvider().setTrustStorePassword("changeit");
        
        // 初始化（会在 @PostConstruct 中调用）
        config.init();
        
        // 验证 SSL 上下文已创建
        SSLContext context = config.getProviderSslContext();
        assertNotNull(context);
        assertEquals("TLSv1.2", context.getProtocol());
    }
}
