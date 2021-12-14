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

import java.util.LinkedList;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.CORE_KING_INTERNAL_SERIAL)
public class ListSerial
        extends LinkedList<IoSerial>
        implements IoSerial
{

    private final IoFactory _Factory;

    public ListSerial(IoFactory factory)
    {
        super();
        _Factory = factory;
    }

    @Override
    public void fold(ByteBuf input, int remain)
    {
        if(remain > 0) {
            throw new ZException("list serial fold error [remain %d]", remain);
        }
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int length = input.vLength();
        int serial = input.getUnsignedShort();
        int size = input.getInt();
        for(int i = 0; i < size; i++) {
            IoSerial t = _Factory.create(input);
            add(t);
            length -= t.sizeOf();
        }
        return length - 4;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output = output.vPutLength(length())
                       .putShort((short) serial())
                       .putInt(size());
        for(IoSerial t : this) {
            output.put(t.encode());
        }
        return output;
    }

    @Override
    public int length()
    {
        int length = 6;
        for(IoSerial t : this) {
            length += t.sizeOf();
        }
        return length;
    }

    @Override
    public IoSerial subContent()
    {
        return getFirst();
    }

    @Override
    public void withSub(IoSerial sub)
    {
        add(sub);
    }

    @Override
    public void deserializeSub(IoFactory factory)
    {
        throw new UnsupportedOperationException();
    }
}
