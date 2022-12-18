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
import com.isahl.chess.king.base.features.model.IMapSerial;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.features.model.IoSerial;

import java.nio.charset.StandardCharsets;
import java.util.NavigableMap;
import java.util.TreeMap;

import static com.isahl.chess.king.base.features.model.IMapSerial.KeyType.*;

/**
 * @author william.d.zk
 * @date 2022-01-07
 */
@ISerialGenerator(parent = ISerial.CORE_KING_INTERNAL_SERIAL)
public class MapSerial<K extends Comparable<K>, V extends IoSerial>
        extends TreeMap<K, V>
        implements IMapSerial<K, V>
{
    private final IoFactory<V> _Factory;

    public MapSerial(IoFactory<V> factory)
    {
        super();
        _Factory = factory;
    }

    public MapSerial(IoFactory<V> factory, NavigableMap<K, V> map)
    {
        super(map);
        _Factory = factory;
    }

    public MapSerial(ByteBuf input, IoFactory<V> factory)
    {
        super();
        _Factory = factory;
        decode(input);
    }

    @Override
    public IoFactory<V> factory()
    {
        return _Factory;
    }

    @Override
    public int sizeOf(K k)
    {
        switch(KeyType.typeOf(k.getClass()
                               .getTypeName())) {
            case _String -> {
                byte[] b = ((String) k).getBytes(StandardCharsets.UTF_8);
                return ByteBuf.vSizeOf(b.length);
            }
            case _Byte -> {return 1;}
            case _Integer -> {return 4;}
            case _Long -> {return 8;}
            default -> throw new UnsupportedOperationException();
        }
    }

    @Override
    public K nextKey(ByteBuf input)
    {
        byte id = input.get();
        return (K) switch(KeyType.idOf(id)) {
            case _String -> new String(input.vGet());
            case _Long -> input.getLong();
            case _Integer -> input.getInt();
            case _Byte -> input.get();
        };
    }

    @Override
    public void key(ByteBuf output, K k)
    {
        switch(KeyType.valueOf(k.getClass()
                                .getTypeName())) {
            case _String -> {
                byte[] b = ((String) k).getBytes(StandardCharsets.UTF_8);
                output.put(_String.id());
                output.vPut(b);
            }
            case _Byte -> {
                output.put(_Byte.id());
                output.put((Byte) k);
            }
            case _Integer -> {
                output.put(_Integer.id());
                output.putInt((Integer) k);
            }
            case _Long -> {
                output.put(_Long.id());
                output.putLong((Long) k);
            }
            default -> throw new UnsupportedOperationException();
        }
    }

}
