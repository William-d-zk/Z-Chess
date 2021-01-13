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
package com.isahl.chess.queen.io.core.async;

import java.nio.ByteBuffer;

import com.isahl.chess.queen.io.core.inf.IContext;
import com.isahl.chess.queen.io.core.inf.ISessionOption;

/**
 * @author William.d.zk
 */
public abstract class AioContext
        implements
        IContext
{

    /**
     * 存在 一次性投递多个 IControl 的可能性
     * AioPacket 中的 ByteBuffer 仅用于序列化Control 对象
     * 创建时以SocketOption配置为基准进行设定，
     * 
     */
    private ByteBuffer mWrBuf;

    /**
     * 用于缓存 IPoS 分块带入的 RecvBuffer 内容
     */
    private ByteBuffer mRvBuf;

    private long mClientStartTime;
    private long mServerArrivedTime;
    private long mServerResponseTime;
    private long mClientArrivedTime;

    protected AioContext(ISessionOption option)
    {
        mRvBuf = ByteBuffer.allocate(option.getRcvInByte());
        mWrBuf = ByteBuffer.allocate(option.getSnfInByte());
    }

    @Override
    public void reset()
    {
        mRvBuf.clear();
        mWrBuf.clear();
    }

    @Override
    public ByteBuffer getWrBuffer()
    {
        return mWrBuf;
    }

    @Override
    public ByteBuffer getRvBuffer()
    {
        return mRvBuf;
    }

    @Override
    public int getSendMaxSize()
    {
        return mWrBuf.capacity();
    }

    @Override
    public void ntp(long clientStart, long serverArrived, long serverResponse, long clientArrived)
    {
        if (mClientStartTime != 0) mClientStartTime = clientStart;
        if (mServerArrivedTime != 0) mServerArrivedTime = serverArrived;
        if (mServerResponseTime != 0) mServerResponseTime = serverResponse;
        if (mClientArrivedTime != 0) mClientArrivedTime = clientArrived;
    }

    @Override
    public long getNetTransportDelay()
    {
        return (mClientArrivedTime - mClientStartTime - mServerResponseTime + mServerArrivedTime) >> 1;
    }

    @Override
    public long getDeltaTime()
    {
        return (mServerArrivedTime - mClientStartTime + mServerResponseTime - mClientArrivedTime) >> 1;
    }

    @Override
    public long getNtpArrivedTime()
    {
        return mServerArrivedTime;
    }

}
