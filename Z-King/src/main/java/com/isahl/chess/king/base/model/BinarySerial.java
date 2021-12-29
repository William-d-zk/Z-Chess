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

/**
 * @author william.d.zk
 * @date 2021-12-22
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-------+---------------+-------------------------------+
 * |         serial-id(16)         |  variable length [7bit](8~32) |
 * +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
 * |                               |  payload data                 |
 * +-------------------------------- - - - - - - - - - - - - - - - +
 * :                     payload data continued ...                :
 * + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
 */
@ISerialGenerator(parent = ISerial.CORE_KING_INTERNAL_SERIAL)
public class BinarySerial
        implements IoSerial
{
    @Override
    public int sizeOf()
    {
        return ByteBuf.vSizeOf(length()) + 2;
    }

    protected byte[]   mPayload;
    protected IoSerial mSubContent;

    @Override
    public int prefix(ByteBuf input)
    {
        int serial = input.getUnsignedShort();
        int length = input.vLength();
        if(serial != serial()) {
            throw new ZException("serial[%d vs %d] no expected", serial, serial());
        }
        return length;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return output.putShort((short) serial())
                     .vPutLength(length());
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
        ByteBuf subBuffer = subEncoded();
        if(subBuffer != null && factory != null) {
            mSubContent = factory.create(subBuffer);
            mSubContent.decode(subBuffer);
        }
    }

    @Override
    public ByteBuf encode()
    {
        return IoSerial.super.encode()
                             .put(mPayload);//ByteBuf.put 忽略null 输入
    }

    @Override
    public int length()
    {
        return mPayload == null ? 0 : mPayload.length;
    }

    @Override
    public byte[] payload()
    {
        return mPayload;
    }

    @Override
    public BinarySerial withSub(IoSerial sub)
    {
        mSubContent = sub;
        if(mSubContent != null) {
            ByteBuf encoded = mSubContent.encode();
            if(encoded.capacity() > 0) {
                mPayload = encoded.array();
            }
        }
        return this;
    }

    @Override
    public IoSerial withSub(byte[] sub)
    {
        mPayload = sub == null || sub.length > 0 ? sub : null;
        return this;
    }

    @Override
    public IoSerial subContent()
    {
        return mSubContent;
    }

    @Override
    public ByteBuf subEncoded()
    {
        return mPayload != null && mPayload.length > 0 ? ByteBuf.wrap(mPayload) : null;
    }

    public static int seekSerial(ByteBuf buffer)
    {
        return buffer.peekUnsignedShort(0);
    }
}
