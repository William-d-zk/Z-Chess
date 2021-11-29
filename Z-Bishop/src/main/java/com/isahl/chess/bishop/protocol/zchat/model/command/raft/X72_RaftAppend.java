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

import java.nio.ByteBuffer;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x72)
public class X72_RaftAppend
        extends ZCommand
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
    private long mLeaderId;
    private long mTerm;
    private long mPreIndex;
    private long mPreIndexTerm;
    private long mCommit;
    private long mFollowerId;

    @Override
    public int priority()
    {
        return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
    }

    @Override
    public int length()
    {
        return super.length() + 8 * 6;
    }

    @Override
    public void decodec(ByteBuffer input)
    {
        mLeaderId = input.getLong();
        mTerm = input.getLong();
        mPreIndex = input.getLong();
        mPreIndexTerm = input.getLong();
        mCommit = input.getLong();
        mFollowerId = input.getLong();
    }

    @Override
    public void encodec(ByteBuffer output)
    {
        output.putLong(mLeaderId);
        output.putLong(mTerm);
        output.putLong(mPreIndex);
        output.putLong(mPreIndexTerm);
        output.putLong(mCommit);
        output.putLong(mFollowerId);
    }

    public long getLeaderId()
    {
        return mLeaderId;
    }

    public void setLeaderId(long peerId)
    {
        this.mLeaderId = peerId;
    }

    public long getTerm()
    {
        return mTerm;
    }

    public void setTerm(long term)
    {
        this.mTerm = term;
    }

    public long getCommit()
    {
        return mCommit;
    }

    public void setCommit(long commit)
    {
        this.mCommit = commit;
    }

    public long getPreIndex()
    {
        return mPreIndex;
    }

    public void setPreIndex(long preIndex)
    {
        mPreIndex = preIndex;
    }

    public long getPreIndexTerm()
    {
        return mPreIndexTerm;
    }

    public void setPreIndexTerm(long preIndexTerm)
    {
        mPreIndexTerm = preIndexTerm;
    }

    public void setFollower(long follower)
    {
        mFollowerId = follower;
    }

    public long getFollower()
    {
        return mFollowerId;
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return String.format("X72_RaftAppend{%#x → %#x ; term:%d, pre:%d@%d  commit:%d payload[%d]",
                             mLeaderId,
                             mFollowerId,
                             mTerm,
                             mPreIndex,
                             mPreIndexTerm,
                             mCommit,
                             mPayload == null ? 0 : mPayload.length);
    }
}
