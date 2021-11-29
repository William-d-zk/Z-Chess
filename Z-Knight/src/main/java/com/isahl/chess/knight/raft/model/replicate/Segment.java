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

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Segment
{
    final static String SEGMENT_PREFIX          = "z_chess_raft_seg";
    final static String SEGMENT_SUFFIX_WRITE    = "w";
    final static String SEGMENT_SUFFIX_READONLY = "r";
    final static String SEGMENT_DATE_FORMATTER  = "%020d-%020d";

    public static class Record
    {
        private final long     _Offset;
        private final LogEntry _Entry;

        public Record(long offset, LogEntry entry)
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

    private final static Logger _Logger = Logger.getLogger("cluster.knight." + Segment.class.getSimpleName());

    private final long         _StartIndex;
    private final String       _FileDirectory;
    private final List<Record> _Records = new ArrayList<>();

    private RandomAccessFile mRandomAccessFile;
    private String           mFileName;
    private long             mEndIndex;
    private long             mFileSize;
    private boolean          mCanWrite;

    public LogEntry getEntry(long index)
    {
        if(_StartIndex == 0 || mEndIndex == 0) {return null;}
        if(index < _StartIndex || index > mEndIndex) {return null;}
        int indexInList = (int) (index - _StartIndex);
        LogEntry logEntry = _Records.get(indexInList)
                                    .getEntry();
        if(logEntry.getIndex() != index) {
            _Logger.warning("segment get log-entry %d [%s]", index, logEntry);
        }
        return logEntry;
    }

    public Segment(File file, long startIndex, long endIndex, boolean canWrite) throws IOException
    {
        mFileName = file.getAbsolutePath();
        _FileDirectory = file.getParent();
        _StartIndex = startIndex;
        mCanWrite = canWrite;
        mRandomAccessFile = new RandomAccessFile(file, isCanWrite() ? "rw" : "r");
        mEndIndex = endIndex;
        mFileSize = mRandomAccessFile.length();
        loadRecord();
    }

    public static String fileNameFormatter(boolean readonly)
    {
        return String.format("%s_%s_%s",
                             SEGMENT_PREFIX,
                             SEGMENT_DATE_FORMATTER,
                             readonly ? SEGMENT_SUFFIX_READONLY : SEGMENT_SUFFIX_WRITE);
    }

    public String getFileName()
    {
        return mFileName;
    }

    public RandomAccessFile getRandomAccessFile()
    {
        return mRandomAccessFile;
    }

    public long getStartIndex()
    {
        return _StartIndex;
    }

    public long getEndIndex()
    {
        return mEndIndex;
    }

    public void setEndIndex(long newEndIndex)
    {
        mEndIndex = newEndIndex;
    }

    public long getFileSize()
    {
        return mFileSize;
    }

    public boolean isCanWrite()
    {
        return mCanWrite;
    }

    public List<Record> getRecords()
    {
        return _Records;
    }

    public void setFileSize(long newFileSize)
    {
        mFileSize = newFileSize;
        try {
            mRandomAccessFile.setLength(mFileSize);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void loadRecord() throws IOException
    {
        long offset = 0;
        long startIndex = -1;
        long endIndex = 0;
        if(mFileSize == 0) {return;}
        while(offset < mFileSize) {
            long entryStart = mRandomAccessFile.getFilePointer();
            LogEntry entry = LogEntry.load(LogEntry._Factory, mRandomAccessFile);
            if(entry != null) {
                endIndex = entry.getIndex();
                if(startIndex < 0) {
                    startIndex = endIndex;
                }
                _Records.add((int) (endIndex - startIndex), new Record(entryStart, entry));
            }
            offset = mRandomAccessFile.getFilePointer();
        }
        if(startIndex != _StartIndex) {
            throw new ZException("first entry index %d isn't equal segment's start_index %d", startIndex, _StartIndex);
        }
        if(endIndex != mEndIndex) {
            mEndIndex = endIndex;
        }
    }

    public void freeze()
    {
        String newFileName = String.format(fileNameFormatter(true), _StartIndex, mEndIndex);
        String newAbsolutePath = _FileDirectory + File.separator + newFileName;
        File newFile = new File(newAbsolutePath);
        File oldFile = new File(mFileName);
        try {
            mRandomAccessFile.close();
            FileUtils.moveFile(oldFile, newFile);
            mRandomAccessFile = new RandomAccessFile(newFile, "r");
            mCanWrite = false;
        }
        catch(IOException e) {
            _Logger.warning("close error || mv old[%s]->new[%s] ", e, mFileName, newFileName);
        }
    }

    public void add(LogEntry entry)
    {
        try {
            long offset = mRandomAccessFile.getFilePointer();
            ByteBuffer output = entry.encode();
            mRandomAccessFile.write(output.array());
            _Records.add(new Record(offset, entry));
            mEndIndex = entry.getIndex();
            mFileSize += output.capacity();
        }
        catch(IOException e) {
            throw new ZException("add record failed ", e);
        }
    }

    public long drop() throws IOException
    {
        mRandomAccessFile.close();
        File file = new File(mFileName);
        FileUtils.forceDelete(file);
        return mFileSize;
    }

    public long truncate(long newEndIndex) throws IOException
    {
        int recordIndex = (int) (newEndIndex - getStartIndex());
        setEndIndex(newEndIndex);
        long newFileSize = getRecords().get(recordIndex)
                                       .getOffset() - 4;
        long size = getFileSize() - newFileSize;
        setFileSize(newFileSize);
        if(_Records.size() > recordIndex + 1) {
            _Records.subList(recordIndex + 1, _Records.size())
                    .clear();
        }
        mRandomAccessFile.close();
        // 缩减后一定处于可write状态
        String newFileName = String.format(fileNameFormatter(false), getStartIndex(), getEndIndex());
        String newFullFileName = _FileDirectory + File.separator + newFileName;
        if(new File(getFileName()).renameTo(new File(newFullFileName))) {
            mRandomAccessFile = new RandomAccessFile(newFullFileName, "rw");
            mFileName = newFullFileName;
        }
        else {
            throw new ZException("file [%s] rename to [%s] failed", getFileName(), newFileName);
        }
        return size;
    }
}
