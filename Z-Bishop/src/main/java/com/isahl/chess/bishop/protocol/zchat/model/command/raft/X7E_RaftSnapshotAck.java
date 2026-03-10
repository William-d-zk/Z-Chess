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
 * Raft Snapshot 安装响应
 * 
 * @author william.d.zk
 * @date 2024
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x7E)
public class X7E_RaftSnapshotAck
        extends ZCommand
        implements IConsistent
{
    private long mPeer;
    private long mTerm;
    private long mOffset;
    private long mLastIncludeIndex;
    private boolean mSuccess;
    private String mErrorMsg;
    private int    mCode;

    public X7E_RaftSnapshotAck()
    {
        super();
    }

    public X7E_RaftSnapshotAck(long msgId)
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

    public long peer() {return mPeer;}

    public long term() {return mTerm;}

    public long offset() {return mOffset;}

    public long lastIncludeIndex() {return mLastIncludeIndex;}

    public boolean success() {return mSuccess;}

    public String errorMsg() {return mErrorMsg;}

    public void peer(long peer) {mPeer = peer;}

    public void term(long term) {mTerm = term;}

    public void offset(long offset) {mOffset = offset;}

    public void lastIncludeIndex(long index) {mLastIncludeIndex = index;}

    public void success(boolean success) {mSuccess = success;}

    public void errorMsg(String msg) {mErrorMsg = msg;}

    public void setCode(int code) {mCode = code;}

    @Override
    public int code() {return mCode;}

    @Override
    public int length()
    {
        int msgLen = mErrorMsg != null ? mErrorMsg.getBytes().length : 0;
        return super.length() + 37 + msgLen; // 8*4 + 1 + 4 + msgLen
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mPeer = input.getLong();
        mTerm = input.getLong();
        mOffset = input.getLong();
        mLastIncludeIndex = input.getLong();
        mSuccess = input.get() == 1;
        int msgLen = input.getInt();
        if (msgLen > 0) {
            byte[] msgBytes = new byte[msgLen];
            input.get(msgBytes);
            mErrorMsg = new String(msgBytes);
            remain -= msgLen;
        }
        return remain - 37;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output = super.suffix(output)
                      .putLong(mPeer)
                      .putLong(mTerm)
                      .putLong(mOffset)
                      .putLong(mLastIncludeIndex)
                      .put((byte) (mSuccess ? 1 : 0));
        int msgLen = mErrorMsg != null ? mErrorMsg.getBytes().length : 0;
        output.putInt(msgLen);
        if (msgLen > 0) {
            output.put(mErrorMsg.getBytes());
        }
        return output;
    }

    @Override
    public String toString()
    {
        return String.format("X7E_RaftSnapshotAck{msgId=%#x, peer=%#x, term=%d, offset=%d, lastIndex=%d, success=%s, error=%s}", 
                             msgId(), mPeer, mTerm, mOffset, mLastIncludeIndex, mSuccess, mErrorMsg);
    }
}
