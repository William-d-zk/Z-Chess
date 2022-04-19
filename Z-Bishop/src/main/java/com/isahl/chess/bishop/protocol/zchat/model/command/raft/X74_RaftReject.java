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
                  serial = 0x74)
public class X74_RaftReject
        extends ZCommand
{

    public X74_RaftReject(long msgId)
    {
        super(msgId);
    }

    public X74_RaftReject()
    {
        super();
    }

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
        return super.length() + 74;
    }

    private long mPeer;
    private long mTerm;
    private long mIndex;
    private long mIndexTerm;
    private long mAccept;
    private long mCommit;
    private long mReject;
    private long mCandidate;
    private long mLeader;
    private int  mCode;
    private int  mState;

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mPeer = input.getLong();
        mTerm = input.getLong();
        mIndex = input.getLong();
        mIndexTerm = input.getLong();
        mAccept = input.getLong();
        mCommit = input.getLong();
        mReject = input.getLong();
        mCandidate = input.getLong();
        mLeader = input.getLong();
        mCode = input.get();
        mState = input.get();
        return remain - 74;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mPeer)
                    .putLong(mTerm)
                    .putLong(mIndex)
                    .putLong(mIndexTerm)
                    .putLong(mAccept)
                    .putLong(mCommit)
                    .putLong(mReject)
                    .putLong(mCandidate)
                    .putLong(mLeader)
                    .put(mCode)
                    .put(mState);
    }

    public long getPeer()
    {
        return mPeer;
    }

    public void setPeer(long peerId)
    {
        mPeer = peerId;
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

    public void setIndexTerm(long indexTerm)
    {
        mIndexTerm = indexTerm;
    }

    public long getReject()
    {
        return mReject;
    }

    public void setReject(long peerId)
    {
        mReject = peerId;
    }

    public long getLeader()
    {
        return mLeader;
    }

    public void setLeader(long leader)
    {
        mLeader = leader;
    }

    public long getCandidate()
    {
        return mCandidate;
    }

    public void setCandidate(long candidate)
    {
        mCandidate = candidate;
    }

    public int getCode()
    {
        return mCode;
    }

    public void setCode(int code)
    {
        mCode = code;
    }

    public int getState()
    {
        return mState;
    }

    public void setState(int state)
    {
        mState = state;
    }

    public long getAccept()
    {
        return mAccept;
    }

    public void setAccept(long accept)
    {
        mAccept = accept;
    }

    public void setCommit(long commit)
    {
        mCommit = commit;
    }

    public long getCommit()
    {
        return mCommit;
    }

    @Override
    public String toString()
    {
        return String.format("X74_RaftReject{ peerId:%#x,term:%d,current:%d@%d,reject:%#x,code:%d,state:%d }",
                             mPeer,
                             mTerm,
                             mIndex,
                             mIndexTerm,
                             mReject,
                             mCode,
                             mState);
    }

}
