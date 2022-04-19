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
                  serial = 0x73)
public class X73_RaftAccept
        extends ZCommand
{
    public X73_RaftAccept()
    {
        super();
    }

    public X73_RaftAccept(long msgId)
    {
        super(msgId);
    }

    private long mFollowerId;
    private long mTerm;
    private long mCatchUp;
    private long mCatchUpTerm;
    private long mCommit;
    private long mLeaderId;

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
    public int length()
    {
        return super.length() + 48;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mFollowerId = input.getLong();
        mTerm = input.getLong();
        mCatchUp = input.getLong();
        mCatchUpTerm = input.getLong();
        mCommit = input.getLong();
        mLeaderId = input.getLong();
        return remain - 48;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mFollowerId)
                    .putLong(mTerm)
                    .putLong(mCatchUp)
                    .putLong(mCatchUpTerm)
                    .putLong(mCommit)
                    .putLong(mLeaderId);
    }

    public long getTerm()
    {
        return mTerm;
    }

    public void setTerm(long term)
    {
        mTerm = term;
    }

    public long getCatchUp()
    {
        return mCatchUp;
    }

    public void setCatchUp(long catchUp)
    {
        mCatchUp = catchUp;
    }

    public long getCatchUpTerm()
    {
        return mCatchUpTerm;
    }

    public void setCatchUpTerm(long catchUpTerm)
    {
        mCatchUpTerm = catchUpTerm;
    }

    public void setCommit(long commit)
    {
        mCommit = commit;
    }

    public long getCommit()
    {
        return mCommit;
    }

    public long getFollower()
    {
        return mFollowerId;
    }

    public void setFollower(long peerId)
    {
        mFollowerId = peerId;
    }

    public void setLeader(long leaderId)
    {
        mLeaderId = leaderId;
    }

    public long getLeader()
    {
        return mLeaderId;
    }

    @Override
    public String toString()
    {
        return String.format("X73_RaftAccept{ follower:%#x@%d, catch_up:%d@%d }",
                             mFollowerId,
                             mTerm,
                             mCatchUp,
                             mCatchUpTerm);
    }

}
