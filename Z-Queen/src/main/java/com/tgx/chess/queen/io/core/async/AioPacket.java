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

import java.nio.ByteBuffer;

import com.tgx.chess.queen.io.core.inf.IPacket;

/**
 * @author William.d.zk
 */
public class AioPacket
        implements
        IPacket

{

    private final ByteBuffer _Buf;
    private Status           mStatus = Status.No_Send;
    private boolean          isNoAbandon;
    private int              mIdempotentBit;

    public AioPacket(int size, boolean direct)
    {
        _Buf = size > 0 ? (direct ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size)) : null;
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
    public boolean isNoAbandon()
    {
        return isNoAbandon;
    }

    @Override
    public void setNoAbandon()
    {
        isNoAbandon = true;
    }

    @Override
    public int dataLength()
    {
        return _Buf.capacity();
    }

    @Override
    public int getSerial()
    {
        return 0;
    }

    @Override
    public int getSuperSerial()
    {
        return 0;
    }

    @Override
    public boolean idempotent(int bitIdempotent)
    {
        boolean done = (bitIdempotent & mIdempotentBit) != 0;
        mIdempotentBit |= bitIdempotent;
        return done;
    }
}
