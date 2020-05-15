/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.bishop.io.zprotocol.raft;

import com.tgx.chess.bishop.io.zprotocol.ZCommand;
import com.tgx.chess.king.base.util.IoUtil;

/**
 * @author william.d.zk
 */
public class X7E_RaftBroadcast
        extends
        ZCommand
{
    public final static int COMMAND = 0x7E;

    public X7E_RaftBroadcast(long msgId)
    {
        super(COMMAND, msgId);
    }

    public X7E_RaftBroadcast()
    {
        super(COMMAND, true);
    }

    //leaderId
    private long mPeerId;
    private long mTerm;
    private long mCommit;
    private long mPreIndex;
    private long mPreIndexTerm;
    private long mFollower;

    @Override
    public int getPriority()
    {
        return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength() + 8 * 6;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mPeerId = IoUtil.readLong(data, pos);
        pos += 8;
        mTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mCommit = IoUtil.readLong(data, pos);
        pos += 8;
        mPreIndex = IoUtil.readLong(data, pos);
        pos += 8;
        mPreIndexTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mFollower = IoUtil.readLong(data, pos);
        pos += 8;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeLong(mPeerId, data, pos);
        pos += IoUtil.writeLong(mTerm, data, pos);
        pos += IoUtil.writeLong(mCommit, data, pos);
        pos += IoUtil.writeLong(mPreIndex, data, pos);
        pos += IoUtil.writeLong(mPreIndexTerm, data, pos);
        pos += IoUtil.writeLong(mFollower, data, pos);
        return pos;
    }

    public long getPeerId()
    {
        return mPeerId;
    }

    public void setPeerId(long peerId)
    {
        this.mPeerId = peerId;
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
        mFollower = follower;
    }

    public long getFollower()
    {
        return mFollower;
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return String.format("X7E_RaftBroadcast{ leader:%#x; %d@%d term:%d, commit:%d payload[%d]",
                             mPeerId,
                             mPreIndex,
                             mPreIndexTerm,
                             mTerm,
                             mCommit,
                             getPayload() == null ? 0
                                                  : getPayload().length);
    }
}
