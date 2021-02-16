/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.knight.raft.model.log;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.king.base.util.JsonUtil;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class SnapshotMeta
        extends
        BaseMeta
{
    private final static int _SERIAL = INTERNAL_SERIAL + 3;
    private long             mCommit;
    private long             mTerm;

    @JsonCreator
    public SnapshotMeta(@JsonProperty("term") long term,
                        @JsonProperty("commit") long commit)
    {
        mTerm = term;
        mCommit = commit;
    }

    private SnapshotMeta()
    {
    }

    public static SnapshotMeta loadFromFile(RandomAccessFile file)
    {
        try {
            if (file.length() > 0) {
                file.seek(0);
                int length = file.readInt();
                if (length > 0) {
                    byte[] data = new byte[length];
                    file.read(data);
                    SnapshotMeta snapshotMeta = JsonUtil.readValue(data, SnapshotMeta.class);
                    if (snapshotMeta != null) {
                        snapshotMeta.setFile(file);
                        snapshotMeta.decode(data);
                        return snapshotMeta;
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return new SnapshotMeta().setFile(file);
    }

    @JsonIgnore
    public SnapshotMeta setFile(RandomAccessFile source)
    {
        mFile = source;
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

}
