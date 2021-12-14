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
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;

import java.nio.ByteBuffer;

/**
 * @author William.d.zk
 */
@ISerialGenerator(parent = ISerial.IO_QUEEN_PACKET_SERIAL)
public class AioPacket
        extends ByteBuf
        implements IPacket
{

    private Status mStatus = Status.No_Send;
    private int    mRightIdempotentBit;
    private int    mLeftIdempotentBit;

    public AioPacket(int size, boolean direct)
    {
        super(size, direct);
    }

    public AioPacket(ByteBuf exist)
    {
        super(exist);
    }

    @Override
    public ByteBuf getBuffer()
    {
        return this;
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
        return 1 + capacity;
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
    public int prefix(ByteBuf input)
    {
        int length = input.vLength();
        int serial = input.get();
        if(serial != serial()) {
            throw new ZException("serial[%d vs %d] no expected", serial, serial());
        }
        return length - 1;
    }

    @Override
    public void fold(ByteBuf input, int remain)
    {
        if(remain > 0) {
            byte[] array = new byte[remain];
            input.get(array);
            buffer = ByteBuffer.wrap(array);
            capacity = remain;
        }
    }

    @Override
    public int sizeOf()
    {
        return vSizeOf(length());
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return output.vPutLength(length())
                     .put(serial());
    }

    @Override
    public ByteBuf encode()
    {
        return IPacket.super.encode()
                            .put(buffer.array());
    }

}
