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

import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.routes.ITraceable;
import com.isahl.chess.queen.message.InnerProtocol;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author william.d.zk
 * @date 2020/7/13
 */
@ISerialGenerator(parent = IProtocol.CLUSTER_KNIGHT_RAFT_SERIAL)
public class SnapshotEntry
        extends InnerProtocol
        implements ITraceable,
                   Serializable
{
    @Serial
    private static final long serialVersionUID = -8360451804196525728L;

    private long mOrigin;
    private long mEnd;
    private long mTerm;
    private long mCommit;
    private long mAccept;

    @Override
    public int length()
    {
        return 8 + // origin
               8 + // end
               8 + // term
               8 + // commit
               8 + // accept
               super.length();
    }

    public SnapshotEntry(long origin, long start, long end, long term, long commit, long accept, byte[] content)
    {
        super(Operation.OP_INSERT, Strategy.RETAIN);
        mOrigin = origin;
        pKey = start;
        mEnd = end;
        mTerm = term;
        mCommit = commit;
        mAccept = accept;
        withSub(content);
    }

    public SnapshotEntry(ByteBuf input)
    {
        super(input);
        buildSub();
    }

    @Override
    public long origin() {return mOrigin;}

    public long start() {return primaryKey();}

    public long end() {return mEnd;}

    public long term() {return mTerm;}

    public long commit() {return mCommit;}

    public long accept() {return mAccept;}

    public byte[] content() {return payload();}

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(end())
                    .putLong(term())
                    .putLong(commit())
                    .putLong(accept())
                    .putLong(origin());
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mEnd = input.getLong();
        mTerm = input.getLong();
        mCommit = input.getLong();
        mAccept = input.getLong();
        mOrigin = input.getLong();
        return remain - 40;
    }

    @Override
    public String toString()
    {
        return String.format("""
                                     snapshot entry @ %#x {
                                     \t\torigin[%#x],
                                     \t\tstart[%d],
                                     \t\tend[%d],
                                     \t\tterm[%d],
                                     \t\tcommit[%d],
                                     \t\tsub[%#x](%d),
                                     }""", serial(), origin(), start(), end(), term(), commit(), _sub(), pLength());
    }
}
