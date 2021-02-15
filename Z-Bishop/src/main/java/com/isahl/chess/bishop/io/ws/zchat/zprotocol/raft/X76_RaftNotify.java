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

package com.isahl.chess.bishop.io.zprotocol.raft;

import com.isahl.chess.bishop.io.zprotocol.ZCommand;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.inf.IConsistentNotify;

/**
 * @author william.d.zk
 * 
 * @date 2020/4/11
 */
public class X76_RaftNotify
        extends
        ZCommand
        implements
        IConsistentNotify
{
    public final static int COMMAND = 0x76;

    public X76_RaftNotify()
    {
        super(COMMAND, true);
    }

    public X76_RaftNotify(long msgId)
    {
        super(COMMAND, msgId);
    }

    private int               mSerial;
    private long              mOrigin;
    private transient boolean tLeader;
    private transient boolean tNotify;

    public int load()
    {
        return mSerial;
    }

    public void setSerial(int serial)
    {
        mSerial = serial;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeShort(mSerial, data, pos);
        pos += IoUtil.writeLong(mOrigin, data, pos);
        return pos;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mSerial = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        mOrigin = IoUtil.readLong(data, pos);
        pos += 8;
        return pos;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength() + 10;
    }

    @Override
    public long getOrigin()
    {
        return mOrigin;
    }

    public void setOrigin(long origin)
    {
        mOrigin = origin;
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public boolean byLeader()
    {
        return tLeader;
    }

    public void setLeader()
    {
        tLeader = true;
    }

    public void setNotify()
    {
        tNotify = true;
    }

    @Override
    public boolean doNotify()
    {
        return tNotify;
    }
}
