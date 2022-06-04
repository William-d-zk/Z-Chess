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
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;

import java.util.Arrays;
import java.util.List;

import static com.isahl.chess.king.config.KingCode.SUCCESS;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x78)
public class X78_RaftModify
        extends ZCommand
        implements IConsistent
{

    public X78_RaftModify()
    {
        super();
    }

    public X78_RaftModify(long msgId)
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
        return super.length() + 16 + 4 + mCount * 8;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mClient)
                    .putLong(mLeader)
                    .putInt(mCount)
                    .putLongArray(mPeers);
    }

    private long   mClient;
    private long   mLeader;
    private int    mCount;
    private long[] mPeers;

    @Override
    public X78_RaftModify copy()
    {
        X78_RaftModify n = new X78_RaftModify();
        n.mCount = mCount;
        n.mLeader = mLeader;
        n.mClient = mClient;
        n.mPeers = Arrays.copyOf(mPeers, mPeers.length);
        return n;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mClient = input.getLong();
        mLeader = input.getLong();
        remain -= 16;
        mCount = input.getInt();
        remain -= 4;
        mPeers = new long[mCount];
        for(int i = 0; i < mCount; i++) {
            mPeers[i] = input.getLong();
        }
        return remain - 8 * mCount;
    }

    public long getLeader()
    {
        return mLeader;
    }

    public long[] getNewGraph()
    {
        return mPeers;
    }

    public void newGraph(long leader, List<Long> peers)
    {
        mLeader = leader;
        mCount = peers.size();
        mPeers = new long[mCount];
        Arrays.setAll(mPeers, peers::get);
    }

    public long getClient()
    {
        return mClient;
    }

    public void setClient(long client)
    {
        mClient = client;
    }

    @Override
    public String toString()
    {
        return String.format("X78_RaftModify { from:[%#x] to:[%#x] peers-%d:%s }",
                             mClient,
                             mLeader,
                             mCount,
                             IoUtil.longArrayToHex(mPeers));
    }

    @Override
    public int getCode()
    {
        return SUCCESS;
    }

}
