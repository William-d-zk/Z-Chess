/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.tcp;

import com.isahl.chess.bishop.protocol.modbus.master.ModbusRequest;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusResponse;
import com.isahl.chess.bishop.protocol.modbus.tcp.ModbusTcpCodec.ModbusTcpMessage;
import com.isahl.chess.king.base.content.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Modbus TLS 客户端 (完整异步实现)
 * 支持 TLS 1.2 和 TLS 1.3 加密传输
 * 
 * Phase 3: TLS 加密数据传输
 */
public class ModbusTlsAsyncClient {
    
    private static final Logger LOG = LoggerFactory.getLogger(ModbusTlsAsyncClient.class);
    
    private final ModbusTlsConfig tlsConfig;
    private final int appBufferSize;
    private final int packetBufferSize;
    private AsynchronousSocketChannel channel;
    private SSLEngine sslEngine;
    private SSLContext sslContext;
    private final int timeoutMs;
    
    private ModbusTlsAsyncClient(ModbusTlsConfig tlsConfig, int timeoutMs) {
        this.tlsConfig = tlsConfig;
        this.timeoutMs = timeoutMs;
        this.appBufferSize = 32 * 1024;
        this.packetBufferSize = 32 * 1024;
    }
    
    /**
     * 创建 Modbus TLS 异步客户端
     */
    public static ModbusTlsAsyncClient create(ModbusTlsConfig tlsConfig, int timeoutMs) {
        return new ModbusTlsAsyncClient(tlsConfig, timeoutMs);
    }
    
    /**
     * 连接到 TLS 服务端并完成握手
     */
    public void connect(String host, int port) throws Exception {
        sslContext = tlsConfig.createSSLContext();
        
        channel = AsynchronousSocketChannel.open();
        channel.connect(new java.net.InetSocketAddress(host, port)).get(timeoutMs, TimeUnit.MILLISECONDS);
        
        LOG.info("Connected to Modbus TLS server {}:{}", host, port);
        
        // 创建 SSLEngine
        sslEngine = sslContext.createSSLEngine(host, port);
        sslEngine.setUseClientMode(true);
        
        if (tlsConfig.getEnabledCipherSuites() != null) {
            sslEngine.setEnabledCipherSuites(tlsConfig.getEnabledCipherSuites());
        }
        if (tlsConfig.getEnabledProtocols() != null) {
            sslEngine.setEnabledProtocols(tlsConfig.getEnabledProtocols());
        }
        
        // 执行 TLS 握手
        doHandshake();
        
        SSLSession session = sslEngine.getSession();
        LOG.info("TLS Session established: {} - {}", session.getProtocol(), session.getCipherSuite());
    }
    
