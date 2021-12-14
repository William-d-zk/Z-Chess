/*
 * MIT License
 *
 * Copyright (c) 2021~2021. Z-Chess
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

package com.isahl.chess.king.base.model;

import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.features.model.IoSerial;

import java.util.Objects;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.CORE_KING_INTERNAL_SERIAL)
public class BinarySerial
        implements IoSerial
{

    protected byte[]   mPayload;
    protected IoSerial mSubContent;

    @Override
    public int prefix(ByteBuf input)
    {
        int length = input.vLength();
        int serial = input.getUnsignedShort();
        if(serial != serial()) {
            throw new ZException("serial[%d vs %d] no expected", serial, serial());
        }
        return length - 2;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return output.vPutLength(length())
                     .putShort((short) serial());
    }

    @Override
    public void fold(ByteBuf input, int remain)
    {
        if(remain > 0) {
            mPayload = new byte[remain];
            input.get(mPayload);
        }
    }

    @Override
    public void deserializeSub(IoFactory factory)
    {
        ByteBuf payload = payload();
        mSubContent = Objects.requireNonNull(factory)
                             .create(payload);
        mSubContent.decode(payload);
    }

    @Override
    public ByteBuf encode()
    {
        ByteBuf output = IoSerial.super.encode();
        if(mPayload != null) {
            output.put(mPayload);
        }
        return output;
    }

    @Override
    public int length()
    {
        return 2 + (mPayload == null ? 0 : mPayload.length);
    }

    public void put(byte[] payload)
    {
        mPayload = payload;
    }

    public ByteBuf payload()
    {
        return mPayload == null ? null : ByteBuf.wrap(mPayload);
    }

    @Override
    public void withSub(IoSerial sub)
    {
        mSubContent = sub;
        mPayload = mSubContent.encode()
                              .array();
    }

    @Override
    public IoSerial subContent()
    {
        return mSubContent;
    }

    public static int seekSerial(ByteBuf buffer)
    {
        buffer.markReader();
        int length = buffer.vLength();
        int serial = buffer.getUnsignedShort();
        buffer.resetReader();
        return serial;
    }
}
