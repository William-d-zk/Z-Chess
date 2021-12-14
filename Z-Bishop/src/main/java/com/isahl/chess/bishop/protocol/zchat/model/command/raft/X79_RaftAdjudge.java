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

import com.isahl.chess.bishop.protocol.zchat.model.base.ZFrame;
import com.isahl.chess.bishop.protocol.zchat.model.command.ZCommand;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x79)
public class X79_RaftAdjudge
        extends ZCommand
        implements IConsistent
{

    public X79_RaftAdjudge()
    {
        super();
        mFrameHeader |= ZFrame.frame_op_code_ctrl;
    }

    public X79_RaftAdjudge(long msgId)
    {
        super(msgId);
        mFrameHeader |= ZFrame.frame_op_code_ctrl;
    }

    private long mIndex;
    private long mClient;
    private long mOrigin;

    @Override
    public int priority()
    {
        return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
    }

    @Override
    public Level getLevel()
    {
        return Level.AT_LEAST_ONCE;
    }

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
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mIndex)
                    .putLong(mClient)
                    .putLong(mOrigin);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mIndex = input.getLong();
        mClient = input.getLong();
        mOrigin = input.getLong();
        return remain - 24;
    }

    @Override
    public int length()
    {
        return super.length() + 24;
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

}
