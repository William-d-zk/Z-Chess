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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.tgx.chess.king.base.exception.ZException;
import com.tgx.chess.king.base.log.Logger;

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

    private final static Logger _Logger = Logger.getLogger(Segment.class.getName());
    private final long          _StartIndex;
    private final String        _FileDirectory;
    private RandomAccessFile    randomAccessFile;
    private String              fileName;
    private long                endIndex;
    private long                fileSize;
    private boolean             canWrite;
    private List<Record>        records = new ArrayList<>();

    public LogEntry getEntry(long index)
    {
        if (_StartIndex == 0 || endIndex == 0) { return null; }
        if (index < _StartIndex || index > endIndex) { return null; }
        int indexInList = (int) (index - _StartIndex);
        return records.get(indexInList)
                      .getEntry();
    }

    public Segment(File file,
                   long startIndex,
                   long endIndex,
                   boolean canWrite) throws IOException
    {
        fileName = file.getAbsolutePath();
        _FileDirectory = file.getParent();
        this.canWrite = canWrite;
        randomAccessFile = new RandomAccessFile(file,
                                                isCanWrite() ? "rw"
                                                             : "r");
        _StartIndex = startIndex;
        this.endIndex = endIndex;
        fileSize = randomAccessFile.length();
        loadRecord();
    }

    public String getFileName()
    {
        return fileName;
    }

    public RandomAccessFile getRandomAccessFile()
    {
        return randomAccessFile;
    }

    public long getStartIndex()
    {
        return _StartIndex;
    }

    public long getEndIndex()
    {
        return endIndex;
    }

    public void setEndIndex(long newEndIndex)
    {
        endIndex = newEndIndex;
    }

    public long getFileSize()
    {
        return fileSize;
    }

    public boolean isCanWrite()
    {
        return canWrite;
    }

    public List<Record> getRecords()
    {
        return records;
    }

    public void setFileSize(long newFileSize)
    {
        fileSize = newFileSize;
        try {
            randomAccessFile.setLength(fileSize);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadRecord() throws IOException
    {
        long offset = 0;
        long startIndex = -1;
        long endIndex = -1;
        while (offset < fileSize) {
            int length = randomAccessFile.readInt();
            long entryStart = randomAccessFile.getFilePointer();
            byte[] data = new byte[length];
            randomAccessFile.read(data);
            LogEntry entry = new LogEntry();
            entry.decode(data);
            endIndex = entry.getIndex();
            if (startIndex < 0) {
                startIndex = endIndex;
            }
            records.add(new Record(entryStart, entry));
            offset = randomAccessFile.getFilePointer();
        }
        if (startIndex != _StartIndex) {
            throw new IllegalStateException("first log entry term isn't equal segment's start index");
        }
        if (endIndex != this.endIndex) {
            _Logger.warning("input end_index isn't equal read end_index, update this.endIndex %d-> endIndex %d",
                            this.endIndex,
                            endIndex);
            this.endIndex = endIndex;
        }
    }

    public void close()
    {
        String newFileName = String.format("z_chess_raft_seg_%020d-%020d_r", _StartIndex, endIndex);
        String newAbsolutePath = _FileDirectory + File.separator + newFileName;
        File newFile = new File(newAbsolutePath);
        File oldFile = new File(fileName);
        try {
            randomAccessFile.close();
            FileUtils.moveFile(oldFile, newFile);
            randomAccessFile = new RandomAccessFile(newFile, "r");
            canWrite = false;
        }
        catch (IOException e) {
            _Logger.warning("close error || mv old[%s]->new[%s] ", e, fileName, newFileName);
        }
    }

    public void addRecord(LogEntry entry)
    {
        try {
            int length = entry.dataLength();
            randomAccessFile.writeInt(length);
            long offset = randomAccessFile.getFilePointer();
            records.add(new Record(offset, entry));
            randomAccessFile.write(entry.encode());
        }
        catch (IOException e) {
            _Logger.warning("add record failed ", e);
            throw new ZException("add record failed ");
        }
    }

    public long drop() throws IOException
    {
        randomAccessFile.close();
        File file = new File(fileName);
        FileUtils.forceDelete(file);
        return fileSize;
    }

    public long truncate(long newEndIndex) throws IOException
    {
        int i = (int) (newEndIndex + 1 - getStartIndex());
        setEndIndex(newEndIndex);
        long newFileSize = getRecords().get(i)
                                       .getOffset()
                           - 4;
        long size = getFileSize() - newFileSize;
        setFileSize(newFileSize);
        for (int j = records.size(); j > i; j--) {
            records.remove(j - 1);
        }
        randomAccessFile.close();
        //缩减后一定处于可write状态
        String newFileName = String.format("z_chess_raft_seg_%020d-%020d_w", getStartIndex(), getEndIndex());
        String newFullFileName = _FileDirectory + File.separator + newFileName;
        if (new File(getFileName()).renameTo(new File(newFullFileName))) {
            randomAccessFile = new RandomAccessFile(newFullFileName, "rw");
            fileName = newFullFileName;
        }
        else {
            throw new ZException("file [%s] rename to [%s] failed", getFileName(), newFileName);
        }
        return size;
    }
}
