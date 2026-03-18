/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.tcp;

import com.isahl.chess.bishop.protocol.modbus.master.ModbusMaster;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusRequest;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Modbus TLS 客户端
 * 支持 TLS 1.2 和 TLS 1.3 加密传输
 */
public class ModbusTlsClient {
    
    private static final Logger LOG = LoggerFactory.getLogger(ModbusTlsClient.class);
    
    private final ModbusTlsConfig tlsConfig;
    private final ModbusMaster delegate;
    private SSLSocket sslSocket;
    private SSLContext sslContext;
    
    private ModbusTlsClient(ModbusTlsConfig tlsConfig, ModbusMaster delegate) {
        this.tlsConfig = tlsConfig;
        this.delegate = delegate;
    }
    
    /**
     * 创建 Modbus TLS 客户端
     */
    public static ModbusTlsClient create(ModbusTlsConfig tlsConfig, ModbusMaster delegate) {
        return new ModbusTlsClient(tlsConfig, delegate);
    }
    
    /**
     * 连接到 TLS 服务端
     */
    public void connect(String host, int port, int timeoutMs) throws Exception {
        // 初始化 SSLContext
        sslContext = tlsConfig.createSSLContext();
        
        // 创建 SSLSocket
        SSLSocketFactory ssf = sslContext.getSocketFactory();
        sslSocket = (SSLSocket) ssf.createSocket(host, port);
        
        // 配置加密套件和协议
        if (tlsConfig.getEnabledCipherSuites() != null) {
            sslSocket.setEnabledCipherSuites(tlsConfig.getEnabledCipherSuites());
        }
        if (tlsConfig.getEnabledProtocols() != null) {
            sslSocket.setEnabledProtocols(tlsConfig.getEnabledProtocols());
        }
        
        // 启动 TLS 握手
        sslSocket.setSoTimeout(timeoutMs);
        sslSocket.startHandshake();
        
        LOG.info("Connected to Modbus TLS server {}:{}", host, port);
    }
    
    /**
     * 发送 Modbus 请求并接收响应 (TLS 加密)
     */
    public ModbusResponse sendRequest(ModbusRequest request, int timeoutMs) throws IOException {
        if (sslSocket == null || sslSocket.isClosed()) {
            throw new IOException("Not connected");
        }
        
        // Phase 3: 实现完整的 TLS 加密数据传输
        // 当前简化实现，抛出异常提示
        throw new UnsupportedOperationException("TLS encrypted transmission requires Phase 3 implementation");
    }
    
    /**
     * 关闭连接
     */
    public void close() throws IOException {
        if (sslSocket != null && !sslSocket.isClosed()) {
            sslSocket.close();
            LOG.info("TLS connection closed");
        }
    }
    
    public boolean isConnected() {
        return sslSocket != null && sslSocket.isConnected() && !sslSocket.isClosed();
    }
    
    public ModbusTlsConfig getTlsConfig() {
        return tlsConfig;
    }
    
    public ModbusMaster getDelegate() {
        return delegate;
    }
}
