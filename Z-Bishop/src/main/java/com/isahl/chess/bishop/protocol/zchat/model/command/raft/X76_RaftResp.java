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
import com.isahl.chess.queen.io.core.features.cluster.IConsistentResult;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x76)
public class X76_RaftResp
        extends ZCommand
        implements IConsistentResult
{
    public X76_RaftResp()
    {
        super();
    }

    public X76_RaftResp(long msgId)
    {
        super(msgId);
    }

    private long mClient;//8 byte
    private int  mCode;//4 byte
    private int  mState;//1 byte

    private long mOrigin;//8byte

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
    public String toString()
    {
        return String.format("X76_RaftResp { to-client[%#x],%d}", mClient, mState);
    }

    public void code(int code)
    {
        mCode = code;
    }

    @Override
    public int code()
    {
        return mCode;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mClient)
                    .put(mState)
                    .putInt(mCode)
                    .putLong(mOrigin);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mClient = input.getLong();
        mState = input.get();
        mCode = input.getInt();
        mOrigin = input.getLong();
        return remain - 21;
    }

    @Override
    public int length()
    {
        return super.length() + 21;
    }

    public long client()
    {
        return mClient;
    }

    public void client(long client)
    {
        mClient = client;
    }

    public void origin(long origin)
    {
        mOrigin = origin;
    }

    public long origin()
    {
        return mOrigin;
    }

}
