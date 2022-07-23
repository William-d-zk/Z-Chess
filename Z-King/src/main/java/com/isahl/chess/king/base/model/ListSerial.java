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
import com.isahl.chess.king.base.features.model.ICollectionSerial;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.features.model.IoSerial;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.CORE_KING_INTERNAL_SERIAL)
public class ListSerial<T extends IoSerial>
        extends LinkedList<T>
        implements ICollectionSerial<T>
{
    public static <E extends IoSerial> IoFactory<ListSerial<E>> _Factory(IoFactory<E> factory)
    {
        return input->{
            ListSerial<E> list = new ListSerial<>(factory);
            list.decode(input);
            return list;
        };
    }

    private final IoFactory<T> _Factory;

    public ListSerial(IoFactory<T> factory)
    {
        super();
        _Factory = factory;
    }

    public ListSerial(Collection<T> src, IoFactory<T> factory)
    {
        super(src);
        _Factory = factory;
    }

    public ListSerial(ByteBuf input, IoFactory<T> factory)
    {
        super();
        _Factory = factory;
        decode(input);
    }

    @Override
    public IoFactory<T> factory()
    {
        return _Factory;
    }

    @Override
    public boolean add(T t)
    {
        return t != null && super.add(t);
    }

}
