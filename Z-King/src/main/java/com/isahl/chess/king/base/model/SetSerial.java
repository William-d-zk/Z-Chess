/*
 * MIT License
 *
 * Copyright (c) 2022. Z-Chess
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
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.features.model.IoSerial;

import java.util.Collection;
import java.util.TreeSet;

/**
 * @author william.d.zk
 * @date 2022-01-07
 */
@ISerialGenerator(parent = ISerial.CORE_KING_INTERNAL_SERIAL)
public class SetSerial<T extends IoSerial & Comparable<T>>
        extends TreeSet<T>
        implements ICollectionSerial<T>
{
    private final IoFactory<T> _Factory;

    public SetSerial(IoFactory<T> factory)
    {
        super();
        _Factory = factory;
    }

    public SetSerial(IoFactory<T> factory, Collection<T> e)
    {
        super(e);
        _Factory = factory;
    }

    public SetSerial(ByteBuf input, IoFactory<T> factory)
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

}
