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
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.routes.ITraceable;
import com.isahl.chess.queen.message.InnerProtocol;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author william.d.zk
 * @date 2020/7/13
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = IProtocol.CLUSTER_KNIGHT_RAFT_SERIAL)
public class SnapshotEntry
        extends InnerProtocol
        implements ITraceable,
                   Serializable
{
    @Serial
    private static final long serialVersionUID = -8360451804196525728L;

    private long mOrigin;
    private long mIndex;
    private long mTerm;
    private long mCommit;
    private long mApplied;

    @Override
    public int length()
    {
        int length = 8 + // index
                     8 + // term
                     8 + // origin
                     8 + // commit
                     8;  // applied

        return length + super.length();
    }

    @JsonCreator
    public SnapshotEntry(
            @JsonProperty("origin")
                    long origin,
            @JsonProperty("index")
                    long index,
            @JsonProperty("term")
                    long term,
            @JsonProperty("commit")
                    long commit,
            @JsonProperty("applied")
                    long applied,
            @JsonProperty("payload")
                    byte[] payload)
    {
        super(Operation.OP_INSERT, Strategy.RETAIN);
        mOrigin = origin;
        mIndex = index;
        mTerm = term;
        mCommit = commit;
        mApplied = applied;
        mPayload = payload;
    }

    public SnapshotEntry()
    {
        super();
    }

    @Override
    public long origin()
    {
        return mOrigin;
    }

    public long getIndex()
    {
        return mIndex;
    }

    public long getTerm()
    {
        return mTerm;
    }

    public long getCommit()
    {
        return mCommit;
    }

    public long getApplied()
    {
        return mApplied;
    }

    public byte[] getPayload()
    {
        return payload();
    }

    @Override
    public int _sub()
    {
        return mPayload == null || mPayload.length < 2 ? -1 : IoUtil.readUnsignedShort(mPayload, 0);
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output = super.suffix(output)
                      .putLong(getIndex())
                      .putLong(getTerm())
                      .putLong(getCommit())
                      .putLong(getApplied())
                      .putLong(origin());
        return output;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mIndex = input.getLong();
        mTerm = input.getLong();
        mCommit = input.getLong();
        mApplied = input.getLong();
        mOrigin = input.getLong();
        return remain - 40;
    }

    @Override
    public String toString()
    {
        return String.format("""
                                     snapshot entry @ %#x {
                                     \t\torigin[%#x],
                                     \t\tindex[%d],
                                     \t\tterm[%d],
                                     \t\tcommit[%d],
                                     \t\tsub[%#x],
                                     \t\tsub-payload(%d)
                                     }""",
                             serial(),
                             origin(),
                             getIndex(),
                             getTerm(),
                             getCommit(),
                             _sub(),
                             mPayload == null ? 0 : mPayload.length);
    }
}
