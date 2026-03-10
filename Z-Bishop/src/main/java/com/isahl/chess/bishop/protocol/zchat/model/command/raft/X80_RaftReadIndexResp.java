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
 * Raft ReadIndex 响应
 * 
 * @author william.d.zk
 * @date 2024
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x80)
public class X80_RaftReadIndexResp
        extends ZCommand
        implements IConsistent
{
    private long mPeer;
    private long mReadId;
    private long mCommitIndex;   // Leader 当前的 commit index
    private boolean mSuccess;
    private String mErrorMsg;
    private byte[] mResult;      // 读结果数据
    private int    mCode;

    public X80_RaftReadIndexResp()
    {
        super();
    }

    public X80_RaftReadIndexResp(long msgId)
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

    public long readId() {return mReadId;}

    public long commitIndex() {return mCommitIndex;}

    public boolean success() {return mSuccess;}

    public String errorMsg() {return mErrorMsg;}

    public byte[] result() {return mResult;}

    public void peer(long peer) {mPeer = peer;}

    public void readId(long readId) {mReadId = readId;}

    public void commitIndex(long index) {mCommitIndex = index;}

    public void success(boolean success) {mSuccess = success;}

    public void errorMsg(String msg) {mErrorMsg = msg;}

    public void result(byte[] result) {mResult = result;}

    public void setCode(int code) {mCode = code;}

    @Override
    public int code() {return mCode;}

    @Override
    public int length()
    {
        int msgLen = mErrorMsg != null ? mErrorMsg.getBytes().length : 0;
        int resultLen = mResult != null ? mResult.length : 0;
        return super.length() + 33 + msgLen + resultLen; // 8*3 + 1 + 4 + msgLen + 4 + resultLen
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mPeer = input.getLong();
        mReadId = input.getLong();
        mCommitIndex = input.getLong();
        mSuccess = input.get() == 1;
        int msgLen = input.getInt();
        if (msgLen > 0) {
            byte[] msgBytes = new byte[msgLen];
            input.get(msgBytes);
            mErrorMsg = new String(msgBytes);
            remain -= msgLen;
        }
        int resultLen = input.getInt();
        if (resultLen > 0) {
            mResult = new byte[resultLen];
            input.get(mResult);
            remain -= resultLen;
        }
        return remain - 33;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output = super.suffix(output)
                      .putLong(mPeer)
                      .putLong(mReadId)
                      .putLong(mCommitIndex)
                      .put((byte) (mSuccess ? 1 : 0));
        int msgLen = mErrorMsg != null ? mErrorMsg.getBytes().length : 0;
        output.putInt(msgLen);
        if (msgLen > 0) {
            output.put(mErrorMsg.getBytes());
        }
        int resultLen = mResult != null ? mResult.length : 0;
        output.putInt(resultLen);
        if (resultLen > 0) {
            output.put(mResult);
        }
        return output;
    }

    @Override
    public String toString()
    {
        int resultLen = mResult != null ? mResult.length : 0;
        return String.format("X80_RaftReadIndexResp{msgId=%#x, peer=%#x, readId=%#x, commitIndex=%d, success=%s, resultLen=%d}", 
                             msgId(), mPeer, mReadId, mCommitIndex, mSuccess, resultLen);
    }
}
