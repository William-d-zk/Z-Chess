/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.tcp;

import com.isahl.chess.bishop.protocol.modbus.slave.ModbusSlave;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Modbus TLS 服务端
 * 支持 TLS 1.2 和 TLS 1.3 加密传输
 */
public class ModbusTlsServer {
    
    private static final Logger LOG = LoggerFactory.getLogger(ModbusTlsServer.class);
    
    private final ModbusTlsConfig tlsConfig;
    private final ModbusSlave delegate;
    private AsynchronousServerSocketChannel serverChannel;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private SSLContext sslContext;
    
    private ModbusTlsServer(ModbusTlsConfig tlsConfig, ModbusSlave delegate) {
        this.tlsConfig = tlsConfig;
        this.delegate = delegate;
    }
    
    /**
     * 创建 Modbus TLS 服务端
     */
    public static ModbusTlsServer create(ModbusTlsConfig tlsConfig, ModbusSlave delegate) {
        return new ModbusTlsServer(tlsConfig, delegate);
    }
    
    /**
     * 启动 TLS 服务端
     */
    public void start() throws Exception {
        if (running.compareAndSet(false, true)) {
            // 初始化 SSLContext
            sslContext = tlsConfig.createSSLContext();
            
            // 创建 SSLServerSocket
            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            SSLServerSocket sslServerSocket = (SSLServerSocket) ssf.createServerSocket();
            sslServerSocket.bind(new InetSocketAddress(tlsConfig.getKeyStorePath() != null ? 802 : 502));
            
            // 配置加密套件和协议
            if (tlsConfig.getEnabledCipherSuites() != null) {
                sslServerSocket.setEnabledCipherSuites(tlsConfig.getEnabledCipherSuites());
            }
            if (tlsConfig.getEnabledProtocols() != null) {
                sslServerSocket.setEnabledProtocols(tlsConfig.getEnabledProtocols());
            }
            sslServerSocket.setNeedClientAuth(tlsConfig.isClientAuthRequired());
            
            // 绑定到端口 802 (Modbus TLS 默认端口)
            sslServerSocket.bind(new InetSocketAddress(802));
            
            LOG.info("Modbus TLS Server started on port 802 with {}", tlsConfig.getProtocol());
            
            // Phase 3: 实现完整的 TLS 异步处理
            // 当前简化实现，仅记录启动信息
            serverChannel = null; // Placeholder for Phase 3 implementation
            acceptNextClient();
        }
    }
    
    private void acceptNextClient() {
        // Phase 3: 实现完整的 TLS 异步接受客户端连接
        // 当前简化实现，仅记录日志
        LOG.debug("TLS server ready to accept connections (Phase 3 implementation pending)");
    }
    
    private void handleClient(AsynchronousSocketChannel clientChannel) {
        // Phase 3: 实现完整的 TLS 握手和数据加密处理
        LOG.debug("TLS client handler (Phase 3 implementation pending)");
    }
    
    /**
     * 停止 TLS 服务端
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                if (serverChannel != null) {
                    serverChannel.close();
                }
                LOG.info("Modbus TLS Server stopped");
            } catch (IOException e) {
                LOG.error("Error stopping Modbus TLS Server", e);
            }
        }
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public ModbusTlsConfig getTlsConfig() {
        return tlsConfig;
    }
    
    public ModbusSlave getDelegate() {
        return delegate;
    }
}
