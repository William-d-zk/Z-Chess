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
 * @date 2019/12/10
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x70)
public class X70_RaftVote
        extends ZCommand
{

    public X70_RaftVote(long msgId)
    {
        super(msgId);
    }

    public X70_RaftVote()
    {
        super();
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
    }

    private long mCandidate;
    private long mElector;
    private long mTerm;
    private long mIndex;
    private long mIndexTerm;
    private long mAccept;
    private long mCommit;

    @Override
    public int length()
    {
        return super.length() + 56;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mCandidate = input.getLong();
        mElector = input.getLong();
        mTerm = input.getLong();
        mIndex = input.getLong();
        mIndexTerm = input.getLong();
        mAccept = input.getLong();
        mCommit = input.getLong();
        return remain - 56;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mCandidate)
                    .putLong(mElector)
                    .putLong(mTerm)
                    .putLong(mIndex)
                    .putLong(mIndexTerm)
                    .putLong(mAccept)
                    .putLong(mCommit);
    }

    public long getCandidate()
    {
        return mCandidate;
    }

    public void setCandidate(long peerId)
    {
        mCandidate = peerId;
    }

    public long getTerm()
    {
        return mTerm;
    }

    public void setTerm(long term)
    {
        mTerm = term;
    }

    public long getIndex()
    {
        return mIndex;
    }

    public void setIndex(long index)
    {
        mIndex = index;
    }

    public long getIndexTerm()
    {
        return mIndexTerm;
    }

    public void setIndexTerm(long term)
    {
        mIndexTerm = term;
    }

    public long getElector()
    {
        return mElector;
    }

    public void setElector(long elector)
    {
        mElector = elector;
    }

    public void setAccept(long accept)
    {
        mAccept = accept;
    }

    public long getAccept()
    {
        return mAccept;
    }

    public long getCommit()
    {
        return mCommit;
    }

    public void setCommit(long commit)
    {
        mCommit = commit;
    }

    @Override
    public String toString()
    {
        return String.format("X70_RaftVote{ candidate=%#x from %#x term: %d,last: %d@%d, accept=%d, commit=%d }",
                             mCandidate,
                             mElector,
                             mTerm,
                             mIndex,
                             mIndexTerm,
                             mAccept,
                             mCommit);
    }

    @Override
    public Level getLevel()
    {
        return Level.AT_LEAST_ONCE;
    }

}
