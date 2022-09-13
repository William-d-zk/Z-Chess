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
 * @see X71_RaftBallot,X74_RaftReject
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x70)
public class X70_RaftVote
        extends ZCommand
        implements IRaftRecord
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

    @Override
    public long candidate()
    {
        return mCandidate;
    }

    public void candidate(long peer)
    {
        mCandidate = peer;
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

    public void indexTerm(long term)
    {
        mIndexTerm = term;
    }

    @Override
    public long peer()
    {
        return mElector;
    }

    public void peer(long elector)
    {
        mElector = elector;
    }

    public void accept(long accept)
    {
        mAccept = accept;
    }

    @Override
    public long accept()
    {
        return mAccept;
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
    public long leader()
    {
        return 0;
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
    public Level level()
    {
        return Level.AT_LEAST_ONCE;
    }

}
