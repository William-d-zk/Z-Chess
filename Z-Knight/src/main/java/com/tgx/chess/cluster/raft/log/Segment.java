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
import java.util.ArrayList;
import java.util.List;

public class Segment
{
    public static class Record
    {
        private final long     _Offset;
        private final LogEntry _Entry;

        public Record(long offset,
                      LogEntry entry)
        {
            _Offset = offset;
            _Entry = entry;
        }

        public long getOffset()
        {
            return _Offset;
        }

        public LogEntry getEntry()
        {
            return _Entry;
        }
    }

    private final String           _FileName;
    private final RandomAccessFile _RandomAccessFile;
    private final long             _StartIndex;
    private final long             _EndIndex;
    private final long             _FileSize;
    private boolean                canWrite;
    private List<Record>           entries = new ArrayList<>();

    public LogEntry getEntry(long index)
    {
        if (_StartIndex == 0 || _EndIndex == 0) { return null; }
        if (index < _StartIndex || index > _EndIndex) { return null; }
        int indexInList = (int) (index - _StartIndex);
        return entries.get(indexInList)
                      .getEntry();
    }

    public Segment(String fileName,
                   long startIndex,
                   long endIndex,
                   boolean canWrite) throws IOException
    {
        _FileName = fileName;
        this.canWrite = canWrite;
        _RandomAccessFile = new RandomAccessFile(_FileName,
                                                 isCanWrite() ? "rw"
                                                              : "r");
        _StartIndex = startIndex;
        _EndIndex = endIndex;
        _FileSize = _RandomAccessFile.length();
    }

    public String getFileName()
    {
        return _FileName;
    }

    public RandomAccessFile getRandomAccessFile()
    {
        return _RandomAccessFile;
    }

    public long getStartIndex()
    {
        return _StartIndex;
    }

    public long getEndIndex()
    {
        return _EndIndex;
    }

    public long getFileSize()
    {
        return _FileSize;
    }

    public boolean isCanWrite()
    {
        return canWrite;
    }

    public List<Record> getEntries()
    {
        return entries;
    }
}
