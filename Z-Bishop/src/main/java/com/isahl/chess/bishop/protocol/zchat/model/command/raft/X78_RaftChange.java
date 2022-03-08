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
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.model.ListSerial;
import com.isahl.chess.queen.io.core.features.model.routes.ITraceable;

import java.util.List;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x78)
public class X78_RaftChange
        extends ZCommand
        implements ITraceable
{

    public X78_RaftChange()
    {
        super();
    }

    public X78_RaftChange(long msgId)
    {
        super(msgId);
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
    }

    @Override
    public Level getLevel()
    {
        return Level.ALMOST_ONCE;
    }

    @Override
    public int length()
    {
        return super.length() + 8;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mOrigin);
    }

    private long mOrigin;

    public void setOrigin(long origin)
    {
        mOrigin = origin;
    }

    @Override
    public long origin()
    {
        return mOrigin;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mOrigin = input.getLong();
        return remain - 8;
    }

    public <T extends IoSerial> List<T> getNewGraph(IoFactory<T> factory)
    {
        if(mPayload == null || factory == null) {return null;}
        ListSerial<T> result = new ListSerial<>(factory);
        result.decode(ByteBuf.wrap(mPayload));
        return result;
    }

    @Override
    public String toString()
    {
        return String.format("X78_RaftChange { origin:%#x }", mOrigin);
    }
}
