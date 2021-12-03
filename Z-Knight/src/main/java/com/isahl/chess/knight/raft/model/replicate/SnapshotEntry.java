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
import com.isahl.chess.queen.db.model.IStorage;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.routes.ITraceable;
import com.isahl.chess.queen.message.InnerProtocol;

import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * @author william.d.zk
 * @date 2020/7/13
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = IProtocol.CLUSTER_KNIGHT_RAFT_SERIAL)
public class SnapshotEntry
        extends InnerProtocol
        implements ITraceable,
                   IStorage,
                   Serializable
{
    @Serial
    private static final long serialVersionUID = -8360451804196525728L;

    private long mOrigin;
    private long mIndex;
    private long mTerm;
    private long mCommit;
    private long mApplied;
    private int  mSubSerial;

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
                    long applied)
    {
        super(Operation.OP_INSERT, Strategy.RETAIN);
        mOrigin = origin;
        mIndex = index;
        mTerm = term;
        mCommit = commit;
        mApplied = applied;
    }

    public SnapshotEntry()
    {
        super(Operation.OP_INSERT, Strategy.RETAIN);
    }

    @Override
    public long getOrigin()
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

    @Override
    @JsonInclude
    @JsonProperty("sub")
    public int _sub()
    {
        return mSubSerial;
    }

    @Override
    public ByteBuffer encode()
    {
        ByteBuffer output = super.encode();
        output.putLong(getIndex());
        output.putLong(getTerm());
        output.putLong(getCommit());
        output.putLong(getApplied());
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
        pKey = mIndex = input.getLong();
        mTerm = input.getLong();
        mCommit = input.getLong();
        mApplied = input.getLong();
        mOrigin = input.getLong();
        mSubSerial = input.getShort() & 0xFFFF;
        if(input.hasRemaining()) {
            mPayload = new byte[input.remaining()];
            input.get(mPayload);
        }
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
                             getOrigin(),
                             getIndex(),
                             getTerm(),
                             getCommit(),
                             _sub(),
                             mPayload == null ? 0 : mPayload.length);
    }
}
