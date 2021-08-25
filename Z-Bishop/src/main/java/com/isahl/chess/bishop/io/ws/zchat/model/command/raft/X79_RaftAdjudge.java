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

package com.isahl.chess.bishop.io.ws.zchat.model.command.raft;

import com.isahl.chess.bishop.io.ws.zchat.model.ZCommand;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;

public class X79_RaftAdjudge
        extends ZCommand
        implements IConsistent
{

    public final static int COMMAND = 0x79;

    public X79_RaftAdjudge()
    {
        super(COMMAND, true);
    }

    public X79_RaftAdjudge(long msgId)
    {
        super(COMMAND, msgId);
    }

    private long mIndex;
    private long mClient;
    private long mOrigin;
    /*
    X79在单节点内部流转，需要将所有信息附加在结构上，便于传递，
    Link & Cluster 之间存在 线程隔离
     */
    private int  mSubSerial;

    @Override
    public String toString()
    {
        return String.format("X79_RaftResp { log-entry-idx:%d, client:%#x,origin:%#x}", mIndex, mClient, mOrigin);
    }

    public void setOrigin(long origin)
    {
        mOrigin = origin;
    }

    @Override
    public long getOrigin()
    {
        return mOrigin;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeLong(mIndex, data, pos);
        pos += IoUtil.writeLong(mClient, data, pos);
        pos += IoUtil.writeLong(mOrigin, data, pos);
        pos += IoUtil.writeShort(mSubSerial, data, pos);
        return pos;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mIndex = IoUtil.readLong(data, pos);
        pos += 8;
        mClient = IoUtil.readLong(data, pos);
        pos += 8;
        mOrigin = IoUtil.readLong(data, pos);
        pos += 8;
        mSubSerial = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        return pos;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength() + 26;
    }

    public long getClient()
    {
        return mClient;
    }

    public void setClient(long client)
    {
        mClient = client;
    }

    public long getIndex()
    {
        return mIndex;
    }

    public void setIndex(long index)
    {
        this.mIndex = index;
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public int subSerial()
    {
        return mSubSerial;
    }

    public void setSubSerial(int subSerial)
    {
        mSubSerial = subSerial;
    }
}
