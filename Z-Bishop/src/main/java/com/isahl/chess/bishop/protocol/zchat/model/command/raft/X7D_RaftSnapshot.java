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
 * Raft Snapshot 安装请求 (分片传输)
 * 
 * @author william.d.zk
 * @date 2024
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x7D)
public class X7D_RaftSnapshot
        extends ZCommand
        implements IRaftRecord,
                   IConsistent
{
    private long mLeader;
    private long mTerm;
    private long mLastIncludeIndex;
    private long mLastIncludeTerm;
    private long mOffset;          // 分片偏移
    private long mTotalSize;       // 总大小
    private boolean mDone;         // 是否为最后一个分片
    private byte[] mData;          // 分片数据
    private int  mCode;

    public X7D_RaftSnapshot()
    {
        super();
    }

    public X7D_RaftSnapshot(long msgId)
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
    public long peer() {return mLeader;}

    @Override
    public long term() {return mTerm;}

    @Override
    public long index() {return mLastIncludeIndex;}

    @Override
    public long indexTerm() {return mLastIncludeTerm;}

    @Override
    public long commit() {return mLastIncludeIndex;}

    @Override
    public long candidate() {return 0;}

    @Override
    public long accept() {return mLastIncludeIndex;}

    @Override
    public long leader() {return mLeader;}

    public long lastIncludeIndex() {return mLastIncludeIndex;}

    public long lastIncludeTerm() {return mLastIncludeTerm;}

    public long offset() {return mOffset;}

    public long totalSize() {return mTotalSize;}

    public boolean done() {return mDone;}

    public byte[] data() {return mData;}

    public void leader(long leader) {mLeader = leader;}

    public void term(long term) {mTerm = term;}

    public void lastIncludeIndex(long index) {mLastIncludeIndex = index;}

    public void lastIncludeTerm(long term) {mLastIncludeTerm = term;}

    public void offset(long offset) {mOffset = offset;}

    public void totalSize(long size) {mTotalSize = size;}

    public void done(boolean done) {mDone = done;}

    public void data(byte[] data) {mData = data;}

    public void setCode(int code) {mCode = code;}

    @Override
    public int code() {return mCode;}

    @Override
    public int length()
    {
        int dataLen = mData != null ? mData.length : 0;
        return super.length() + 57 + dataLen; // 8*6 + 1 + 4 + dataLen
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mLeader = input.getLong();
        mTerm = input.getLong();
        mLastIncludeIndex = input.getLong();
        mLastIncludeTerm = input.getLong();
        mOffset = input.getLong();
        mTotalSize = input.getLong();
        mDone = input.get() == 1;
        int dataLen = input.getInt();
        if (dataLen > 0) {
            mData = new byte[dataLen];
            input.get(mData);
            remain -= dataLen;
        }
        return remain - 57;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output = super.suffix(output)
                      .putLong(mLeader)
                      .putLong(mTerm)
                      .putLong(mLastIncludeIndex)
                      .putLong(mLastIncludeTerm)
                      .putLong(mOffset)
                      .putLong(mTotalSize)
                      .put((byte) (mDone ? 1 : 0));
        int dataLen = mData != null ? mData.length : 0;
        output.putInt(dataLen);
        if (dataLen > 0) {
            output.put(mData);
        }
        return output;
    }

    @Override
    public String toString()
    {
        int dataLen = mData != null ? mData.length : 0;
        return String.format("X7D_RaftSnapshot{msgId=%#x, leader=%#x, term=%d, lastIndex=%d@%d, offset=%d, total=%d, done=%s, dataLen=%d}", 
                             msgId(), mLeader, mTerm, mLastIncludeIndex, mLastIncludeTerm, mOffset, mTotalSize, mDone, dataLen);
    }
}
