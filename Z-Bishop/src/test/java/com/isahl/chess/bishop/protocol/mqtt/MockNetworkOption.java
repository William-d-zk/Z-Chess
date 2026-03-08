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

package com.isahl.chess.bishop.protocol.mqtt;

import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;

import java.nio.channels.NetworkChannel;
import java.time.Duration;

/**
 * 测试用的 INetworkOption 简单实现
 */
public class MockNetworkOption implements INetworkOption {

    @Override
    public void configChannel(NetworkChannel channel) {
        // 测试用，不做任何操作
    }

    @Override
    public boolean isTcpNoDelay() {
        return true;
    }

    @Override
    public Duration getSoLingerInSecond() {
        return Duration.ZERO;
    }

    @Override
    public int getSnfByte() {
        return 65536;
    }

    @Override
    public int getRcvByte() {
        return 65536;
    }

    @Override
    public int getSendQueueMax() {
        return 64;
    }

    @Override
    public int getReadTimeOutInSecond() {
        return 30;
    }

    @Override
    public int getWriteTimeOutInSecond() {
        return 30;
    }

    @Override
    public boolean isKeepAlive() {
        return true;
    }

    @Override
    public Duration getConnectTimeout() {
        return Duration.ofSeconds(10);
    }
}
