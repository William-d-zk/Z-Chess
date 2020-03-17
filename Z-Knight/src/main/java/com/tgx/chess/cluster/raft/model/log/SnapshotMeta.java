/*
 * MIT License
 *
 * Copyright (c) 2016~2020 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.cluster.raft.model.log;

import java.io.RandomAccessFile;
import java.util.Objects;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tgx.chess.json.JsonUtil;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class SnapshotMeta
        extends
        BaseMeta
{
    private final static int _SERIAL = INTERNAL_SERIAL + 3;
    private long             mCommit;
    private long             mTerm;

    SnapshotMeta()
    {
        super(null);
    }

    public SnapshotMeta(RandomAccessFile file)
    {
        super(file);
    }

    public SnapshotMeta load()
    {
        loadFromFile();
        return this;
    }

    @Override
    public int serial()
    {
        return _SERIAL;
    }

    @Override
    public int superSerial()
    {
        return INTERNAL_SERIAL;
    }

    public void setCommit(long commit)
    {
        this.mCommit = commit;
    }

    public void setTerm(long term)
    {
        this.mTerm = term;
    }

    public long getCommit()
    {
        return mCommit;
    }

    public long getTerm()
    {
        return mTerm;
    }

    @Override
    public int decode(byte[] data)
    {
        SnapshotMeta json = JsonUtil.readValue(data, getClass());
        Objects.requireNonNull(json);
        mCommit = json.getCommit();
        mTerm = json.getTerm();
        return length = data.length;
    }

    @Override
    public byte[] encode()
    {
        byte[] data = JsonUtil.writeValue(this);
        Objects.requireNonNull(data);
        length = data.length;
        return data;
    }
}
