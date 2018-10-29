/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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

import java.util.concurrent.TimeUnit;

import com.tgx.chess.king.config.Config;
import com.tgx.chess.queen.config.QueenConfigKey;
import com.tgx.chess.queen.io.core.inf.ISessionCreator;

/**
 * @author William.d.zk
 */
public abstract class AioCreator
        implements
        ISessionCreator
{
    private final Config _Config;

    public AioCreator(Config config)
    {
        _Config = config.load(getConfigName());
    }

    @Override
    public int setSNF()
    {
        return _Config.getConfigValue(getConfigGroup(), QueenConfigKey.OWNER_SOCKET_OPTION, QueenConfigKey.KEY_OPTION_SNF);
    }

    @Override
    public int setRCV()
    {
        return _Config.getConfigValue(getConfigGroup(), QueenConfigKey.OWNER_SOCKET_OPTION, QueenConfigKey.KEY_OPTION_RCV);
    }

    @Override
    public int setQueueMax()
    {
        return _Config.getConfigValue(getConfigGroup(), QueenConfigKey.OWNER_SOCKET_SEND, QueenConfigKey.KEY_SEND_QUEUE_SIZE);
    }

    @Override
    public int setReadTimeOut()
    {
        int duration = _Config.getConfigValue(getConfigGroup(), QueenConfigKey.OWNER_SOCKET_IN, QueenConfigKey.KEY_IN_MINUTE);
        return (int) TimeUnit.MINUTES.toSeconds(duration);
    }

    @Override
    public int setWriteTimeOut()
    {
        return _Config.getConfigValue(getConfigGroup(), QueenConfigKey.OWNER_SOCKET_OUT, QueenConfigKey.KEY_OUT_SECOND);
    }

    @Override
    public boolean setKeepAlive()
    {
        return _Config.getConfigValue(getConfigGroup(), QueenConfigKey.OWNER_SOCKET_OPTION, QueenConfigKey.KEY_OPTION_KEEP_ALIVE);
    }
}
