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

package com.tgx.chess.cluster.raft;

import com.tgx.chess.cluster.raft.model.log.LogEntry;
import com.tgx.chess.cluster.raft.model.log.LogMeta;
import com.tgx.chess.cluster.raft.model.log.SnapshotMeta;

import java.util.List;

/**
 * @author william.d.zk
 */
public interface IRaftDao
{
    void updateAll();

    long getLastLogIndex();

    long getFirstLogIndex();

    LogEntry getEntry(long index);

    long getEntryTerm(long index);

    void updateLogMeta(long term, long firstLogIndex, long candidate);

    void updateSnapshotMeta(long lastIncludeIndex, long lastIncludeTerm);

    long append(List<LogEntry> entryList);

    void truncatePrefix(long newFirstIndex);

    void truncateSuffix(long newEndIndex);

    LogMeta getLogMeta();

    SnapshotMeta getSnapshotMeta();

    long getTotalSize();
}
