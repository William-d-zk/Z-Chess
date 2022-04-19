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

package com.isahl.chess.knight.raft.features;

import com.isahl.chess.king.base.features.IReset;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.knight.raft.model.replicate.LogEntry;
import com.isahl.chess.knight.raft.model.replicate.LogMeta;
import com.isahl.chess.knight.raft.model.replicate.SnapshotEntry;
import com.isahl.chess.knight.raft.model.replicate.SnapshotMeta;

/**
 * @author william.d.zk
 */
public interface IRaftMapper
        extends IValid,
                IReset
{
    void flush();

    void flushAll();

    long getEndIndex();

    long getStartIndex();

    LogEntry getEntry(long index);

    long getEntryTerm(long index);

    void updateLogStart(long firstLogIndex);

    void updateIndexAtTerm(long index, long indexTerm);

    void updateCommit(long commit);

    void updateTerm(long term);

    void updateAccept(long applied);

    void updateSnapshotMeta(long lastIncludeIndex, long lastIncludeTerm);

    boolean append(LogEntry entry);

    void truncatePrefix(long newFirstIndex);

    LogEntry truncateSuffix(long newEndIndex);

    LogMeta getLogMeta();

    SnapshotMeta getSnapshotMeta();

    SnapshotEntry getSnapshot();

    long getTotalSize();
}
