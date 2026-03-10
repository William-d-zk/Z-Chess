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
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;

import static com.isahl.chess.king.config.KingCode.SUCCESS;

/**
 * Leader 转让请求
 * 
 * 用于主动将 Leadership 转让给其他节点
 * 
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x81)
public class X81_RaftTransferLeadership
        extends ZCommand
        implements IConsistent
{

    public X81_RaftTransferLeadership()
    {
        super();
    }

    public X81_RaftTransferLeadership(long msgId)
    {
        super(msgId);
    }

    /**
     * 转让目标节点ID
     */
    private long mTargetPeer;
    
    /**
     * 当前 Leader 的 commit index
     */
    private long mLeaderCommit;
    
    /**
     * 当前 Leader 的 log index
     */
    private long mLeaderIndex;
    
    /**
     * 当前 term
     */
    private long mTerm;
    
    /**
     * 转让超时时间（毫秒）
     */
    private long mTimeoutMs;

    @Override
    public int priority()
    {
        return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
    }

    @Override
    public Level level()
    {
        return Level.AT_LEAST_ONCE;
    }

    @Override
    public int length()
    {
        return super.length() + 8 + 8 + 8 + 8 + 8;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mTargetPeer)
                    .putLong(mLeaderCommit)
                    .putLong(mLeaderIndex)
                    .putLong(mTerm)
                    .putLong(mTimeoutMs);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mTargetPeer = input.getLong();
        remain -= 8;
        mLeaderCommit = input.getLong();
        remain -= 8;
        mLeaderIndex = input.getLong();
        remain -= 8;
        mTerm = input.getLong();
        remain -= 8;
        mTimeoutMs = input.getLong();
        remain -= 8;
        return remain;
    }

    @Override
    public String toString()
    {
        return String.format("X81_RaftTransferLeadership { target:%#x, commit:%d, index:%d, term:%d, timeout:%dms }",
                             mTargetPeer, mLeaderCommit, mLeaderIndex, mTerm, mTimeoutMs);
    }

    @Override
    public int code()
    {
        return SUCCESS;
    }

    public long getTargetPeer()
    {
        return mTargetPeer;
    }

    public void setTargetPeer(long targetPeer)
    {
        mTargetPeer = targetPeer;
    }

    public long getLeaderCommit()
    {
        return mLeaderCommit;
    }

    public void setLeaderCommit(long leaderCommit)
    {
        mLeaderCommit = leaderCommit;
    }

    public long getLeaderIndex()
    {
        return mLeaderIndex;
    }

    public void setLeaderIndex(long leaderIndex)
    {
        mLeaderIndex = leaderIndex;
    }

    public long getTerm()
    {
        return mTerm;
    }

    public void setTerm(long term)
    {
        mTerm = term;
    }

    public long getTimeoutMs()
    {
        return mTimeoutMs;
    }

    public void setTimeoutMs(long timeoutMs)
    {
        mTimeoutMs = timeoutMs;
    }
}
