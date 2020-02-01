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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tgx.chess.config.ZRaftConfig;
import com.tgx.chess.king.base.log.Logger;

@Component
public class RaftDao
{
    private final Logger _Logger = Logger.getLogger(RaftDao.class.getName());

    private final ZRaftConfig            _Config;
    private final String                 _BaseDir;
    private final String                 _LogDataDir;
    private final String                 _LogMetaDir;
    private final int                    _MaxSegmentSize;
    private LogMeta                      logMeta;
    private final TreeMap<Long,
                          Segment>       _Index2SegmentMap = new TreeMap<>();

    @Autowired
    public RaftDao(ZRaftConfig config)
    {
        _Config = config;
        _BaseDir = _Config.getBaseDir();
        _LogMetaDir = String.format("%s%s.raft", _BaseDir, File.separator);
        _LogDataDir = String.format("%s%sdata", _BaseDir, File.separator);
        _MaxSegmentSize = _Config.getMaxSegmentSize();
    }

    @PostConstruct
    private void init()
    {
        File file = new File(_LogMetaDir);
        if (!file.exists()) {
            if (!file.mkdirs()) { throw new SecurityException(String.format("%s check mkdir authority", _LogMetaDir)); }
        }
        file = new File(_LogDataDir);
        if (!file.exists()) {
            if (!file.mkdirs()) { throw new SecurityException(String.format("%s check mkdir authority", _LogDataDir)); }
        }
        List<Segment> segments = readSegments();
        {
            String metaFileName = _LogMetaDir + File.separator + "metadata";
            try {
                RandomAccessFile metaFile = new RandomAccessFile(metaFileName, "rw");
                logMeta = new LogMeta(metaFile).load();
            }
            catch (FileNotFoundException e) {
                _Logger.warning("meta file not exist, name=%s", metaFileName);
            }
        }
    }

    public long getLastLogIndex()
    {
        /* 
         有两种情况segment为空
         1、第一次初始化，firstLogIndex = 1，lastLogIndex = 0
         2、snapshot刚完成，日志正好被清理掉，firstLogIndex = snapshotIndex + 1， lastLogIndex = snapshotIndex
        */
        if (_Index2SegmentMap.isEmpty()) { return getFirstLogIndex() - 1; }
        Segment lastSegment = _Index2SegmentMap.lastEntry()
                                               .getValue();
        return lastSegment.getEndIndex();
    }

    public long getFirstLogIndex()
    {
        return logMeta.getFirstLogIndex();
    }

    public LogEntry getEntry(long index)
    {
        long firstLogIndex = getFirstLogIndex();
        long lastLogIndex = getLastLogIndex();
        if (index == 0 || index < firstLogIndex || index > lastLogIndex) {
            _Logger.debug("index out of range, index={}, firstLogIndex={}, lastLogIndex={}",
                          index,
                          firstLogIndex,
                          lastLogIndex);
            return null;
        }
        if (_Index2SegmentMap.isEmpty()) { return null; }
        Segment segment = _Index2SegmentMap.floorEntry(index)
                                           .getValue();
        return segment.getEntry(index);
    }

    public long getEntryTerm(long index)
    {
        LogEntry entry = getEntry(index);
        return entry == null ? 0
                             : entry.getTerm();
    }

    public void updateLogMeta(long term, long firstLogIndex, long candidate)
    {
        logMeta.setTerm(term);
        logMeta.setFirstLogIndex(firstLogIndex);
        logMeta.setCandidate(candidate);
        logMeta.update();
        _Logger.info(" term:%d,first log index:%d,candidate:%d", term, firstLogIndex, candidate);
    }

    private final static Pattern SEGMENT_NAME = Pattern.compile("z_chess_raft_seg_(\\d+)_([rw])");

    public List<Segment> readSegments()
    {
        File dataDir = new File(_LogDataDir);
        if (!dataDir.isDirectory()) throw new IllegalArgumentException("_LogDataDir doesn't point to a directory");
        File[] subs = dataDir.listFiles();
        if (subs != null) {
            return Stream.of(subs)
                         .filter(sub -> !sub.isDirectory())
                         .map(sub ->
                         {
                             try {
                                 String fileName = sub.getName();
                                 _Logger.info("sub:%s", fileName);
                                 Matcher matcher = SEGMENT_NAME.matcher(fileName);
                                 if (matcher.matches()) {
                                     long start = Long.parseLong(matcher.group(1));
                                     String g2 = matcher.group(2);
                                     boolean readOnly = !g2.equalsIgnoreCase("w");
                                     return new Segment(sub.getName(), start, 0, !readOnly);
                                 }
                             }
                             catch (IOException e) {
                                 e.printStackTrace();
                             }
                             return null;
                         })
                         .filter(Objects::nonNull)
                         .collect(Collectors.toList());
        }
        return null;
    }
}
