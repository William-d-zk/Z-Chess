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
 * @date 2019/12/10
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x74)
public class X74_RaftReject
        extends ZCommand
        implements IRaftRecord,
                   IConsistent
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
    public Level level()
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

    @Override
    public long peer()
    {
        return mPeer;
    }

    public void peer(long peerId)
    {
        mPeer = peerId;
    }

    public long term()
    {
        return mTerm;
    }

    public void term(long term)
    {
        mTerm = term;
    }

    @Override
    public long index()
    {
        return mIndex;
    }

    public void index(long index)
    {
        mIndex = index;
    }

    @Override
    public long indexTerm()
    {
        return mIndexTerm;
    }

    public void indexTerm(long indexTerm)
    {
        mIndexTerm = indexTerm;
    }

    public long reject()
    {
        return mReject;
    }

    public void reject(long peerId)
    {
        mReject = peerId;
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

    public long candidate()
    {
        return mCandidate;
    }

    public void candidate(long candidate)
    {
        mCandidate = candidate;
    }

    @Override
    public int code()
    {
        return mCode;
    }

    public void setCode(int code)
    {
        mCode = code;
    }

    public int state()
    {
        return mState;
    }

    public void state(int state)
    {
        mState = state;
    }

    @Override
    public long accept()
    {
        return mAccept;
    }

    public void accept(long accept)
    {
        mAccept = accept;
    }

    public void commit(long commit)
    {
        mCommit = commit;
    }

    @Override
    public long commit()
    {
        return mCommit;
    }

    @Override
    public String toString()
    {
        return String.format("X74_RaftReject{ peer:%#x, current:[%d@%d;%d], reject to:%#x, code:%d, state:%d }",
                             mPeer,
                             mIndex,
                             mIndexTerm,
                             mTerm,
                             mReject,
                             mCode,
                             mState);
    }

}
