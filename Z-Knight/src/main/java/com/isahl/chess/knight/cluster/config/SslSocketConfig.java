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

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.config.ISocketConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.unit.DataSize;

import jakarta.annotation.PostConstruct;
import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * TLS/SSL 配置类
 * 
 * 为 zchat 协议提供完整的 TLS 接入能力支持
 * 
 * 配置前缀: z.chess.pawn.io.{consumer,provider,cluster,internal}.ssl
 * 
 * @author william.d.zk
 */
@Configuration
@ConfigurationProperties(prefix = "z.chess.pawn.io")
@PropertySource("classpath:pawn.io.ssl.properties")
public class SslSocketConfig implements ISocketConfig {

    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    // ==================== 服务端配置 ====================
    private SslConfig provider = new SslConfig();
    
    // ==================== 客户端配置 ====================
    private SslConfig consumer = new SslConfig();
    
    // ==================== 集群配置 ====================
    private SslConfig cluster = new SslConfig();
    
    // ==================== 内部服务配置 ====================
    private SslConfig internal = new SslConfig();
    
    // ==================== 全局 TLS 配置 ====================
    private DataSize sslPacketBufferSize = DataSize.ofKilobytes(16);
    private DataSize sslAppBufferSize = DataSize.ofKilobytes(16);
    private boolean enableSessionCreation = true;
    private int sessionTimeout = 300;

    /**
     * TLS 配置子类
     */
    public static class SslConfig {
        private boolean enabled = false;
        private String keyStorePath;
        private String keyStorePassword;
        private String trustStorePath;
        private String trustStorePassword;
        private boolean clientAuth = false;
        private String protocol = "TLSv1.2";
        private List<String> ciphers;
        private boolean verifyHostname = true;
        private String keyStoreType = "PKCS12";
        
        // 缓存的 SSL 上下文
        private volatile SSLContext sslContext;
        public volatile KeyManager[] keyManagers;  // public for test access
        public volatile TrustManager[] trustManagers;  // public for test access

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getKeyStorePath() { return keyStorePath; }
        public void setKeyStorePath(String keyStorePath) { this.keyStorePath = keyStorePath; }
        
        public String getKeyStorePassword() { return keyStorePassword; }
        public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }
        
        public String getTrustStorePath() { return trustStorePath; }
        public void setTrustStorePath(String trustStorePath) { this.trustStorePath = trustStorePath; }
        
        public String getTrustStorePassword() { return trustStorePassword; }
        public void setTrustStorePassword(String trustStorePassword) { this.trustStorePassword = trustStorePassword; }
        
        public boolean isClientAuth() { return clientAuth; }
        public void setClientAuth(boolean clientAuth) { this.clientAuth = clientAuth; }
        
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        
        public List<String> getCiphers() { return ciphers; }
        public void setCiphers(List<String> ciphers) { this.ciphers = ciphers; }
        
        public boolean isVerifyHostname() { return verifyHostname; }
        public void setVerifyHostname(boolean verifyHostname) { this.verifyHostname = verifyHostname; }
        
