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

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x79)
public class X7C_RaftConfirm
        extends ZCommand
        implements IRaftRecord
{

    public X7C_RaftConfirm()
    {
        super();
    }

    public X7C_RaftConfirm(long msgId)
    {
        super(msgId);
    }

    private long mFollower;
    private long mTerm;
    private long mCatchUp;
    private long mCatchUpTerm;
    private long mCommit;
    private long mLeader;

    @Override
    public int priority()
    {
        return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
    }

    @Override
    public Level getLevel()
    {
        return Level.AT_LEAST_ONCE;
    }

    @Override
    public String toString()
    {
        return String.format("X7C_RaftConfirm { peer:%#x,[%d@%d;%d],commit:%d}",
                             mFollower,
                             mCatchUp,
                             mCatchUpTerm,
                             mTerm,
                             mCommit);
    }

    public long peer()
    {
        return mFollower;
    }

    public void peer(long peer)
    {
        mFollower = peer;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mFollower)
                    .putLong(mTerm)
                    .putLong(mCatchUp)
                    .putLong(mCatchUpTerm)
                    .putLong(mCommit)
                    .putLong(mLeader);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mFollower = input.getLong();
        mTerm = input.getLong();
        mCatchUp = input.getLong();
        mCatchUpTerm = input.getLong();
        mCommit = input.getLong();
        mLeader = input.getLong();
        return remain - 48;
    }

    @Override
    public int length()
    {
        return super.length() + 48;
    }

    @Override
    public long index()
    {
        return mCatchUp;
    }

    public void index(long index)
    {
        mCatchUp = index;
    }

    @Override
    public long indexTerm()
    {
        return mCatchUpTerm;
    }

    public void indexTerm(long term)
    {
        mCatchUpTerm = term;
    }

    @Override
    public long term()
    {
        return mTerm;
    }

    public void term(long term)
    {
        mTerm = term;
    }

    @Override
    public long accept()
    {
        return mCatchUp;
    }

    @Override
    public long commit()
    {
        return mCommit;
    }

    public void commit(long commit)
    {
        mCommit = commit;
    }

    @Override
    public long candidate()
    {
        return mLeader;
    }

    @Override
    public long leader()
    {
        return mLeader;
    }

    public void leader(long leader)
    {
        mLeader = leader;
    }
}
