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

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * UDP 数据包
 * 
 * @author william.d.zk
 */
public class UdpPacket
{
    private final ByteBuf _Buffer;
    private final InetSocketAddress _RemoteAddress;
    
    public UdpPacket(ByteBuf buffer, InetSocketAddress remoteAddress)
    {
        _Buffer = Objects.requireNonNull(buffer);
        _RemoteAddress = remoteAddress;
    }
    
    public ByteBuf getBuffer()
    {
        return _Buffer;
    }
    
    public InetSocketAddress getRemoteAddress()
    {
        return _RemoteAddress;
    }
    
    public int length()
    {
        return _Buffer.readableBytes();
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UdpPacket udpPacket = (UdpPacket) o;
        return Objects.equals(_Buffer, udpPacket._Buffer) &&
               Objects.equals(_RemoteAddress, udpPacket._RemoteAddress);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(_Buffer, _RemoteAddress);
    }
    
    @Override
    public String toString()
    {
        return "UdpPacket{" +
               "bytes=" + _Buffer.readableBytes() +
               ", remote=" + _RemoteAddress +
               '}';
    }
}
