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

package com.isahl.chess.bishop.protocol.zchat.model.command.raft;

import com.isahl.chess.bishop.protocol.zchat.model.command.ZCommand;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x7A)
public class X7A_RaftJoint
        extends ZCommand
        implements IConsistent
{

    public X7A_RaftJoint()
    {
        super();
    }

    public X7A_RaftJoint(long msgId)
    {
        super(msgId);
    }

    private long mPeer;
    private long mIndex;
    private long mOrigin;
    private int  mCode;

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
        return String.format("X7A_RaftJoint { answer:%d, peer:%#x, origin:%#x, code:%d}",
                             mIndex,
                             mPeer,
                             mOrigin,
                             mCode);
    }

    public void origin(long origin)
    {
        mOrigin = origin;
    }

    public long origin()
    {
        return mOrigin;
    }

    @Override
    public int code()
    {
        return mCode;
    }

    public void code(int code)
    {
        mCode = code;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mPeer)
                    .putLong(mIndex)
                    .putLong(mOrigin)
                    .putInt(mCode);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mPeer = input.getLong();
        mIndex = input.getLong();
        mOrigin = input.getLong();
        mCode = input.getInt();
        return remain - 28;
    }

    @Override
    public int length()
    {
        return super.length() + 28;
    }

    public long peer()
    {
        return mPeer;
    }

    public void peer(long peer)
    {
        mPeer = peer;
    }

    public long index()
    {
        return mIndex;
    }

    public void index(long index)
    {
        this.mIndex = index;
    }

}
