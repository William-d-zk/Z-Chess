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

package com.isahl.chess.bishop.protocol.zchat.model.command;

import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.util.IoUtil;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x1F)
public class X1F_Exchange
        extends ZCommand
{
    public X1F_Exchange()
    {
        super();
    }

    public X1F_Exchange(long msgId)
    {
        super(msgId);
    }

    @Override
    public boolean isMapping()
    {
        return false;
    }

    private long mPeer;
    private long mTarget;

    private int mFactory;

    @Override
    public int priority()
    {
        return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
    }

    @Override
    public Level level()
    {
        return Level.AT_LEAST_ONCE;
    }

    @Override
    public String toString()
    {
        return String.format("X0F_Exchange { node-client:%#x, origin:%#x, factory:%s sub-size[%d]}",
                             mPeer,
                             mTarget,
                             IoUtil.int2Chars(mFactory),
                             payload() == null ? payload().length : 0);
    }

    public void target(long target)
    {
        mTarget = target;
    }

    public long target()
    {
        return mTarget;
    }

    public int factory()
    {
        return mFactory;
    }

    public void factory(int factory)
    {
        mFactory = factory;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mPeer)
                    .putLong(mTarget)
                    .putInt(mFactory);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mPeer = input.getLong();
        mTarget = input.getLong();
        mFactory = input.getInt();
        return remain - 20;
    }

    @Override
    public int length()
    {
        return super.length() + 20;
    }

    public long peer()
    {
        return mPeer;
    }

    public void peer(long peer)
    {
        mPeer = peer;
    }

}
