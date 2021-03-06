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
 * 
 * @date 2019/12/10
 */
public class X74_RaftReject
        extends
        ZCommand
{
    public final static int COMMAND = 0x74;

    public X74_RaftReject(long msgId)
    {
        super(COMMAND, msgId);
    }

    public X74_RaftReject()
    {
        super(COMMAND, true);
    }

    @Override
    public int getPriority()
    {
        return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength() + 42;
    }

    private long mPeerId;
    private long mTerm;
    private long mIndex;
    private long mIndexTerm;
    private long mReject;
    private int  mCode;
    private int  mState;

    @Override
    public int decodec(byte[] data, int pos)
    {
        mPeerId = IoUtil.readLong(data, pos);
        pos += 8;
        mTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mIndex = IoUtil.readLong(data, pos);
        pos += 8;
        mIndexTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mReject = IoUtil.readLong(data, pos);
        pos += 8;
        mCode = data[pos++];
        mState = data[pos++];
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeLong(mPeerId, data, pos);
        pos += IoUtil.writeLong(mTerm, data, pos);
        pos += IoUtil.writeLong(mIndex, data, pos);
        pos += IoUtil.writeLong(mIndexTerm, data, pos);
        pos += IoUtil.writeLong(mReject, data, pos);
        pos += IoUtil.writeByte(mCode, data, pos);
        pos += IoUtil.writeByte(mState, data, pos);
        return pos;
    }

    public long getPeerId()
    {
        return mPeerId;
    }

    public void setPeerId(long peerId)
    {
        mPeerId = peerId;
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

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return String.format("X74_RaftReject{peerId:%#x,term:%d,current:%d@%d,reject:%#x,code:%d,state:%d}",
                             mPeerId,
                             mTerm,
                             mIndex,
                             mIndexTerm,
                             mReject,
                             mCode,
                             mState);
    }
}
