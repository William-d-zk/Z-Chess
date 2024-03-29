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

package com.isahl.chess.king.base.features.model;

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;

import java.util.Map;
import java.util.NavigableMap;

public interface IMapSerial<K extends Comparable<K>, V extends IoSerial>
        extends IoSerial,
                NavigableMap<K, V>
{
    int SERIAL_POS = 0;
    int LENGTH_POS = 2;
    int SIZE_POS   = 6;

    IoFactory<V> factory();

    default void fold(ByteBuf input, int remain)
    {
        if(remain > 0) {
            throw new ZException("map serial fold error [remain %d]", remain);
        }
    }

    int sizeOf(K k);

    K nextKey(ByteBuf input);

    void key(ByteBuf output, K k);

    @Override
    default int prefix(ByteBuf input)
    {
        int serial = input.getUnsignedShort();
        int length = input.getInt();
        int size = input.getInt();
        for(int i = 0; i < size; i++) {
            K key = nextKey(input);
            length -= sizeOf(key);
            V value = factory().create(input);
            put(key, value);
            length -= value.sizeOf();
        }
        return length - 4;
    }

    @Override
    default ByteBuf suffix(ByteBuf output)
    {
        output = output.putShort(serial())
                       .putInt(length())
                       .putInt(size());
        for(Map.Entry<K, V> e : entrySet()) {
            key(output, e.getKey());
            output.put(e.getValue()
                        .encode());
        }
        return output;
    }

    @Override
    default int length()
    {
        int length = 0;
        for(Map.Entry<K, V> e : entrySet()) {
            length += sizeOf(e.getKey());
            length += e.getValue()
                       .sizeOf();
        }
        return length;
    }

    @Override
    default int sizeOf()
    {
        return 2 + // serial
               4 + // length
               4 + // size
               length();
    }

    @Override
    default IoSerial subContent()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default IMapSerial<K, V> withSub(IoSerial sub)
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
    default <E extends IoSerial> E deserializeSub(IoFactory<E> factory)
    {
        throw new UnsupportedOperationException();
    }

    enum KeyType
    {
        _String(String.class.getTypeName(), (byte) 0),
        _Long(Long.class.getTypeName(), (byte) 1),
        _Integer(Integer.class.getTypeName(), (byte) 2),
        _Byte(Byte.class.getTypeName(), (byte) 3);

        private final String _Type;
        private final byte   _Id;

        KeyType(String type, byte id)
        {
            _Type = type;
            _Id = id;
        }

        public String type() {return _Type;}

        public byte id() {return _Id;}

        public static KeyType typeOf(String type)
        {
            for(KeyType t : KeyType.values()) {
                if(t._Type.equals(type)) {
                    return t;
                }
            }
            throw new IllegalArgumentException();
        }
        public static KeyType idOf(byte id){
            return  switch(id){
                case 0->_String;
                case 1->_Long;
                case 2->_Integer;
                case 3->_Byte;
                default -> throw new IllegalArgumentException();
            };
        }
    }

}
