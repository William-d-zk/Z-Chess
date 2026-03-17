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
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UDP 服务器基础实现
 * 基于 DatagramChannel 的非阻塞 UDP 服务器
 * 
 * @author william.d.zk
 */
public class BaseUdpServer implements UdpChannel
{
    protected final Logger _Logger = Logger.getLogger("io.queen.udp." + getClass().getSimpleName());
    
    private final DatagramChannel _Channel;
    private final InetSocketAddress _LocalAddress;
    private final BlockingQueue<UdpPacket> _ReceiveQueue;
    private final AtomicBoolean _IsOpen;
    private final int _BufferSize;
    
    public BaseUdpServer(String host, int port) throws IOException
    {
        this(host, port, 1024);
    }
    
    public BaseUdpServer(String host, int port, int bufferSize) throws IOException
    {
        _BufferSize = bufferSize;
        _Channel = DatagramChannel.open();
        _Channel.configureBlocking(false);
        _LocalAddress = new InetSocketAddress(host, port);
        _Channel.socket().setReuseAddress(true);
        _Channel.bind(_LocalAddress);
        
        _ReceiveQueue = new LinkedBlockingQueue<>();
        _IsOpen = new AtomicBoolean(true);
        
        _Logger.info("UDP Server started on %s:%d", host, port);
    }
    
    @Override
    public InetSocketAddress localAddress()
    {
        return _LocalAddress;
    }
    
    @Override
    public int send(byte[] data, InetSocketAddress remote)
    {
        if (!_IsOpen.get()) {
            _Logger.warning("Cannot send: channel is closed");
            return 0;
        }
        
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            return _Channel.send(buffer, remote);
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
     * 启动接收线程（非阻塞轮询）
     */
    public void startReceiveLoop()
    {
        Thread receiveThread = new Thread(this::receiveLoop, "UdpServer-Receive-" + _LocalAddress.getPort());
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
    
    @Override
    public void joinGroup(InetSocketAddress group, String networkInterface)
    {
        try {
            NetworkInterface ni = NetworkInterface.getByName(networkInterface);
            if (ni == null) {
                _Logger.error("Network interface not found: %s", networkInterface);
                return;
            }
            
            _Channel.join(group.getAddress(), ni);
            _Logger.info("Joined multicast group %s on %s", group, networkInterface);
        } catch (IOException e) {
            _Logger.error("Failed to join multicast group: %s", e.getMessage());
        }
    }
    
    @Override
    public void leaveGroup(InetSocketAddress group)
    {
        _Logger.warning("Multicast leaveGroup not implemented for UDP server");
    }
    
    @Override
    public boolean isMulticastSupported()
    {
        return true;
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
            _Logger.info("UDP Server closed: %s", _LocalAddress);
        }
    }
    
    @Override
    public boolean isValid()
    {
        return _IsOpen.get() && _Channel.isOpen();
    }
}
