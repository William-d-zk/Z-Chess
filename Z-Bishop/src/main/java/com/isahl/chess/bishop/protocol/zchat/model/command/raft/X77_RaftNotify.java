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
import com.isahl.chess.queen.io.core.features.cluster.IConsistency;

import static com.isahl.chess.king.config.KingCode.SUCCESS;

/**
 * @author william.d.zk
 * @date 2020/4/11
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x77)
public class X77_RaftNotify
        extends ZCommand
        implements IConsistency
{

    private long mOrigin;
    private long mClient;

    public X77_RaftNotify()
    {
        super();
    }

    public X77_RaftNotify(long msgId)
    {
        super(msgId);
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
    }

    @Override
    public Level level()
    {
        return Level.ALMOST_ONCE;
    }

    @Override
    public int code()
    {
        return SUCCESS;
    }

    @Override
    public long origin()
    {
        return mOrigin;
    }

    public void origin(long origin)
    {
        mOrigin = origin;
    }

    public long client()
    {
        return mClient;
    }

    public void client(long client)
    {
        mClient = client;
    }

    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mClient)
                    .putLong(mOrigin);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mClient = input.getLong();
        mOrigin = input.getLong();
        return remain - 16;
    }

    @Override
    public int length()
    {
        return super.length() + 16;
    }

    @Override
    public String toString()
    {
        return String.format("X77_RaftNotify { sub:%s from %#x → %#x }", subContent(), mClient, mOrigin);
    }

}
