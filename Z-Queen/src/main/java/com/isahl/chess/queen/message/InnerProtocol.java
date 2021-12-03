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

package com.isahl.chess.queen.message;

import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.db.model.IStorage;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

/**
 * @author william.d.zk
 */
public abstract class InnerProtocol
        implements IStorage
{

    protected final Operation   _Operation;
    protected final Strategy    _Strategy;
    protected final CreatorType _Type;

    protected long pKey, fKey;

    protected byte[] mPayload;

    enum CreatorType
    {
        ANONYMOUS,
        CUSTOM
    }

    protected InnerProtocol(Operation operation, Strategy strategy)
    {
        this(operation, strategy, CreatorType.CUSTOM);
    }

    public InnerProtocol()
    {
        this(Operation.OP_NULL, Strategy.CLEAN, CreatorType.ANONYMOUS);
    }

    private InnerProtocol(Operation operation, Strategy strategy, CreatorType type)
    {
        _Operation = operation;
        _Strategy = strategy;
        _Type = type;
    }

    @Override
    public int length()
    {
        /*
         operation (1)
         strategy (1)
         serial(2)
         primaryKey (8)
         hasForeignKey (1) ? (9) : (1)
         */
        return 8 + 2 + 1 + 1 + (hasForeignKey() ? 9 : 1) + (mPayload == null ? 0 : mPayload.length);
    }

    @Override
    public long primaryKey()
    {
        return pKey;
    }

    @Override
    public long foreignKey()
    {
        return fKey;
    }

    public void bind(long key)
    {
        fKey = key;
    }

    public void generate(long primaryKey)
    {
        pKey = primaryKey;
    }

    @Override
    public void put(byte[] payload)
    {
        mPayload = payload;
    }

    @Override
    public ByteBuffer payload()
    {
        return mPayload != null ? ByteBuffer.wrap(mPayload) : null;
    }

    @Override
    public boolean hasForeignKey()
    {
        return fKey != 0;
    }

    @Override
    public Strategy strategy()
    {
        return _Strategy;
    }

    @Override
    public Operation operation()
    {
        return _Operation;
    }

    @Override
    public void decode(ByteBuffer input)
    {
        IStorage.super.decode(input);
        pKey = input.getLong();
        if(input.get() > 0) {
            fKey = input.getLong();
        }
    }

    public static <T extends InnerProtocol> T load(Class<T> clazz,
                                                   DataInput input) throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        int length = IoUtil.readVariableIntLength(input);
        byte[] vLength = IoUtil.variableLength(length);
        ByteBuffer buffer = ByteBuffer.allocate(length + vLength.length);
        buffer.put(vLength);
        T t = clazz.getDeclaredConstructor()
                   .newInstance();
        if(length > 0 && t.serial() == input.readUnsignedShort()) { //vLength ≥ 1
            buffer.putShort((short) t.serial());
            input.readFully(buffer.array(), buffer.position(), buffer.remaining());
            t.decode(buffer.clear());
        }
        return t;
    }

}
