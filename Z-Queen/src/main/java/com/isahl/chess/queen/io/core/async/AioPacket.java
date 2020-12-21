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

import com.isahl.chess.queen.io.core.inf.IPacket;

/**
 * @author William.d.zk
 */
public class AioPacket
        implements
        IPacket
{
    public final static int SERIAL = PACKET_SERIAL + 1;

    private final ByteBuffer _Buf;
    private Status           mStatus = Status.No_Send;
    private int              mRightIdempotentBit;
    private int              mLeftIdempotentBit;

    public AioPacket(int size,
                     boolean direct)
    {
        _Buf = size > 0 ? (direct ? ByteBuffer.allocateDirect(size): ByteBuffer.allocate(size)): null;

    }

    public AioPacket(ByteBuffer buf)
    {
        _Buf = buf;
    }

    @Override
    public ByteBuffer getBuffer()
    {
        return _Buf;
    }

    @Override
    public IPacket wrapper(ByteBuffer buffer)
    {
        return new AioPacket(buffer);
    }

    @Override
    public boolean isSending()
    {
        return mStatus.equals(Status.Sending);
    }

    @Override
    public boolean isSent()
    {
        return mStatus.equals(Status.Sent);
    }

    @Override
    public IPacket send()
    {
        mStatus = Status.Sending;
        return this;
    }

    @Override
    public IPacket waitSend()
    {
        mStatus = Status.To_Send;
        return this;
    }

    @Override
    public IPacket noSend()
    {
        mStatus = Status.No_Send;
        return this;
    }

    @Override
    public IPacket sent()
    {
        mStatus = Status.Sent;
        return this;
    }

    @Override
    public Status getStatus()
    {
        return mStatus;
    }

    @Override
    public void setAbandon()
    {
        mStatus = Status.Abandon;
    }

    @Override
    public int dataLength()
    {
        return _Buf.capacity();
    }

    @Override
    public int serial()
    {
        return SERIAL;
    }

    @Override
    public int superSerial()
    {
        return PACKET_SERIAL;
    }

    @Override
    public boolean inIdempotent(int bit)
    {
        boolean todo = (bit & mRightIdempotentBit) == 0;
        mRightIdempotentBit |= bit;
        return todo;
    }

    @Override
    public boolean outIdempotent(int bit)
    {
        boolean todo = (bit & mLeftIdempotentBit) == 0;
        mLeftIdempotentBit |= bit;
        return todo;
    }

    @Override
    public IPacket flip()
    {
        _Buf.flip();
        return this;
    }
}
