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
public class X7F_RaftResponse
        extends
        ZCommand
{
    public final static int COMMAND = 0x7F;

    public X7F_RaftResponse()
    {
        super(COMMAND, true);
    }

    public X7F_RaftResponse(long msgId)
    {
        super(COMMAND, msgId);
    }

    private long mPeerId;
    private long mTerm;
    private long mCandidate;
    private long mCatchUp;
    private int  mCode;
    private int  mState;

    @Override
    public int dataLength()
    {
        return super.dataLength() + 34;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mPeerId = IoUtil.readLong(data, pos);
        pos += 8;
        mTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mCandidate = IoUtil.readLong(data, pos);
        pos += 8;
        mCatchUp = IoUtil.readLong(data, pos);
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
        pos += IoUtil.writeLong(mCandidate, data, pos);
        pos += IoUtil.writeLong(mCatchUp, data, pos);
        pos += IoUtil.writeByte(mCode, data, pos);
        pos += IoUtil.writeByte(mState, data, pos);
        return pos;
    }

    public long getTerm()
    {
        return mTerm;
    }

    public void setTerm(long term)
    {
        mTerm = term;
    }

    public int getCode()
    {
        return mCode;
    }

    public void setCode(int code)
    {
        mCode = code;
    }

    public long getCatchUp()
    {
        return mCatchUp;
    }

    public void setCatchUp(long catchUp)
    {
        mCatchUp = catchUp;
    }

    public long getPeerId()
    {
        return mPeerId;
    }

    public void setPeerId(long peerId)
    {
        mPeerId = peerId;
    }

    public int getState()
    {
        return mState;
    }

    public void setState(int state)
    {
        mState = state;
    }

    public void setCandidate(long candidate)
    {
        mCandidate = candidate;
    }

    public long getCandidate()
    {
        return mCandidate;
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return "X7F_RaftResponse{"
               + "mPeerId="
               + mPeerId
               + ", mTerm="
               + mTerm
               + ", mCandidate="
               + mCandidate
               + ", mCatchUp="
               + mCatchUp
               + ", mCode="
               + mCode
               + ", mState="
               + mState
               + '}';
    }
}
