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

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.model.ListSerial;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.commons.io.FileUtils;

public class Segment {
  static final String SEGMENT_PREFIX = "z_chess_raft_seg";
  static final String SEGMENT_SUFFIX_WRITE = "w";
  static final String SEGMENT_SUFFIX_READONLY = "r";
  static final String SEGMENT_DATE_FORMATTER = "%020d-%020d";

  private static final Logger _Logger =
      Logger.getLogger("cluster.knight." + Segment.class.getSimpleName());

  private final long _StartIndex;
  private final String _FileDirectory;
  private final ListSerial<LogEntry> _Records;

  private RandomAccessFile mRandomAccessFile;
  private FileChannel mFileChannel;
  private String mFileName;
  private long mEndIndex;
  private long mFileSize;
  private boolean mCanWrite;
  private volatile boolean mFsyncEnabled = true; // 默认开启 fsync

  public LogEntry getEntry(long index) {
    int listIndex = (int) (index - _StartIndex);
    if (_StartIndex == 0
        || mEndIndex == 0
        || index < _StartIndex
        || index > mEndIndex
        || listIndex < 0
        || listIndex >= _Records.size()) {
      _Logger.warning(
          "get entry failed@%d;start:%d,end:%d,size:%d",
          index, _StartIndex, mEndIndex, _Records.size());
      return null;
    }
    LogEntry logEntry = _Records.get(listIndex);
    if (logEntry.index() != index) {
      _Logger.warning("segment get(%d) log entry [%s]", index, logEntry);
    }
    return logEntry;
  }

  public Segment(File file, long startIndex, boolean canWrite)
      throws IOException,
          InvocationTargetException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException {
    _FileDirectory = file.getParent();
    _Records = new ListSerial<>(LogEntry::new);
    mEndIndex = _StartIndex = startIndex;
    mFileName = file.getAbsolutePath();
    mCanWrite = canWrite;
    mRandomAccessFile = new RandomAccessFile(file, isCanWrite() ? "rw" : "r");
    mFileChannel = mRandomAccessFile.getChannel();
    mFileSize = mRandomAccessFile.length();
    loadRecord();
  }

  public static String fileNameFormatter(boolean readonly) {
    return String.format(
        "%s_%s_%s",
        SEGMENT_PREFIX,
        SEGMENT_DATE_FORMATTER,
        readonly ? SEGMENT_SUFFIX_READONLY : SEGMENT_SUFFIX_WRITE);
  }

  public long getStartIndex() {
    return _StartIndex;
  }

  public long getEndIndex() {
    return mEndIndex;
  }

  private void setEndIndex(long newEndIndex) {
    mEndIndex = newEndIndex;
  }

  public long getFileSize() {
    return mFileSize;
  }

  public boolean isCanWrite() {
    return mCanWrite;
  }

  /**
   * 设置是否启用 fsync
   *
   * @param fsyncEnabled true 表示每次写入后自动刷盘
   */
  public void setFsyncEnabled(boolean fsyncEnabled) {
    mFsyncEnabled = fsyncEnabled;
  }

  /** 获取当前 fsync 设置 */
  public boolean isFsyncEnabled() {
    return mFsyncEnabled;
  }

  /**
   * 强制将内存映射缓冲区的数据刷写到磁盘
   *
   * @throws IOException 当刷盘失败时抛出
   */
  public void flush() throws IOException {
    if (mFileChannel != null && mFileChannel.isOpen()) {
      // 强制文件通道刷盘，包含文件元数据
      mFileChannel.force(true);
      _Logger.debug("segment flushed [%s], size=%d", mFileName, mFileSize);
    }
  }

  /**
   * 尝试刷盘，不抛出异常
   *
   * @return true 表示刷盘成功，false 表示失败
   */
  public boolean tryFlush() {
    try {
      flush();
      return true;
    } catch (IOException e) {
      _Logger.warning("segment flush failed [%s]: %s", e, mFileName, e.getMessage());
      return false;
    }
  }

  private void setFileSize(long newFileSize) {
    mFileSize = newFileSize;
    try {
      mRandomAccessFile.setLength(mFileSize);
    } catch (IOException e) {
      _Logger.warning("set file size failed (%d)", e, mFileSize);
    }
  }

  private void loadRecord()
      throws IOException,
          InvocationTargetException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException {
    if (mFileSize == 0) {
      return;
    }
    _Records.decode(ByteBuf.wrap(mFileChannel.map(FileChannel.MapMode.READ_ONLY, 0, mFileSize)));
    // NPE 防护：检查列表是否为空
    if (_Records.isEmpty()) {
      throw new ZException("segment records is empty after decode, file: %s", mFileName);
    }
    long startIndex = _Records.get(0).index();
    if (startIndex != _StartIndex) {
      throw new ZException(
          "first entry index %d isn't equal segment's start_index %d", startIndex, _StartIndex);
    }
    mEndIndex = _Records.get(_Records.size() - 1).index();
  }

