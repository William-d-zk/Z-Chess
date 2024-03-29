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

package com.isahl.chess.queen.config;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.time.Duration;

/**
 * @author william.d.zk
 * @date 2020/2/1
 */
public interface ISocketConfig
{

    default boolean isKeepAlive()
    {
        return true;
    }

    default Duration getWriteTimeoutInSecond()
    {
        return Duration.ofSeconds(30);
    }

    default Duration getReadTimeoutInMinute()
    {
        return Duration.ofMinutes(15);
    }

    default int getSendQueueMax()
    {
        return 64;
    }

    default int getRcvInByte()
    {
        return 65536;
    }

    default int getSnfInByte()
    {
        return 131072;
    }

    default boolean isTcpNoDelay()
    {
        return true;
    }

    default Duration getSoLingerInSecond()
    {
        return Duration.ofSeconds(30);
    }

    default Duration getConnectTimeoutInSecond()
    {
        return Duration.ofSeconds(5);
    }

    default KeyManager[] getKeyManagers()
    {
        return null;
    }

    default TrustManager[] getTrustManagers()
    {
        return null;
    }

    default void init()
    {
    }

    int getSslPacketBufferSize();

    int getSslAppBufferSize();

    boolean isClientAuth();
}