    private void doHandshake() throws Exception {
        ByteBuffer appOut = ByteBuffer.allocate(appBufferSize);
        ByteBuffer appIn = ByteBuffer.allocate(appBufferSize);
        ByteBuffer pktOut = ByteBuffer.allocate(packetBufferSize);
        ByteBuffer pktIn = ByteBuffer.allocate(packetBufferSize);
        
        sslEngine.beginHandshake();
        SSLEngineResult.HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
        
        while (handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING &&
               handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED) {
            
            if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                // 读取服务器握手数据
                ByteBuffer readBuffer = ByteBuffer.allocate(packetBufferSize);
                int bytesRead = channel.read(readBuffer).get(timeoutMs, TimeUnit.MILLISECONDS);
                
                if (bytesRead < 0) {
                    throw new IOException("Connection closed during handshake");
                }
                
                readBuffer.flip();
                pktIn.put(readBuffer);
                pktIn.flip();
                
                appIn.clear();
                SSLEngineResult result = sslEngine.unwrap(pktIn, appIn);
                handshakeStatus = result.getHandshakeStatus();
                
                if (result.getStatus() == SSLEngineResult.Status.OK) {
                    runDelegatedTasks();
                }
                
            } else if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                // 发送握手数据
                appOut.clear();
                pktOut.clear();
                
                SSLEngineResult result = sslEngine.wrap(appOut, pktOut);
                pktOut.flip();
                
                byte[] encryptedData = new byte[pktOut.remaining()];
                pktOut.get(encryptedData);
                
                channel.write(java.nio.ByteBuffer.wrap(encryptedData)).get(timeoutMs, TimeUnit.MILLISECONDS);
                handshakeStatus = result.getHandshakeStatus();
                
            } else if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                runDelegatedTasks();
                handshakeStatus = sslEngine.getHandshakeStatus();
            }
        }
        
        LOG.debug("TLS handshake completed");
    }
    
    /**
     * 发送 Modbus 请求并接收响应 (TLS 加密)
     */
    public ModbusResponse sendRequest(ModbusRequest request) throws IOException, InterruptedException, 
            ExecutionException, TimeoutException {
        
        // 编码请求
        ByteBuf encoded = ModbusTcpCodec.encode(request, request.getTransactionId());
        byte[] requestData = new byte[encoded.readableBytes()];
        encoded.get(requestData);
        
        // 加密数据
        ByteBuffer appOut = ByteBuffer.wrap(requestData);
        ByteBuffer pktOut = ByteBuffer.allocate(packetBufferSize);
        
        SSLEngineResult wrapResult = sslEngine.wrap(appOut, pktOut);
        
        if (wrapResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new IOException("Failed to encrypt request: " + wrapResult.getStatus());
        }
        
        pktOut.flip();
        byte[] encryptedRequest = new byte[pktOut.remaining()];
        pktOut.get(encryptedRequest);
        
        // 发送加密请求
        channel.write(java.nio.ByteBuffer.wrap(encryptedRequest)).get(timeoutMs, TimeUnit.MILLISECONDS);
        LOG.debug("Sent {} bytes encrypted request", encryptedRequest.length);
        
        // 接收加密响应
        ByteBuffer pktIn = ByteBuffer.allocate(packetBufferSize);
        ByteBuffer appIn = ByteBuffer.allocate(appBufferSize);
        
        long startTime = System.currentTimeMillis();
        int totalBytesRead = 0;
        
        while ((System.currentTimeMillis() - startTime) < timeoutMs) {
            ByteBuffer readBuffer = ByteBuffer.allocate(packetBufferSize);
            Integer bytesRead = channel.read(readBuffer).get(timeoutMs, TimeUnit.MILLISECONDS);
            
            if (bytesRead < 0) {
                throw new IOException("Connection closed while waiting for response");
            }
            
            if (bytesRead > 0) {
                readBuffer.flip();
                pktIn.put(readBuffer);
                totalBytesRead += bytesRead;
                
                // 尝试解密
                pktIn.flip();
                appIn.clear();
                
                SSLEngineResult unwrapResult = sslEngine.unwrap(pktIn, appIn);
                
                if (unwrapResult.getStatus() == SSLEngineResult.Status.OK && appIn.position() > 0) {
                    appIn.flip();
                    byte[] responseData = new byte[appIn.remaining()];
                    appIn.get(responseData);
                    
                    // 解码响应
                    ByteBuf responseBuffer = ByteBuf.wrap(responseData);
                    ModbusTcpMessage response = ModbusTcpCodec.decode(responseBuffer);
                    
                    if (response == null) {
                        throw new IOException("Failed to decode Modbus response");
                    }
                    
                    LOG.debug("Received {} bytes encrypted response", totalBytesRead);
                    return new ModbusResponse(response.getUnitId(), response.getFunction(), response.getData());
                }
                
                pktIn.compact();
            }
        }
        
        throw new TimeoutException("Modbus TLS request timeout after " + timeoutMs + "ms");
    }
    
    private void runDelegatedTasks() {
        Runnable delegatedTask;
        while ((delegatedTask = sslEngine.getDelegatedTask()) != null) {
            delegatedTask.run();
        }
    }
    
    /**
     * 关闭连接
     */
    public void close() throws IOException {
        try {
            if (sslEngine != null) {
                // 发送 close_notify
                ByteBuffer appOut = ByteBuffer.allocate(0);
                ByteBuffer pktOut = ByteBuffer.allocate(packetBufferSize);
                sslEngine.wrap(appOut, pktOut);
                pktOut.flip();
                
                if (pktOut.hasRemaining() && channel != null && channel.isOpen()) {
                    byte[] closeData = new byte[pktOut.remaining()];
                    pktOut.get(closeData);
                    channel.write(java.nio.ByteBuffer.wrap(closeData));
                }
            }
        } catch (Exception e) {
            LOG.warn("Error during TLS close", e);
        } finally {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            LOG.info("TLS connection closed");
        }
    }
    
    public boolean isConnected() {
        return channel != null && channel.isOpen() && sslEngine != null;
    }
    
    public ModbusTlsConfig getTlsConfig() {
        return tlsConfig;
    }
    
    public SSLSession getSSLSession() {
        return sslEngine != null ? sslEngine.getSession() : null;
    }
}
