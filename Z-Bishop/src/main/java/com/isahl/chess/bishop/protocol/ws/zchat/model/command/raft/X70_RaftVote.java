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

package com.isahl.chess.bishop.protocol.ws.zchat.model.command.raft;

import com.isahl.chess.bishop.protocol.ws.zchat.model.ZCommand;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.util.IoUtil;

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
        super(true);
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
    }

    // candidateId
    private long mCandidateId;
    private long mTerm;
    private long mIndex;
    private long mIndexTerm;
    private long mCommit;
    private long mElectorId;

    @Override
    public int length()
    {
        return super.length() + 48;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mCandidateId = IoUtil.readLong(data, pos);
        pos += 8;
        mTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mIndex = IoUtil.readLong(data, pos);
        pos += 8;
        mIndexTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mElectorId = IoUtil.readLong(data, pos);
        pos += 8;
        mCommit = IoUtil.readLong(data, pos);
        pos += 8;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeLong(mCandidateId, data, pos);
        pos += IoUtil.writeLong(mTerm, data, pos);
        pos += IoUtil.writeLong(mIndex, data, pos);
        pos += IoUtil.writeLong(mIndexTerm, data, pos);
        pos += IoUtil.writeLong(mElectorId, data, pos);
        pos += IoUtil.writeLong(mCommit, data, pos);
        return pos;
    }

    public long getCandidateId()
    {
        return mCandidateId;
    }

    public void setCandidateId(long peerId)
    {
        mCandidateId = peerId;
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

    public long getElectorId()
    {
        return mElectorId;
    }

    public void setElectorId(long elector)
    {
        mElectorId = elector;
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
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return String.format("X70_RaftVote{candidate=%#x  term: %d,last: %d@%d, commit=%d, elector=%#x}",
                             mCandidateId,
                             mTerm,
                             mIndex,
                             mIndexTerm,
                             mCommit,
                             mElectorId);
    }
}
