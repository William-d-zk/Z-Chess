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

package com.isahl.chess.queen.io.core.net.udp;

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UDP 客户端基础实现
 * 
 * @author william.d.zk
 */
public class BaseUdpClient implements UdpChannel
{
    protected final Logger _Logger = Logger.getLogger("io.queen.udp." + getClass().getSimpleName());
    
    private final DatagramChannel _Channel;
    private final InetSocketAddress _RemoteAddress;
    private final BlockingQueue<UdpPacket> _ReceiveQueue;
    private final AtomicBoolean _IsOpen;
    private final int _BufferSize;
    
    public BaseUdpClient(String host, int port) throws IOException
    {
        this(host, port, 1024);
    }
    
    public BaseUdpClient(String host, int port, int bufferSize) throws IOException
    {
        _BufferSize = bufferSize;
        _Channel = DatagramChannel.open();
        _Channel.configureBlocking(false);
        _RemoteAddress = new InetSocketAddress(host, port);
        
        _ReceiveQueue = new LinkedBlockingQueue<>();
        _IsOpen = new AtomicBoolean(true);
        
        _Logger.info("UDP Client connected to %s:%d", host, port);
    }
    
    @Override
    public InetSocketAddress localAddress()
    {
        return (InetSocketAddress) _Channel.socket().getLocalSocketAddress();
    }
    
    @Override
    public int send(byte[] data, InetSocketAddress remote)
    {
        if (!_IsOpen.get()) {
            _Logger.warning("Cannot send: channel is closed");
            return 0;
        }
        
        try {
            InetSocketAddress target = remote != null ? remote : _RemoteAddress;
            ByteBuffer buffer = ByteBuffer.wrap(data);
            return _Channel.send(buffer, target);
        } catch (IOException e) {
            _Logger.error("Send failed: %s", e.getMessage());
            return 0;
        }
    }
    
    @Override
    public UdpPacket receive()
    {
        return _ReceiveQueue.poll();
    }
    
    /**
     * 启动接收线程
     */
    public void startReceiveLoop()
    {
        Thread receiveThread = new Thread(this::receiveLoop, "UdpClient-Receive");
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
    
    private void receiveLoop()
    {
        ByteBuffer buffer = ByteBuffer.allocate(_BufferSize);
        
        while (_IsOpen.get()) {
            try {
                buffer.clear();
                InetSocketAddress remote = (InetSocketAddress) _Channel.receive(buffer);
                
                if (remote != null) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    
                    ByteBuf packetBuffer = ByteBuf.allocate(data.length);
                    packetBuffer.put(data);
                    
                    UdpPacket packet = new UdpPacket(packetBuffer, remote);
                    _ReceiveQueue.offer(packet);
                    
                    _Logger.trace("Received %d bytes from %s", data.length, remote);
                }
                
                Thread.sleep(1);
            } catch (IOException e) {
                if (_IsOpen.get()) {
                    _Logger.error("Receive error: %s", e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * 发送到默认远程地址
     */
    public int send(byte[] data)
    {
        return send(data, _RemoteAddress);
    }
    
    @Override
    public void joinGroup(InetSocketAddress group, String networkInterface)
    {
        _Logger.warning("UDP Client does not support multicast by default");
    }
    
    @Override
    public void leaveGroup(InetSocketAddress group)
    {
        _Logger.warning("UDP Client does not support multicast by default");
    }
    
    @Override
    public boolean isMulticastSupported()
    {
        return false;
    }
    
    @Override
    public SocketAddress getSocketAddress()
    {
        return _Channel.socket().getLocalSocketAddress();
    }
    
    @Override
    public void close() throws IOException
    {
        if (_IsOpen.compareAndSet(true, false)) {
            _Channel.close();
            _Logger.info("UDP Client closed");
        }
    }
    
    @Override
    public boolean isValid()
    {
        return _IsOpen.get() && _Channel.isOpen();
    }
}
