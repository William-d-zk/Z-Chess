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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tgx.chess.cluster.raft.model.RaftGraph;
import com.tgx.chess.json.JsonUtil;
import com.tgx.chess.queen.io.core.inf.IProtocol;

import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class LogEntry
        implements
        IProtocol
{
    private final static int _SERIAL = INTERNAL_SERIAL + 2;

    private long      term;
    private long      index;
    private RaftGraph raftGraph;
    @JsonIgnore
    private int       length;

    @Override
    public int dataLength()
    {
        return length;
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

    public long getTerm()
    {
        return term;
    }

    public void setTerm(long term)
    {
        this.term = term;
    }

    public long getIndex()
    {
        return index;
    }

    public void setIndex(long index)
    {
        this.index = index;
    }

    public RaftGraph getRaftGraph()
    {
        return raftGraph;
    }

    public void setRaftGraph(RaftGraph raftGraph)
    {
        this.raftGraph = raftGraph;
    }

    @Override
    public int decode(byte[] data)
    {
        LogEntry json = JsonUtil.readValue(data, getClass());
        Objects.requireNonNull(json);
        index = json.getIndex();
        term = json.getTerm();
        raftGraph = json.getRaftGraph();
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
