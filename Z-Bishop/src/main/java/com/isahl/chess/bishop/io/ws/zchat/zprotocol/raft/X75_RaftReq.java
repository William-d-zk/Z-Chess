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
import com.isahl.chess.queen.io.core.inf.IConsistent;

/**
 * @author william.d.zk
 * 
 * @date 2020/4/11
 */
public class X75_RaftReq
        extends
        ZCommand
        implements
        IConsistent
{
    public final static int COMMAND = 0x75;

    public X75_RaftReq()
    {
        super(COMMAND, true);
    }

    public X75_RaftReq(long msgId)
    {
        super(COMMAND, msgId);
    }

    private long    mClientId;
    private int     mPayloadSerial;
    private long    mOrigin;
    private boolean mPublic;

    public int getPayloadSerial()
    {
        return mPayloadSerial;
    }

    public void setPayloadSerial(int payloadSerial)
    {
        mPayloadSerial = payloadSerial;
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
        pos += IoUtil.writeLong(mClientId, data, pos);
        pos += IoUtil.writeLong(mOrigin, data, pos);
        pos += IoUtil.writeByte(mPublic ? 1: 0, data, pos);
        pos += IoUtil.writeShort(mPayloadSerial, data, pos);
        return pos;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mClientId = IoUtil.readLong(data, pos);
        pos += 8;
        mOrigin = IoUtil.readLong(data, pos);
        pos += 8;
        mPublic = data[pos++] > 0;
        mPayloadSerial = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        return pos;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength() + 19;
    }

    public long getClientId()
    {
        return mClientId;
    }

    public void setClientId(long clientId)
    {
        mClientId = clientId;
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public boolean isPublic()
    {
        return mPublic;
    }

    public void setPublic(boolean pub)
    {
        mPublic = pub;
    }

    @Override
    public String toString()
    {
        return String.format(" X75_RaftRequest { client:%#x, origin:%#x, serial:%#x,payload[%d] public:%s }",
                             mClientId,
                             mOrigin,
                             mPayloadSerial,
                             getPayload() == null ? 0: getPayload().length,
                             mPublic ? "all": "one");
    }
}
