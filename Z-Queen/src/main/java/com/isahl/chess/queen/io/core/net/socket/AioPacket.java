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

import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;

import java.nio.ByteBuffer;

/**
 * @author William.d.zk
 */
@ISerialGenerator(parent = ISerial.IO_QUEEN_PACKET_SERIAL)
public class AioPacket
        implements IPacket
{

    private ByteBuffer mBuf;
    private Status     mStatus = Status.No_Send;
    private int        mRightIdempotentBit;
    private int        mLeftIdempotentBit;

    public AioPacket(int size, boolean direct)
    {
        mBuf = size > 0 ? (direct ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size)) : null;
    }

    public AioPacket(ByteBuffer buf)
    {
        mBuf = buf;
    }

    @Override
    public ByteBuffer payload()
    {
        return mBuf;
    }

    @Override
    public ByteBuffer getBuffer()
    {
        return mBuf;
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
    public int length()
    {
        return mBuf.limit();
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
        mBuf.flip();
        return this;
    }

    @Override
    public void put(byte[] payload)
    {
        if(mBuf.isDirect() && mBuf.capacity() < payload.length) {
            mBuf = ByteBuffer.allocateDirect(payload.length);
            IoUtil.write(payload, 0, mBuf.array(), 0, payload.length);
        }
        else if(mBuf.capacity() < payload.length) {
            mBuf = ByteBuffer.wrap(payload);
        }
        else {
            mBuf.clear();
            IoUtil.write(payload, 0, mBuf.array(), 0, payload.length);
            mBuf.limit(payload.length);
        }
    }

    @Override
    public void put(ByteBuffer src)
    {
        if(mBuf.capacity() < src.remaining()) {
            int size = src.remaining();
            mBuf = mBuf.isDirect() ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
        }
        else {
            mBuf.clear();
        }
        IoUtil.write(src.array(), src.position(), mBuf.array(), mBuf.position(), src.remaining());
        //mBuf.position == 0
        mBuf.limit(src.remaining());
    }

    @Override
    public void replace(ByteBuffer src)
    {
        mBuf = src;
    }

    @Override
    public void expand(int size)
    {
        mBuf = IoUtil.expandBuffer(mBuf, size);
    }

    public void append(IPacket other)
    {
        if(mBuf.remaining() < other.length()) {
            expand(other.length() - mBuf.remaining());
        }
        put(other.encode()
                 .flip());
    }

    @Override
    public void decodec(ByteBuffer input)
    {
        mBuf.put(input);
    }

    @Override
    public void encodec(ByteBuffer output)
    {
        /*
        int len = Math.min(output.remain,mBuf.remain)
        mBuf.pos → mBuf.pos<=mBuf.limit && mBuf.pos=mBuf.pos + len
        output.pos → output.pos=output.pos+len
         */
        output.put(mBuf);
    }
}
