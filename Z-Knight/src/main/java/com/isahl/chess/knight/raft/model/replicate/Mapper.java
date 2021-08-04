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

package com.isahl.chess.knight.raft.model.replicate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.config.ZRaftConfig;
import com.isahl.chess.knight.raft.inf.IRaftMapper;
import com.isahl.chess.knight.raft.model.RaftConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.isahl.chess.knight.raft.inf.IRaftMachine.MIN_START;
import static com.isahl.chess.knight.raft.inf.IRaftMachine.TERM_NAN;
import static com.isahl.chess.knight.raft.model.replicate.Segment.SEGMENT_PREFIX;
import static com.isahl.chess.knight.raft.model.replicate.Segment.SEGMENT_SUFFIX_WRITE;

@Component
public class Mapper
        implements IRaftMapper
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    private final String                    _LogDataDir;
    private final String                    _LogMetaDir;
    private final String                    _SnapshotDir;
    private final String                    _RaftConfigDir;
    private final long                      _MaxSegmentSize;
    private final TreeMap<Long, Segment>    _Index2SegmentMap          = new TreeMap<>();
    private final IRaftConfig               _RaftConfig;
    private final TypeReference<RaftConfig> _TypeReferenceOfRaftConfig = new TypeReference<>() {};

    private          LogMeta       mLogMeta;
    private          SnapshotMeta  mSnapshotMeta;
    private volatile long          vTotalSize;
    private volatile boolean       vValid;
    // 表示是否正在安装snapshot，leader向follower安装，leader和follower同时处于installSnapshot状态
    private final    AtomicBoolean _InstallSnapshot = new AtomicBoolean(false);
    // 表示节点自己是否在对状态机做snapshot
    private final    AtomicBoolean _TakeSnapshot    = new AtomicBoolean(false);
    private final    Lock          _SnapshotLock    = new ReentrantLock();

    @Autowired
    public Mapper(ZRaftConfig config)
    {
        String baseDir = config.getBaseDir();
        _RaftConfig = config;
        _RaftConfigDir = String.format("%s%s.conf", baseDir, File.separator);
        _LogMetaDir = String.format("%s%s.raft", baseDir, File.separator);
        _LogDataDir = String.format("%s%s.data", baseDir, File.separator);
        _SnapshotDir = String.format("%s%s.snapshot", baseDir, File.separator);
        _MaxSegmentSize = config.getMaxSegmentSize();
    }

    @PostConstruct
    private void init()
    {
        File file = new File(_RaftConfigDir);
        if(!file.exists() && !file.mkdirs()) {
            throw new SecurityException(String.format("%s check mkdir authority", _RaftConfigDir));
        }
        file = new File(_LogMetaDir);
        if(!file.exists() && !file.mkdirs()) {
            throw new SecurityException(String.format("%s check mkdir authority", _LogMetaDir));
        }
        file = new File(_LogDataDir);
        if(!file.exists() && !file.mkdirs()) {
            throw new SecurityException(String.format("%s check mkdir authority", _LogDataDir));
        }
        file = new File(_SnapshotDir);
        if(!file.exists() && !file.mkdirs()) {
            throw new SecurityException(String.format("%s check mkdir authority", _SnapshotDir));
        }
        List<Segment> segments = readSegments();
        if(segments != null) {
            for(Segment segment : segments) {
                _Index2SegmentMap.put(segment.getStartIndex(), segment);
            }
        }
        String metaFileName = _LogMetaDir + File.separator + ".metadata";
        try {
            RandomAccessFile metaFile = new RandomAccessFile(metaFileName, "rw");
            mLogMeta = LogMeta.loadFromFile(metaFile);
        }
        catch(FileNotFoundException e) {
            _Logger.warning("meta file not exist, name: %s", metaFileName);
        }
        metaFileName = _SnapshotDir + File.separator + ".metadata";
        try {
            RandomAccessFile metaFile = new RandomAccessFile(metaFileName, "rw");
            mSnapshotMeta = SnapshotMeta.loadFromFile(metaFile);
        }
        catch(FileNotFoundException e) {
            _Logger.warning("meta file not exist, name: %s", metaFileName);
        }
        File configFile = getConfigFile();
        try {
            FileInputStream fis = new FileInputStream(configFile);
            _RaftConfig.update(JsonUtil.readValue(fis, _TypeReferenceOfRaftConfig));
        }
        catch(FileNotFoundException e) {
            _Logger.warning("config file not exist, name: %s", configFile.getName());
        }
        catch(IOException e) {
            _Logger.warning("update config failed", e);
        }
        if(checkState()) {
            installSnapshot();
        }
        else {
            //存在本地文件错误损失→重置
            _Index2SegmentMap.clear();
            mLogMeta.reset();
            mSnapshotMeta.reset();
            clearSegments();
            loadDefaultGraphSet();
            flushAll();
        }
        vValid = true;
    }

    @PreDestroy
    void dispose()
    {
        mLogMeta.close();
        mSnapshotMeta.close();
        String configFileName = _RaftConfigDir + File.separator + ".raft_config";
        try {
            File configFile = new File(configFileName);
            if(configFile.exists() || configFile.createNewFile()) {
                JsonUtil.writeValueWithFile(_RaftConfig.getConfig(), configFile);
            }
        }
        catch(IOException e) {
            _Logger.warning("config file create & write ", e);
        }
        _Logger.debug("raft dao dispose");
    }

    private File getConfigFile()
    {
        String configFileName = _RaftConfigDir + File.separator + ".raft_config";
        return new File(configFileName);
    }

    @Override
    public void loadDefaultGraphSet()
    {
        mLogMeta.setPeerSet(_RaftConfig.getPeers());
        mLogMeta.setGateSet(_RaftConfig.getGates());
    }

    @Override
    public void flushAll()
    {
        mLogMeta.flush();
        mSnapshotMeta.flush();
        JsonUtil.writeValueWithFile(_RaftConfig.getConfig(), getConfigFile());
    }

    @Override
    public void flush()
    {
        mLogMeta.flush();
    }

    @Override
    public long getEndIndex()
    {
        /*
         * 有两种情况segment为空
         * 1、第一次初始化，start = 1，end = 0
         * 2、snapshot刚完成，日志正好被清理掉，start = snapshot + 1， end = snapshot
         */
        if(_Index2SegmentMap.isEmpty()) {return getStartIndex() - 1;}
        return _Index2SegmentMap.lastEntry()
                                .getValue()
                                .getEndIndex();
    }

    @Override
    public long getStartIndex()
    {
        return mLogMeta.getStart();
    }

    @Cacheable(value = "raft_log_entry",
               unless = "#result == null",
               key = "#index")
    @Override
    public LogEntry getEntry(long index)
    {
        long startIndex = getStartIndex();
        long endIndex = getEndIndex();
        if(index < startIndex || index > endIndex) {
            if(startIndex > MIN_START) {
                _Logger.debug("index out of range, index=%d, start_index=%d, end_index=%d",
                              index,
                              startIndex,
                              endIndex);
            }
            return null;
        }
        if(_Index2SegmentMap.isEmpty()) {return null;}
        Segment segment = _Index2SegmentMap.floorEntry(index)
                                           .getValue();
        return segment.getEntry(index);
    }

    @Override
    public long getEntryTerm(long index)
    {
        LogEntry entry = getEntry(index);
        return entry == null ? TERM_NAN : entry.getTerm();
    }

    @Override
    public void updateLogStart(long start)
    {
        mLogMeta.setStart(start);
    }

    @Override
    public void updateLogIndexAndTerm(long index, long term)
    {
        mLogMeta.setIndex(index);
        mLogMeta.setIndexTerm(term);
    }

    @Override
    public void updateLogCommit(long commit)
    {
        mLogMeta.setCommit(commit);
        mSnapshotMeta.setCommit(commit);
    }

    @Override
    public void updateTerm(long term)
    {
        mLogMeta.setTerm(term);
        mSnapshotMeta.setTerm(term);
    }

    @Override
    public void updateCandidate(long candidate)
    {
        mLogMeta.setCandidate(candidate);
    }

    @Override
    public void updateLogApplied(long applied)
    {
        mLogMeta.setApplied(applied);
    }

    @Override
    public void updateSnapshotMeta(long lastIncludeIndex, long lastIncludeTerm)
    {
        mSnapshotMeta.setCommit(lastIncludeIndex);
        mSnapshotMeta.setTerm(lastIncludeTerm);
        mSnapshotMeta.flush();
    }

    /**
     * @see Segment fileNameFormatter(boolean readonly)
     * [%s_%s_%s]
     * SEGMENT_PREFIX,SEGMENT_DATE_FORMATTER,SEGMENT_SUFFIX_{READONLY/WRITE}
     */
    private final static Pattern SEGMENT_NAME_PATTERN = Pattern.compile(SEGMENT_PREFIX + "_(\\d+)-(\\d+)_([rw])");

    private List<Segment> readSegments()
    {
        File dataDir = new File(_LogDataDir);
        if(!dataDir.isDirectory()) {throw new IllegalArgumentException("_LogDataDir doesn't point to a directory");}
        File[] subs = dataDir.listFiles();
        if(subs != null) {
            return Stream.of(subs)
                         .filter(sub->!sub.isDirectory())
                         .map(sub->{
                             try {
                                 String fileName = sub.getName();
                                 _Logger.debug("sub:%s", fileName);
                                 Matcher matcher = SEGMENT_NAME_PATTERN.matcher(fileName);
                                 if(matcher.matches()) {
                                     long start = Long.parseLong(matcher.group(1));
                                     long end = Long.parseLong(matcher.group(2));
                                     String g3 = matcher.group(3);
                                     boolean canWrite = SEGMENT_SUFFIX_WRITE.equalsIgnoreCase(g3);
                                     return new Segment(sub, start, end, canWrite);
                                 }
                             }
                             catch(IOException | IllegalArgumentException e) {
                                 e.printStackTrace();
                             }
                             return null;
                         })
                         .filter(Objects::nonNull)
                         .peek(segment->vTotalSize += segment.getFileSize())
                         .collect(Collectors.toList());
        }
        return null;
    }

    private void clearSegments()
    {
        File dataDir = new File(_LogDataDir);
        File[] subs = dataDir.listFiles();
        if(subs != null) {
            Stream.of(subs)
                  .forEach(sub->{
                      if(sub.isDirectory()) {
                          if(sub.listFiles() == null) {
                              boolean success = sub.delete();
                              _Logger.info("remove segment-dir %s[%s]", sub.getName(), success);
                          }
                      }
                      else {
                          boolean success = sub.delete();
                          _Logger.info("remove segment %s[%s]", sub.getName(), success);
                      }
                  });
        }
    }

    @Override
    public boolean append(LogEntry entry)
    {
        _Logger.debug("wait to append %s", entry);
        if(entry == null) {return false;}
        long newEndIndex = getEndIndex() + 1;
        if(entry.getIndex() == newEndIndex) {
            byte[] data = entry.encode();
            int size = data.length + 4;
            boolean needNewFile = false;
            if(_Index2SegmentMap.isEmpty()) {
                needNewFile = true;
            }
            else {
                Segment segment = _Index2SegmentMap.lastEntry()
                                                   .getValue();
                if(!segment.isCanWrite()) {
                    needNewFile = true;
                }
                else if(segment.getFileSize() + size >= _MaxSegmentSize) {
                    needNewFile = true;
                    // segment的文件close并改名
                    segment.freeze();
                }
            }
            Segment targetSegment = null;
            if(needNewFile) {
                String newFileName = String.format(Segment.fileNameFormatter(false), newEndIndex, 0);
                _Logger.info("new segment file :%s", newFileName);
                File newFile = new File(_LogDataDir + File.separator + newFileName);
                if(!newFile.exists()) {
                    try {
                        if(newFile.createNewFile()) {
                            targetSegment = new Segment(newFile, newEndIndex, 0, true);
                            _Index2SegmentMap.put(newEndIndex, targetSegment);
                        }
                        else {throw new IOException("create file failed");}
                    }
                    catch(IOException e) {
                        _Logger.warning("create segment file failed %s", e, newFileName);
                        return false;
                    }
                }
            }
            else {
                targetSegment = _Index2SegmentMap.lastEntry()
                                                 .getValue();
            }
            if(targetSegment != null) {
                targetSegment.add(data, entry);
                vTotalSize += size;
                _Logger.debug("append ok: %d", newEndIndex);
                return true;
            }
        }
        _Logger.warning("append failed: [new end %d|expect %d]", newEndIndex, entry.getIndex());
        return false;
    }

    @Override
    public void truncatePrefix(long newFirstIndex)
    {
        if(newFirstIndex <= getStartIndex()) {return;}
        long oldFirstIndex = getStartIndex();
        while(!_Index2SegmentMap.isEmpty()) {
            Segment segment = _Index2SegmentMap.firstEntry()
                                               .getValue();
            if(segment.isCanWrite()) {
                break;
            }
            if(newFirstIndex > segment.getEndIndex()) {
                try {
                    vTotalSize -= segment.drop();
                    _Index2SegmentMap.remove(segment.getStartIndex());
                }
                catch(Exception ex2) {
                    _Logger.warning("delete file exception:", ex2);
                }
            }
            else {
                break;
            }
        }
        long newActualFirstIndex;
        if(_Index2SegmentMap.isEmpty()) {
            newActualFirstIndex = newFirstIndex;
        }
        else {
            newActualFirstIndex = _Index2SegmentMap.firstKey();
        }
        updateLogStart(newActualFirstIndex);
        _Logger.debug("Truncating log from old first index %d to new first index %d",
                      oldFirstIndex,
                      newActualFirstIndex);
    }

    @Override
    public LogEntry truncateSuffix(long newEndIndex)
    {
        long endIndex = getEndIndex();
        if(newEndIndex >= endIndex) {return null;}
        _Logger.debug("Truncating log from old end index %d to new end index %d", getEndIndex(), newEndIndex);
        while(!_Index2SegmentMap.isEmpty()) {
            Segment segment = _Index2SegmentMap.lastEntry()
                                               .getValue();
            try {
                if(newEndIndex == segment.getEndIndex()) {
                    LogEntry newEndEntry = getEntry(newEndIndex);
                    // 此时entry 不会为null, 无需此处直接操作log meta.由上层逻辑负责更新
                    _Logger.debug("truncate suffix,new entry: %d@%d", newEndEntry.getIndex(), newEndEntry.getTerm());
                    return newEndEntry;
                }
                else if(newEndIndex < segment.getStartIndex()) {
                    vTotalSize -= segment.drop();
                    _Index2SegmentMap.remove(segment.getStartIndex());
                }
                else if(newEndIndex < segment.getEndIndex()) {
                    vTotalSize -= segment.truncate(newEndIndex);
                }
            }
            catch(IOException ex) {
                _Logger.warning("truncateSuffix error ", ex);
                return null;
            }
        }
        return null;
    }

    public boolean isInstallSnapshot()
    {
        return _InstallSnapshot.get();
    }

    public boolean isTakeSnapshot()
    {
        return _TakeSnapshot.get();
    }

    public void lockSnapshot()
    {
        _SnapshotLock.lock();
    }

    @Override
    public LogMeta getLogMeta()
    {
        return mLogMeta;
    }

    @Override
    public SnapshotMeta getSnapshotMeta()
    {
        return mSnapshotMeta;
    }

    @Override
    public long getTotalSize()
    {
        return vTotalSize;
    }

    private void installSnapshot()
    {
        // TODO 安装本地已经获取的 sn
        //  snapshot 存档。
        // 1. 检查snapshot的完整性
        // 2. 检查snapshot的数签
    }

    private boolean checkState()
    {
        return mLogMeta.getIndex() == getEndIndex();
    }

    @Override
    public boolean isValid()
    {
        return vValid;
    }
}
