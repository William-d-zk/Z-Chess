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
package com.isahl.chess.queen.io.core.net.socket;

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.model.session.IContext;
import com.isahl.chess.queen.io.core.features.model.session.IOption;

/**
 * @author William.d.zk
 */
public abstract class AioContext<O extends IOption>
        implements IContext
{

    private final ByteBuf _WrBuffer;
    private final ByteBuf _RvBuffer;

    private long mClientStartTime;
    private long mServerArrivedTime;
    private long mServerResponseTime;
    private long mClientArrivedTime;

    protected AioContext(O option)
    {
        _RvBuffer = ByteBuf.allocate(option.getRcvByte());
        _WrBuffer = ByteBuf.allocate(option.getSnfByte());
    }

    @Override
    public void ntp(long clientStart, long serverArrived, long serverResponse, long clientArrived)
    {
        if(mClientStartTime != 0) {mClientStartTime = clientStart;}
        if(mServerArrivedTime != 0) {mServerArrivedTime = serverArrived;}
        if(mServerResponseTime != 0) {mServerResponseTime = serverResponse;}
        if(mClientArrivedTime != 0) {mClientArrivedTime = clientArrived;}
    }

    @Override
    public ByteBuf getRvBuffer()
    {
        return _RvBuffer;
    }

    @Override
    public ByteBuf getWrBuffer()
    {
        return _WrBuffer;
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
