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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.RandomAccessFile;

public class LogMeta
        extends
        BaseMeta
{
    private final static int _SERIAL = INTERNAL_SERIAL + 1;

    private long firstLogIndex;
    private long term;
    private long candidate;

    LogMeta(RandomAccessFile file)
    {
        super((file));
    }

    LogMeta load()
    {
        loadFromFile();
        return this;
    }

    @Override
    public int decode(byte[] data)
    {
        try {
            JsonNode json = _JsonMapper.readTree(data);
            firstLogIndex = json.get("first_log_index")
                                .asLong();
            term = json.get("term")
                       .asLong();
            candidate = json.get("candidate")
                            .asLong();

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return length = data.length;
    }

    @Override
    public byte[] encode()
    {
        try {
            byte[] data = _JsonMapper.writeValueAsBytes(this);
            length = data.length;
            return data;
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
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

    public long getFirstLogIndex()
    {
        return firstLogIndex;
    }

    public void setFirstLogIndex(long firstLogIndex)
    {
        this.firstLogIndex = firstLogIndex;
    }

    public long getTerm()
    {
        return term;
    }

    public void setTerm(long term)
    {
        this.term = term;
    }

    public long getCandidate()
    {
        return candidate;
    }

    public void setCandidate(long candidate)
    {
        this.candidate = candidate;
    }
}
