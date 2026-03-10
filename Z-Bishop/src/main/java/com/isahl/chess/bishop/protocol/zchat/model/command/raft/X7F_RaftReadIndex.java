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
 * Raft ReadIndex 请求
 * 用于线性一致性读
 * 
 * @author william.d.zk
 * @date 2024
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x7F)
public class X7F_RaftReadIndex
        extends ZCommand
        implements IConsistent
{
    private long mClient;
    private long mReadId;      // 读请求唯一标识
    private long mOrigin;      // 关联的设备/客户端origin
    private byte[] mReadData;  // 读请求数据(可选)
    private int  mCode;

    public X7F_RaftReadIndex()
    {
        super();
    }

    public X7F_RaftReadIndex(long msgId)
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

    public long client() {return mClient;}

    public long readId() {return mReadId;}

    public long origin() {return mOrigin;}

    public byte[] readData() {return mReadData;}

    public void client(long client) {mClient = client;}

    public void readId(long readId) {mReadId = readId;}

    public void origin(long origin) {mOrigin = origin;}

    public void readData(byte[] data) {mReadData = data;}

    public void setCode(int code) {mCode = code;}

    @Override
    public int code() {return mCode;}

    @Override
    public int length()
    {
        int dataLen = mReadData != null ? mReadData.length : 0;
        return super.length() + 28 + dataLen; // 8*3 + 4 + dataLen
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mClient = input.getLong();
        mReadId = input.getLong();
        mOrigin = input.getLong();
        int dataLen = input.getInt();
        if (dataLen > 0) {
            mReadData = new byte[dataLen];
            input.get(mReadData);
            remain -= dataLen;
        }
        return remain - 28;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output = super.suffix(output)
                      .putLong(mClient)
                      .putLong(mReadId)
                      .putLong(mOrigin);
        int dataLen = mReadData != null ? mReadData.length : 0;
        output.putInt(dataLen);
        if (dataLen > 0) {
            output.put(mReadData);
        }
        return output;
    }

    @Override
    public String toString()
    {
        int dataLen = mReadData != null ? mReadData.length : 0;
        return String.format("X7F_RaftReadIndex{msgId=%#x, client=%#x, readId=%#x, origin=%#x, dataLen=%d}", 
                             msgId(), mClient, mReadId, mOrigin, dataLen);
    }
}
