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
 * @date 2020/4/11
 */
public class X75_RaftRequest
        extends
        ZCommand
{
    public final static int COMMAND = 0x75;

    public X75_RaftRequest()
    {
        super(COMMAND, true);
    }

    public X75_RaftRequest(long msgId)
    {
        super(COMMAND, msgId);
    }

    private long    mPeerId;
    private int     mPayloadSerial;
    private long    mOrigin;
    private boolean mBroadcast;

    public int getPayloadSerial()
    {
        return mPayloadSerial;
    }

    public void setPayloadSerial(int serial)
    {
        mPayloadSerial = serial;
    }

    public void setOrigin(long origin)
    {
        mOrigin = origin;

    }

    public long getOrigin()
    {
        return mOrigin;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeShort(mPayloadSerial, data, pos);
        pos += IoUtil.writeLong(mOrigin, data, pos);
        pos += IoUtil.writeLong(mPeerId, data, pos);
        return pos;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mPayloadSerial = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        mOrigin = IoUtil.readLong(data, pos);
        pos += 8;
        mPeerId = IoUtil.readLong(data, pos);
        pos += 8;
        return pos;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength() + 18;
    }

    public long getPeerId()
    {
        return mPeerId;
    }

    public void setPeerId(long peerId)
    {
        mPeerId = peerId;
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    public boolean isBroadcast()
    {
        return mBroadcast;
    }

    public void setBroadcast(boolean broadcast)
    {
        mBroadcast = broadcast;
    }
}
