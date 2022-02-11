/*
 * MIT License
 *
 * Copyright (c) 2022~2022. Z-Chess
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

package com.isahl.chess.king.base.features.model;

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;

import java.util.Collection;

public interface ICollectionSerial<T extends IoSerial>
        extends IoSerial,
                Collection<T>
{

    int SIZE_POS   = 6;
    int SERIAL_POS = 0;
    int LENGTH_POS = 2;

    IoFactory<T> factory();

    default void fold(ByteBuf input, int remain)
    {
        if(remain > 0) {
            throw new ZException("collection serial fold error [remain %d]", remain);
        }
    }

    @Override
    default int prefix(ByteBuf input)
    {
        int serial = input.getUnsignedShort();
        int length = input.getInt();
        int size = input.getInt();
        for(int i = 0; i < size; i++) {
            T content = factory().create(input);
            add(content);
            length -= content.sizeOf();
        }
        return length - 4;
    }

    @Override
    default ByteBuf suffix(ByteBuf output)
    {
        output = output.putShort((short) serial())
                       .putInt(length())
                       .putInt(size());
        for(IoSerial t : this) {
            output.put(t.encode());
        }
        return output;
    }

    @Override
    default int length()
    {
        int length = 4;  //collection.size
        for(IoSerial t : this) {
            length += t.sizeOf();
        }
        return length;
    }

    @Override
    default int sizeOf()
    {
        return length() + 4 + 2;
    }

    @Override
    default IoSerial subContent()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default ICollectionSerial<T> withSub(IoSerial sub)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default byte[] payload()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default IoSerial withSub(byte[] payload)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default <E extends IoSerial> void deserializeSub(IoFactory<E> factory)
    {
        throw new UnsupportedOperationException();
    }
}
