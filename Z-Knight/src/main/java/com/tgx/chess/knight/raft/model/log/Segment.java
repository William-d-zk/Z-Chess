/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.tgx.chess.knight.raft.model.log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.tgx.chess.king.base.exception.ZException;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.knight.json.JsonUtil;

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

    private final static Logger _Logger  = Logger.getLogger("cluster.knight." + Segment.class.getSimpleName());
    private final long          _StartIndex;
    private final String        _FileDirectory;
    private final List<Record>  _Records = new ArrayList<>();
    private RandomAccessFile    mRandomAccessFile;
    private String              mFileName;
    private long                mEndIndex;
    private long                mFileSize;
    private boolean             mCanWrite;

    public LogEntry getEntry(long index)
    {
        if (_StartIndex == 0 || mEndIndex == 0) { return null; }
        if (index < _StartIndex || index > mEndIndex) { return null; }
        int indexInList = (int) (index - _StartIndex);
        return _Records.get(indexInList)
                       .getEntry();
    }

    public Segment(File file,
                   long startIndex,
                   long endIndex,
                   boolean canWrite) throws IOException
    {
        mFileName = file.getAbsolutePath();
        _FileDirectory = file.getParent();
        mCanWrite = canWrite;
        mRandomAccessFile = new RandomAccessFile(file,
                                                 isCanWrite() ? "rw"
                                                              : "r");
        _StartIndex = startIndex;
        mEndIndex = endIndex;
        mFileSize = mRandomAccessFile.length();
        loadRecord();
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
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadRecord() throws IOException
    {
        long offset = 0;
        long startIndex = -1;
        long endIndex = 0;
        if (offset == mFileSize) { return; }
        while (offset < mFileSize) {
            int length = mRandomAccessFile.readInt();
            long entryStart = mRandomAccessFile.getFilePointer();
            byte[] data = new byte[length];
            mRandomAccessFile.read(data);
            LogEntry entry = JsonUtil.readValue(data, LogEntry.class);
            if (entry != null) {
                entry.decode(data);
                endIndex = entry.getIndex();
                if (startIndex < 0) {
                    startIndex = endIndex;
                }
                _Records.add(new Record(entryStart, entry));
            }
            offset = mRandomAccessFile.getFilePointer();
        }
        if (startIndex != _StartIndex) {
            throw new IllegalArgumentException(String.format("first entry index %d isn't equal segment's start_index %d",
                                                             startIndex,
                                                             _StartIndex));
        }
        if (endIndex != mEndIndex) {
            _Logger.debug("input end_index isn't equal read end_index, update mEndIndex %d-> endIndex %d",
                          mEndIndex,
                          endIndex);
            mEndIndex = endIndex;
        }
    }

    public void freeze()
    {
        String newFileName = String.format("z_chess_raft_seg_%020d-%020d_r", _StartIndex, mEndIndex);
        String newAbsolutePath = _FileDirectory + File.separator + newFileName;
        File newFile = new File(newAbsolutePath);
        File oldFile = new File(mFileName);
        try {
            mRandomAccessFile.close();
            FileUtils.moveFile(oldFile, newFile);
            mRandomAccessFile = new RandomAccessFile(newFile, "r");
            mCanWrite = false;
        }
        catch (IOException e) {
            _Logger.warning("close error || mv old[%s]->new[%s] ", e, mFileName, newFileName);
        }
    }

    public void addRecord(LogEntry entry)
    {
        try {
            int length = entry.dataLength();
            mRandomAccessFile.writeInt(length);
            long offset = mRandomAccessFile.getFilePointer();
            _Records.add(new Record(offset, entry));
            mRandomAccessFile.write(entry.encode());
            mEndIndex = entry.getIndex();
        }
        catch (IOException e) {
            _Logger.warning("add record failed ", e);
            throw new ZException("add record failed ");
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
        int recordIndex = (int) (newEndIndex + 1 - getStartIndex());
        setEndIndex(newEndIndex);
        long newFileSize = getRecords().get(recordIndex)
                                       .getOffset()
                           - 4;
        long size = getFileSize() - newFileSize;
        setFileSize(newFileSize);
        if (_Records.size() > recordIndex + 1) {
            _Records.subList(recordIndex + 1, _Records.size())
                    .clear();
        }
        mRandomAccessFile.close();
        //缩减后一定处于可write状态
        String newFileName = String.format("z_chess_raft_seg_%020d-%020d_w", getStartIndex(), getEndIndex());
        String newFullFileName = _FileDirectory + File.separator + newFileName;
        if (new File(getFileName()).renameTo(new File(newFullFileName))) {
            mRandomAccessFile = new RandomAccessFile(newFullFileName, "rw");
            mFileName = newFullFileName;
        }
        else {
            throw new ZException("file [%s] rename to [%s] failed", getFileName(), newFileName);
        }
        return size;
    }
}
