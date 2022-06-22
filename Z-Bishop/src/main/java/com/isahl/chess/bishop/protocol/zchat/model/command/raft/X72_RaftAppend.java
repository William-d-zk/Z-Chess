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
 * @see X73_RaftAccept,X74_RaftReject
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x72)
public class X72_RaftAppend
        extends ZCommand
        implements IRaftRecord
{

    public X72_RaftAppend(long msgId)
    {
        super(msgId);
    }

    public X72_RaftAppend()
    {
        super();
    }

    // leaderId
    private long mLeader;
    private long mTerm;
    private long mPreIndex;
    private long mPreIndexTerm;
    private long mAccept;
    private long mCommit;
    private long mFollower;

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
        return super.length() + 56;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mLeader = input.getLong();
        mTerm = input.getLong();
        mPreIndex = input.getLong();
        mPreIndexTerm = input.getLong();
        mAccept = input.getLong();
        mCommit = input.getLong();
        mFollower = input.getLong();
        return remain - 56;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mLeader)
                    .putLong(mTerm)
                    .putLong(mPreIndex)
                    .putLong(mPreIndexTerm)
                    .putLong(mAccept)
                    .putLong(mCommit)
                    .putLong(mFollower);
    }

    @Override
    public long leader()
    {
        return mLeader;
    }

    public void leader(long peerId)
    {
        this.mLeader = peerId;
    }

    @Override
    public long term()
    {
        return mTerm;
    }

    public void term(long term)
    {
        this.mTerm = term;
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

    @Override
    public long commit()
    {
        return mCommit;
    }

    public void commit(long commit)
    {
        this.mCommit = commit;
    }

    @Override
    public long index()
    {
        return mPreIndex;
    }

    @Override
    public long indexTerm()
    {
        return mPreIndexTerm;
    }

    public void preIndex(long preIndex)
    {
        mPreIndex = preIndex;
    }

    public void preIndexTerm(long preIndexTerm)
    {
        mPreIndexTerm = preIndexTerm;
    }

    public void setFollower(long follower)
    {
        mFollower = follower;
    }

    @Override
    public long peer()
    {
        return mFollower;
    }

    @Override
    public long candidate()
    {
        return leader();
    }

    @Override
    public String toString()
    {
        return String.format("X72_RaftAppend{ %#x → %#x ; term:%d, pre:%d@%d  commit:%d  payload-size[%d] }",
                             mLeader,
                             mFollower,
                             mTerm,
                             mPreIndex,
                             mPreIndexTerm,
                             mCommit,
                             mPayload == null ? 0 : mPayload.length);
    }

}
