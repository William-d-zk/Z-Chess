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

import com.isahl.chess.board.base.IFactory;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.model.BinarySerial;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.db.model.IStorage;

import java.io.DataInput;
import java.io.IOException;

/**
 * @author william.d.zk
 */
public abstract class InnerProtocol
        extends BinarySerial
        implements IStorage
{
    protected final Operation   _Operation;
    protected final Strategy    _Strategy;
    protected final CreatorType _Type;

    protected long pKey, fKey;

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
        return 1 + //operation (1)
               1 + //strategy (1)
               8 + //primaryKey (8)
               (hasForeignKey() ? 9 : 1) + //hasForeignKey ? (9) : (1)
               super.length(); //payload.length
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

    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        input.skip(2);
        remain -= 2;
        pKey = input.getLong();
        remain -= 8;
        if(input.get() > 0) {
            fKey = input.getLong();
            remain -= 8;
        }
        return remain - 1;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output = super.suffix(output)
                      .put(operation().getValue())
                      .put(strategy().getCode())
                      .putLong(pKey)
                      .put(hasForeignKey() ? 1 : 0);
        if(hasForeignKey()) {
            output.putLong(fKey);
        }
        return output;
    }

    @SuppressWarnings("unchecked")
    public static <T extends InnerProtocol> T load(IFactory factory, DataInput input) throws IOException
    {
        final Logger _Logger = Logger.getLogger("io.queen." + InnerProtocol.class.getSimpleName());
        //TODO 升级成zero-copy 模式【mapping-buffer】
        int length = IoUtil.readVariableIntLength(input);
        if(length > 0) {
            ByteBuf buffer = ByteBuf.allocate(length);
            input.readFully(buffer.array(), buffer.writerIdx(), buffer.writableBytes());
            buffer.seek(length);
            int serial = buffer.peekUnsignedShort(0);
            T t = (T) factory.build(serial);
            if(t.serial() == serial) { //vLength ≥ 1
                t.decode(buffer);
            }
            _Logger.info("inner-protool load:[%s]", t.toString());
            return t;
        }
        return null;
    }

}
