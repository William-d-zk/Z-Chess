/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.rook.config;

import java.time.Duration;

import org.springframework.util.unit.DataSize;

import com.isahl.chess.queen.config.ISocketConfig;

public class SocketConfig
        implements
        ISocketConfig
{
    private boolean  keepAlive;
    private Duration connectTimeoutInSecond;
    private Duration writeTimeoutInSecond;
    private Duration readTimeoutInMinute;
    private Duration soLingerInSecond;
    private DataSize sendBufferSize;
    private DataSize recvBufferSize;
    private int      sendQueueMax;
    private boolean  tcpNoDelay;

    @Override
    public boolean isKeepAlive()
    {
        return keepAlive;
    }

    @Override
    public Duration getWriteTimeoutInSecond()
    {
        return writeTimeoutInSecond;
    }

    @Override
    public Duration getReadTimeoutInMinute()
    {
        return readTimeoutInMinute;
    }

    @Override
    public int getSendQueueMax()
    {
        return sendQueueMax;
    }

    @Override
    public int getRcvInByte()
    {
        return (int) recvBufferSize.toBytes();
    }

    @Override
    public int getSnfInByte()
    {
        return (int) sendBufferSize.toBytes();
    }

    @Override
    public boolean isTcpNoDelay()
    {
        return tcpNoDelay;
    }

    @Override
    public Duration getSoLingerInSecond()
    {
        return soLingerInSecond;
    }

    public void setKeepAlive(boolean keepAlive)
    {
        this.keepAlive = keepAlive;
    }

    public void setWriteTimeoutInSecond(Duration writeTimeoutInSecond)
    {
        this.writeTimeoutInSecond = writeTimeoutInSecond;
    }

    public void setReadTimeoutInMinute(Duration readTimeoutInMinute)
    {
        this.readTimeoutInMinute = readTimeoutInMinute;
    }

    public void setSoLingerInSecond(Duration soLingerInSecond)
    {
        this.soLingerInSecond = soLingerInSecond;
    }

    public void setSendBufferSize(DataSize sendBufferSize)
    {
        this.sendBufferSize = sendBufferSize;
    }

    public void setRecvBufferSize(DataSize recvBufferSize)
    {
        this.recvBufferSize = recvBufferSize;
    }

    public void setSendQueueMax(int sendQueueMax)
    {
        this.sendQueueMax = sendQueueMax;
    }

    public void setTcpNoDelay(boolean tcpNoDelay)
    {
        this.tcpNoDelay = tcpNoDelay;
    }

    @Override
    public Duration getConnectTimeoutInSecond()
    {
        return connectTimeoutInSecond;
    }

    public void setConnectTimeoutInSecond(Duration connectTimeoutInSecond)
    {
        this.connectTimeoutInSecond = connectTimeoutInSecond;
    }
}
