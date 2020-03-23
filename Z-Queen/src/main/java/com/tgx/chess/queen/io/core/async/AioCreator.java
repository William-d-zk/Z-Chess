/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.tgx.chess.queen.io.core.async;

import com.tgx.chess.queen.config.ISocketConfig;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.ISessionCreator;

import java.time.Duration;

/**
 * @author William.d.zk
 */
public abstract class AioCreator<C extends IContext<C>>
        implements
        ISessionCreator<C>
{
    private final ISocketConfig _Config;

    protected AioCreator(ISocketConfig config)
    {
        _Config = config;
    }

    @Override
    public int getSnfInByte()
    {
        return _Config.getSnfInByte();
    }

    @Override
    public int getRcvInByte()
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
}
