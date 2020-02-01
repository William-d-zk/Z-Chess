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

package com.tgx.chess.cluster.raft.log;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgx.chess.queen.io.core.inf.IProtocol;

public class LogMeta
        implements
        IProtocol
{
    private final static int       _SERIAL     = INTERNAL_SERIAL + 1;
    private final RandomAccessFile _File;
    private long                   firstLogIndex;
    private long                   term;
    private long                   candidate;
    private int                    length;
    private final ObjectMapper     _JsonMapper = new ObjectMapper();

    LogMeta(RandomAccessFile file)
    {
        _File = file;
    }

    LogMeta load()
    {
        try {
            if (_File.length() == 0) { return this; }
            _File.seek(0);
            length = _File.readInt();
            if (length > 0) {
                byte[] data = new byte[length];
                _File.read(data);
                decode(data);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    void update()
    {
        try {
            _File.seek(0);
            _File.writeInt(dataLength());
            _File.write(encode());
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

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
