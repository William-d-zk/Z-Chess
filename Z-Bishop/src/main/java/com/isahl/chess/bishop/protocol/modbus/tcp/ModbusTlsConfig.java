/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.tcp;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Modbus TLS 配置
 * 支持 TLS 1.2 和 TLS 1.3
 */
public class ModbusTlsConfig {
    
    private final String protocol;
    private final String keyStorePath;
    private final char[] keyStorePassword;
    private final String trustStorePath;
    private final char[] trustStorePassword;
    private final String[] enabledCipherSuites;
    private final String[] enabledProtocols;
    private final boolean clientAuthRequired;
    
    private ModbusTlsConfig(Builder builder) {
        this.protocol = builder.protocol;
        this.keyStorePath = builder.keyStorePath;
        this.keyStorePassword = builder.keyStorePassword;
        this.trustStorePath = builder.trustStorePath;
        this.trustStorePassword = builder.trustStorePassword;
        this.enabledCipherSuites = builder.enabledCipherSuites;
        this.enabledProtocols = builder.enabledProtocols;
        this.clientAuthRequired = builder.clientAuthRequired;
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public String getKeyStorePath() {
        return keyStorePath;
    }
    
    public char[] getKeyStorePassword() {
        return keyStorePassword;
    }
    
    public String getTrustStorePath() {
        return trustStorePath;
    }
    
    public char[] getTrustStorePassword() {
        return trustStorePassword;
    }
    
    public String[] getEnabledCipherSuites() {
        return enabledCipherSuites;
    }
    
    public String[] getEnabledProtocols() {
        return enabledProtocols;
    }
    
    public boolean isClientAuthRequired() {
        return clientAuthRequired;
    }
    
    /**
     * 创建 SSLContext
     */
    public SSLContext createSSLContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance(protocol);
        
        javax.net.ssl.KeyManager[] keyManagers = null;
        if (keyStorePath != null) {
            KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keyStorePassword);
            keyManagers = kmf.getKeyManagers();
        }
        
        javax.net.ssl.TrustManager[] trustManagers = null;
        if (trustStorePath != null) {
            KeyStore trustStore = loadKeyStore(trustStorePath, trustStorePassword);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            trustManagers = tmf.getTrustManagers();
        }
        
        sslContext.init(keyManagers, trustManagers, null);
        return sslContext;
    }
    
    private KeyStore loadKeyStore(String path, char[] password) 
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(path)) {
            keyStore.load(fis, password);
        }
        return keyStore;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String protocol = "TLSv1.3";
        private String keyStorePath;
        private char[] keyStorePassword;
        private String trustStorePath;
        private char[] trustStorePassword;
        private String[] enabledCipherSuites;
        private String[] enabledProtocols = new String[]{"TLSv1.3", "TLSv1.2"};
        private boolean clientAuthRequired = false;
        
        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }
        
        public Builder keyStore(String path, char[] password) {
            this.keyStorePath = path;
            this.keyStorePassword = password;
            return this;
        }
        
        public Builder trustStore(String path, char[] password) {
            this.trustStorePath = path;
            this.trustStorePassword = password;
            return this;
        }
        
        public Builder enabledCipherSuites(String... cipherSuites) {
            this.enabledCipherSuites = cipherSuites;
            return this;
        }
        
        public Builder enabledProtocols(String... protocols) {
            this.enabledProtocols = protocols;
            return this;
        }
        
        public Builder clientAuthRequired(boolean required) {
            this.clientAuthRequired = required;
            return this;
        }
        
        public ModbusTlsConfig build() {
            return new ModbusTlsConfig(this);
        }
    }
}
