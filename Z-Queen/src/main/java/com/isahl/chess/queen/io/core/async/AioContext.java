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
    private final ByteBuffer _WrBuf;

    /*
     * 用于缓存 IPoS 分块带入的 RecvBuffer 内容 由于 AioWorker 中 channel 的 read_buffer - protocol_buffer - 都以
     * SocketOption 设定为准，所以不存在 IPoS 带入一个包含多个分页的协议
     * 内容的情况
     */
    private final ByteBuffer _RvBuf;

    private long mClientStartTime;
    private long mServerArrivedTime;
    private long mServerResponseTime;
    private long mClientArrivedTime;

    protected AioContext(ISessionOption option)
    {
        _RvBuf = allocateRcv(option);
        _WrBuf = allocateSnf(option);
    }

    @Override
    public void reset()
    {
        _RvBuf.clear();
        _WrBuf.clear();
    }

    @Override
    public ByteBuffer getWrBuffer()
    {
        return _WrBuf;
    }

    @Override
    public ByteBuffer getRvBuffer()
    {
        return _RvBuf;
    }

    @Override
    public int getSendMaxSize()
    {
        return _WrBuf.capacity();
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

    protected ByteBuffer allocateRcv(ISessionOption option)
    {
        return ByteBuffer.allocate(option.getRcvByte());
    }

    protected ByteBuffer allocateSnf(ISessionOption option)
    {
        return ByteBuffer.allocate(option.getSnfByte());
    }
}