        public String getKeyStoreType() { return keyStoreType; }
        public void setKeyStoreType(String keyStoreType) { this.keyStoreType = keyStoreType; }
    }

    @PostConstruct
    public void init() {
        _Logger.info("Initializing TLS configuration...");
        
        // 初始化各组件的 SSL 上下文
        if (provider.isEnabled()) {
            initSslContext(provider, "provider");
        }
        if (consumer.isEnabled()) {
            initSslContext(consumer, "consumer");
        }
        if (cluster.isEnabled()) {
            initSslContext(cluster, "cluster");
        }
        if (internal.isEnabled()) {
            initSslContext(internal, "internal");
        }
    }

    /**
     * 初始化 SSL 上下文
     */
    private void initSslContext(SslConfig config, String name) {
        try {
            _Logger.info("Initializing SSL context for %s", name);
            
            // 加载密钥管理器
            if (config.getKeyStorePath() != null && config.getKeyStorePassword() != null) {
                config.keyManagers = loadKeyManagers(config);
                _Logger.debug("Loaded key managers for %s from %s", name, config.getKeyStorePath());
            }
            
            // 加载信任管理器
            if (config.getTrustStorePath() != null && config.getTrustStorePassword() != null) {
                config.trustManagers = loadTrustManagers(config);
                _Logger.debug("Loaded trust managers for %s from %s", name, config.getTrustStorePath());
            }
            
            // 创建 SSL 上下文
            config.sslContext = SSLContext.getInstance(config.getProtocol());
            config.sslContext.init(config.keyManagers, config.trustManagers, null);
            
            _Logger.info("SSL context initialized for %s using protocol %s", name, config.getProtocol());
            
        } catch (Exception e) {
            _Logger.warning("Failed to initialize SSL context for %s: %s", name, e.getMessage());
            throw new ZException(e, "SSL initialization failed for " + name);
        }
    }

    /**
     * 加载密钥管理器
     */
    private KeyManager[] loadKeyManagers(SslConfig config) 
            throws KeyStoreException, NoSuchAlgorithmException, 
                   CertificateException, IOException, UnrecoverableKeyException {
        
        KeyStore keyStore = KeyStore.getInstance(config.getKeyStoreType());
        
        try (InputStream is = loadKeyStoreStream(config.getKeyStorePath())) {
            keyStore.load(is, config.getKeyStorePassword().toCharArray());
        }
        
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore, config.getKeyStorePassword().toCharArray());
        
        return factory.getKeyManagers();
    }

    /**
     * 加载信任管理器
     */
    private TrustManager[] loadTrustManagers(SslConfig config) 
            throws KeyStoreException, NoSuchAlgorithmException, 
                   CertificateException, IOException {
        
        KeyStore trustStore = KeyStore.getInstance(config.getKeyStoreType());
        
        try (InputStream is = loadKeyStoreStream(config.getTrustStorePath())) {
            trustStore.load(is, config.getTrustStorePassword().toCharArray());
        }
        
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trustStore);
        
        return factory.getTrustManagers();
    }

    /**
     * 加载密钥库文件流
     */
    private InputStream loadKeyStoreStream(String path) throws IOException {
        // 首先尝试从文件系统加载
        Path filePath = Paths.get(path);
        if (Files.exists(filePath)) {
            return Files.newInputStream(filePath);
        }
        
        // 然后尝试从类路径加载
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is != null) {
            return is;
        }
        
        throw new IOException("KeyStore not found: " + path);
    }

    // ==================== ISocketConfig 实现 ====================

    @Override
    public boolean isKeepAlive() { return false; }

    @Override
    public Duration getConnectTimeoutInSecond() { return Duration.ofSeconds(5); }

    @Override
    public Duration getWriteTimeoutInSecond() { return Duration.ofSeconds(15); }

    @Override
    public Duration getReadTimeoutInMinute() { return Duration.ofMinutes(5); }

    @Override
    public Duration getSoLingerInSecond() { return Duration.ofSeconds(10); }

    @Override
    public int getRcvInByte() { return (int) sslPacketBufferSize.toBytes(); }

    @Override
    public int getSnfInByte() { return (int) sslPacketBufferSize.toBytes(); }

    @Override
    public int getSendQueueMax() { return 8; }

    @Override
    public boolean isTcpNoDelay() { return true; }

    @Override
    public KeyManager[] getKeyManagers() { return consumer.keyManagers; }

    @Override
    public TrustManager[] getTrustManagers() { return consumer.trustManagers; }

    @Override
    public boolean isClientAuth() { return consumer.isClientAuth(); }

    @Override
    public int getSslPacketBufferSize() { return (int) sslPacketBufferSize.toBytes(); }
    
    public DataSize getSslPacketBufferSizeValue() { return sslPacketBufferSize; }
    public void setSslPacketBufferSize(DataSize sslPacketBufferSize) { this.sslPacketBufferSize = sslPacketBufferSize; }

    @Override
    public int getSslAppBufferSize() { return (int) sslAppBufferSize.toBytes(); }
    
    public DataSize getSslAppBufferSizeValue() { return sslAppBufferSize; }
    public void setSslAppBufferSize(DataSize sslAppBufferSize) { this.sslAppBufferSize = sslAppBufferSize; }

    // ==================== 获取各组件配置 ====================

    public SslConfig getProvider() { return provider; }
    public void setProvider(SslConfig provider) { this.provider = provider; }

    public SslConfig getConsumer() { return consumer; }
    public void setConsumer(SslConfig consumer) { this.consumer = consumer; }

    public SslConfig getCluster() { return cluster; }
    public void setCluster(SslConfig cluster) { this.cluster = cluster; }

    public SslConfig getInternal() { return internal; }
    public void setInternal(SslConfig internal) { this.internal = internal; }

    public SSLContext getProviderSslContext() { return provider.sslContext; }
    public SSLContext getConsumerSslContext() { return consumer.sslContext; }
    public SSLContext getClusterSslContext() { return cluster.sslContext; }
    public SSLContext getInternalSslContext() { return internal.sslContext; }

    public boolean isEnableSessionCreation() { return enableSessionCreation; }
    public void setEnableSessionCreation(boolean enableSessionCreation) { this.enableSessionCreation = enableSessionCreation; }

    public int getSessionTimeout() { return sessionTimeout; }
    public void setSessionTimeout(int sessionTimeout) { this.sessionTimeout = sessionTimeout; }

    /**
     * 检查指定组件是否启用 TLS
     */
    public boolean isSslEnabled(String component) {
        return switch (component.toLowerCase()) {
            case "provider" -> provider.isEnabled();
            case "consumer" -> consumer.isEnabled();
            case "cluster" -> cluster.isEnabled();
            case "internal" -> internal.isEnabled();
            default -> false;
        };
    }

    /**
     * 获取指定组件的 SSL 上下文
     */
    public SSLContext getSslContext(String component) {
        return switch (component.toLowerCase()) {
            case "provider" -> provider.sslContext;
            case "consumer" -> consumer.sslContext;
            case "cluster" -> cluster.sslContext;
            case "internal" -> internal.sslContext;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return String.format("SslSocketConfig{provider=%s, consumer=%s, cluster=%s, internal=%s}",
                provider.isEnabled(), consumer.isEnabled(), cluster.isEnabled(), internal.isEnabled());
    }
}
