/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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
package com.isahl.chess.queen.io.core.async;

import com.isahl.chess.queen.config.ISocketConfig;
import com.isahl.chess.queen.io.core.inf.ISessionCreator;
import com.isahl.chess.queen.io.core.inf.ISslOption;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.NetworkChannel;
import java.time.Duration;

/**
 * @author William.d.zk
 */
public abstract class AioCreator
        implements
        ISessionCreator<AsynchronousSocketChannel>,
        ISslOption
{
    private final ISocketConfig _Config;

    protected AioCreator(ISocketConfig config)
    {
        _Config = config;
        config.init();
    }

    @Override
    public int getSnfByte()
    {
        return _Config.getSnfInByte();
    }

    @Override
    public int getRcvByte()
    {
        return _Config.getRcvInByte();
    }

    @Override
    public int getSendQueueMax()
    {
        return _Config.getSendQueueMax();
    }

    @Override
    public int getReadTimeOutInSecond()
    {
        return (int) _Config.getReadTimeoutInMinute()
                            .getSeconds();
    }

    @Override
    public int getWriteTimeOutInSecond()
    {
        return (int) _Config.getWriteTimeoutInSecond()
                            .getSeconds();
    }

    @Override
    public boolean isKeepAlive()
    {
        return _Config.isKeepAlive();
    }

    @Override
    public boolean isTcpNoDelay()
    {
        return _Config.isTcpNoDelay();
    }

    @Override
    public Duration getSoLingerInSecond()
    {
        return _Config.getSoLingerInSecond();
    }

    @Override
    public Duration getConnectTimeout()
    {
        return _Config.getConnectTimeoutInSecond();
    }

    @Override
    public KeyManager[] getKeyManagers()
    {
        return _Config.getKeyManagers();
    }

    @Override
    public TrustManager[] getTrustManagers()
    {
        return _Config.getTrustManagers();
    }

    @Override
    public boolean isSslClientAuth()
    {
        return _Config.isClientAuth();
    }

    @Override
    public int getSslPacketSize()
    {
        return _Config.getSslPacketBufferSize();
    }

    @Override
    public int getSslAppSize()
    {
        return _Config.getSslAppBufferSize();
    }

    @Override
    public void configChannel(NetworkChannel channel)
    {
        if (channel != null) {
            try {
                channel.setOption(StandardSocketOptions.TCP_NODELAY, isTcpNoDelay());
                channel.setOption(StandardSocketOptions.SO_RCVBUF, getRcvByte());
                channel.setOption(StandardSocketOptions.SO_SNDBUF, getSnfByte());
                channel.setOption(StandardSocketOptions.SO_KEEPALIVE, isKeepAlive());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