  public void freeze() {
    String newFileName = String.format(fileNameFormatter(true), _StartIndex, mEndIndex);
    String newAbsolutePath = _FileDirectory + File.separator + newFileName;
    File newFile = new File(newAbsolutePath);
    File oldFile = new File(mFileName);
    try {
      // 关闭前强制刷盘
      if (mFsyncEnabled && mFileChannel != null && mFileChannel.isOpen()) {
        mFileChannel.force(true);
      }
      mRandomAccessFile.close();
      FileUtils.moveFile(oldFile, newFile);
      mRandomAccessFile = new RandomAccessFile(newFile, "r");
      mFileChannel = mRandomAccessFile.getChannel();
      mCanWrite = false;
    } catch (IOException e) {
      _Logger.warning("close error || mv old[%s]->new[%s] ", e, mFileName, newFileName);
    }
  }

  public boolean add(LogEntry entry) {
    try {
      if (_Records.add(entry)) {
        byte[] output;
        MappedByteBuffer mapped;
        if (_Records.size() > 1) {
          output = entry.encoded();
          mapped = mFileChannel.map(FileChannel.MapMode.READ_WRITE, mFileSize, output.length);
          mapped.put(output);
          // 强制刷盘（如果开启）
          if (mFsyncEnabled) {
            mapped.force();
          }
          mapped =
              mFileChannel.map(
                  FileChannel.MapMode.READ_WRITE,
                  _Records.SERIAL_POS,
                  _Records.SIZE_POS + Integer.BYTES);
          int oldLength = mapped.getInt(_Records.LENGTH_POS);
          mapped.putInt(_Records.LENGTH_POS, oldLength + output.length);
          mapped.putInt(_Records.SIZE_POS, _Records.size());
          // 强制刷盘（如果开启）
          if (mFsyncEnabled) {
            mapped.force();
          }
        } else {
          output = _Records.encoded();
          mapped = mFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, output.length);
          mapped.put(output);
          // 强制刷盘（如果开启）
          if (mFsyncEnabled) {
            mapped.force();
          }
        }
        mFileSize += output.length;
        // 最终刷盘确保文件元数据也落盘
        if (mFsyncEnabled) {
          mFileChannel.force(true);
        }
      }
      mEndIndex = entry.index();
      return true;
    } catch (IOException e) {
      _Logger.warning("add record failed ", e);
    }
    return false;
  }

  public long drop() throws IOException {
    // 关闭前尝试刷盘
    if (mFileChannel != null && mFileChannel.isOpen()) {
      try {
        mFileChannel.force(true);
      } catch (IOException e) {
        _Logger.warning("force before drop failed: %s", e.getMessage());
      }
    }
    mRandomAccessFile.close();
    File file = new File(mFileName);
    FileUtils.forceDelete(file);
    return mFileSize;
  }

  public long truncate(long newEndIndex) throws IOException {
    int newRecordCount = (int) (newEndIndex - _StartIndex) + 1;
    if (newRecordCount < 0) {
      throw new ZException("new record size[%d],error input", newRecordCount);
    }
    if (_Records.size() > newRecordCount) {
      _Records.subList(newRecordCount, _Records.size()).clear();
      mEndIndex = newEndIndex;
      long newFileSize = _Records.sizeOf();
      long dropSize = mFileSize - newFileSize;
      MappedByteBuffer mapped =
          mFileChannel.map(
              FileChannel.MapMode.READ_WRITE,
              _Records.SERIAL_POS,
              _Records.SIZE_POS + Integer.BYTES);
      mapped.putInt(_Records.LENGTH_POS, _Records.length());
      mapped.putInt(_Records.SIZE_POS, _Records.size());
      // 强制刷盘
      if (mFsyncEnabled) {
        mapped.force();
      }
      mFileChannel.force(true);
      mRandomAccessFile.setLength(mFileSize = newFileSize);
      mRandomAccessFile.close();
      // 缩减后一定处于可write状态
      String newFileName = String.format(fileNameFormatter(false), _StartIndex, mEndIndex);
      String newFullFileName = _FileDirectory + File.separator + newFileName;
      if (new File(mFileName).renameTo(new File(newFullFileName))) {
        mRandomAccessFile = new RandomAccessFile(newFullFileName, "rw");
        mFileChannel = mRandomAccessFile.getChannel();
        mFileName = newFullFileName;
      } else {
        throw new ZException("file [%s] rename to [%s] failed", mFileName, newFileName);
      }
      return dropSize;
    }
    return 0;
  }
}
