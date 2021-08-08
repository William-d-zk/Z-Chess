/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.bishop.io.ws.zchat.zprotocol.raft;

import com.isahl.chess.bishop.io.ws.zchat.zprotocol.ZCommand;
import com.isahl.chess.king.base.util.IoUtil;

/**
 * @author william.d.zk
 * @date 2019/12/10
 */
public class X71_RaftBallot
        extends ZCommand
{

    public final static int COMMAND = 0x71;

    public X71_RaftBallot(long msgId)
    {
        super(COMMAND, msgId);
    }

    public X71_RaftBallot()
    {
        super(COMMAND, true);
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
    }

    private long mElectorId;
    private long mTerm;
    private long mIndex;
    private long mIndexTerm;
    private long mCandidateId;
    private long mCommit;

    @Override
    public int dataLength()
    {
        return super.dataLength() + 48;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mElectorId = IoUtil.readLong(data, pos);
        pos += 8;
        mTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mIndex = IoUtil.readLong(data, pos);
        pos += 8;
        mIndexTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mCandidateId = IoUtil.readLong(data, pos);
        pos += 8;
        mCommit = IoUtil.readLong(data, pos);
        pos += 8;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeLong(mElectorId, data, pos);
        pos += IoUtil.writeLong(mTerm, data, pos);
        pos += IoUtil.writeLong(mIndex, data, pos);
        pos += IoUtil.writeLong(mIndexTerm, data, pos);
        pos += IoUtil.writeLong(mCandidateId, data, pos);
        pos += IoUtil.writeLong(mCommit, data, pos);
        return pos;
    }

    public long getElectorId()
    {
        return mElectorId;
    }

    public void setElectorId(long electorId)
    {
        mElectorId = electorId;
    }

    public long getTerm()
    {
        return mTerm;
    }

    public void setTerm(long term)
    {
        mTerm = term;
    }

    public long getCandidateId()
    {
        return mCandidateId;
    }

    public void setCandidateId(long candidate)
    {
        mCandidateId = candidate;
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

    public long getCommit()
    {
        return mCommit;
    }

    public void setCommit(long commit)
    {
        mCommit = commit;
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return String.format("X71_RaftBallot{ elector:%#x,term:%d,last:%d@%d,candidate:%#x,commit:%d}",
                             mElectorId,
                             mTerm,
                             mIndex,
                             mIndexTerm,
                             mCandidateId,
                             mCommit);
    }
}
