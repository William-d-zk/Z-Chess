/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
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
import com.isahl.chess.queen.io.core.features.model.session.IQoS.Level;

import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.AT_LEAST_ONCE;

/**
 * Raft Pre-vote 请求
 * 用于选举前的探测，避免网络分区节点干扰集群
 * 
 * @author william.d.zk
 * @date 2024
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x6F)
public class X6F_RaftPreVote
        extends ZCommand
        implements IRaftRecord,
                   IConsistent
{
    private long mPeer;
    private long mTerm;
    private long mIndex;
    private long mIndexTerm;
    private long mCommit;
    private int  mCode;

    public X6F_RaftPreVote()
    {
        super();
    }

    public X6F_RaftPreVote(long msgId)
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
        return AT_LEAST_ONCE;
    }

    @Override
    public long peer() {return mPeer;}

    @Override
    public long term() {return mTerm;}

    @Override
    public long index() {return mIndex;}

    @Override
    public long indexTerm() {return mIndexTerm;}

    @Override
    public long commit() {return mCommit;}

    @Override
    public long candidate() {return mPeer;}

    @Override
    public long accept() {return mIndex;}

    @Override
    public long leader() {return 0;}

    public void peer(long peer) {mPeer = peer;}

    public void term(long term) {mTerm = term;}

    public void index(long index) {mIndex = index;}

    public void indexTerm(long indexTerm) {mIndexTerm = indexTerm;}

    public void commit(long commit) {mCommit = commit;}

    public void setCode(int code) {mCode = code;}

    @Override
    public int code() {return mCode;}

    @Override
    public int length()
    {
        return super.length() + 40; // 5 * 8
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mPeer = input.getLong();
        mTerm = input.getLong();
        mIndex = input.getLong();
        mIndexTerm = input.getLong();
        mCommit = input.getLong();
        return remain - 40;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mPeer)
                    .putLong(mTerm)
                    .putLong(mIndex)
                    .putLong(mIndexTerm)
                    .putLong(mCommit);
    }

    @Override
    public String toString()
    {
        return String.format("X6F_RaftPreVote{msgId=%#x, peer=%#x, term=%d, index=%d@%d, commit=%d}", 
                             msgId(), mPeer, mTerm, mIndex, mIndexTerm, mCommit);
    }
}
