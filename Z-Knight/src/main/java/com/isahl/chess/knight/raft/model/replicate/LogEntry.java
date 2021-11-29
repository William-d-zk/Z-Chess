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

package com.isahl.chess.knight.raft.model.replicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.routes.ITraceable;
import com.isahl.chess.queen.message.InnerProtocol;

import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = IProtocol.CLUSTER_KNIGHT_CONSISTENT_SERIAL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LogEntry
        extends InnerProtocol
        implements ITraceable,
                   Serializable
{
    @Serial
    private final static long serialVersionUID = -3944638115324945253L;

    private long mTerm;
    private long mIndex;
    /*
    纪录哪个Raft-Client发起的当前这次一致性记录
     */
    private long mClient;
    /*
    完成一致性记录后，成功的消息反馈给哪个请求人
    在Z-Chat体系中表达了device 终端，
    web api 中表达了某次请求的session
     */
    private long mOrigin;
    private int  mSubSerial;

    @JsonCreator
    public LogEntry(
            @JsonProperty("term")
                    long term,
            @JsonProperty("index")
                    long index,
            @JsonProperty("client")
                    long client,
            @JsonProperty("origin")
                    long origin,
            @JsonProperty("sub")
                    int sub,
            @JsonProperty("content")
                    byte[] content)
    {
        super(Operation.OP_INSERT, Strategy.RETAIN);
        mTerm = term;
        mIndex = index;
        mClient = client;
        mOrigin = origin;
        mSubSerial = sub;
        put(content);
        pKey = mIndex;
    }

    public LogEntry()
    {
        super();
    }

    public long getTerm()
    {
        return mTerm;
    }

    public long getIndex()
    {
        return mIndex;
    }

    public byte[] getContent()
    {
        return mPayload;
    }

    @Override
    public int length()
    {
        int length = 8 + //term
                     8 + //index
                     8 + //client
                     8 + //origin
                     2;  //sub
        return length + super.length();
    }

    public static Factory<LogEntry> _Factory = serial->{
        LogEntry entry = new LogEntry();
        if(serial == entry.serial()) {return entry;}
        return null;
    };

    @Override
    public ByteBuffer encode()
    {
        ByteBuffer output = super.encode();
        output.putLong(getTerm());
        output.putLong(getIndex());
        output.putLong(getClient());
        output.putLong(getOrigin());
        output.putShort((short) _sub());
        if(mPayload != null) {
            output.put(mPayload);
        }
        return output;
    }

    @Override
    public void decode(ByteBuffer input)
    {
        super.decode(input);
        mTerm = input.getLong();
        pKey = mIndex = input.getLong();
        mClient = input.getLong();
        mOrigin = input.getLong();
        mSubSerial = input.getShort() & 0xFFFF;
        if(input.hasRemaining()) {
            mPayload = new byte[input.remaining()];
            input.get(mPayload);
        }
    }

    @Override
    @JsonInclude
    @JsonProperty("sub")
    public int _sub()
    {
        return mSubSerial;
    }

    public long getClient()
    {
        return mClient;
    }

    @Override
    public long getOrigin()
    {
        return mOrigin;
    }

    @Override
    public String toString()
    {
        return String.format("raft_log{ id: %d [%d], raft-client:%#x, biz-session:%#x, sub-serial:%#x, sub-size:%d }",
                             mIndex,
                             mTerm,
                             mClient,
                             mOrigin,
                             mSubSerial,
                             mPayload == null ? 0 : mPayload.length);
    }

}
