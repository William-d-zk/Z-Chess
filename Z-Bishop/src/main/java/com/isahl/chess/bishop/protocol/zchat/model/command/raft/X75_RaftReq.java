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
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;

import java.nio.ByteBuffer;

/**
 * @author william.d.zk
 * @date 2020/4/11
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x75)
public class X75_RaftReq
        extends ZCommand
        implements IConsistent
{

    public X75_RaftReq()
    {
        super();
    }

    public X75_RaftReq(long msgId)
    {
        super(msgId);
    }

    private long mClientId;
    private int  mSubSerial;
    private long mOrigin;

    @Override
    public int _sub()
    {
        return mSubSerial;
    }

    public void setSubSerial(int serial)
    {
        mSubSerial = serial;
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
    public void encodec(ByteBuffer output)
    {
        output.putLong(mClientId);
        output.putLong(mOrigin);
        output.putShort((short) mSubSerial);
    }

    @Override
    public void decodec(ByteBuffer input)
    {
        mClientId = input.getLong();
        mOrigin = input.getLong();
        mSubSerial = input.getShort() & 0xFFFF;
    }

    @Override
    public int length()
    {
        return super.length() + 18;
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
    public String toString()
    {
        return String.format(" X75_RaftRequest { client:%#x, origin:%#x,sub-serial:%#x,payload[%d] }",
                             mClientId,
                             mOrigin,
                             mSubSerial,
                             mPayload == null ? 0 : mPayload.length);
    }
}
