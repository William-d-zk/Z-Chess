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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.routes.ITraceable;
import com.isahl.chess.queen.message.InnerProtocol;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = IProtocol.CLUSTER_KNIGHT_RAFT_SERIAL)
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
            @JsonProperty("payload")
                    byte[] payload)
    {
        super(Operation.OP_INSERT, Strategy.RETAIN);
        mTerm = term;
        pKey = mIndex = index;
        mClient = client;
        mOrigin = origin;
        mPayload = payload;
    }

    public LogEntry()
    {
        super();
    }

    public LogEntry(ByteBuf input)
    {
        super(input);
    }

    public long getTerm()
    {
        return mTerm;
    }

    public long getIndex()
    {
        return mIndex;
    }

    @Override
    public int length()
    {
        return 8 + //term
               8 + //client
               8 +  //origin
               super.length(); //pKey == index
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(getTerm())
                    .putLong(getClient())
                    .putLong(origin());
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mIndex = pKey;
        mTerm = input.getLong();
        mClient = input.getLong();
        mOrigin = input.getLong();
        return remain - 24;
    }

    public long getClient()
    {
        return mClient;
    }

    @Override
    public long origin()
    {
        return mOrigin;
    }

    @Override
    public String toString()
    {
        return String.format("raft_log{ id: %d [%d], raft-client:%#x, biz-session:%#x, sub-size:%d }",
                             getIndex(),
                             getTerm(),
                             getClient(),
                             origin(),
                             mPayload == null ? 0 : mPayload.length);
    }

}
