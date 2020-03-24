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

package com.tgx.chess.cluster.raft.model.log;

import java.io.RandomAccessFile;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tgx.chess.json.JsonUtil;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class LogMeta
        extends
        BaseMeta
{
    private final static int _SERIAL = INTERNAL_SERIAL + 1;

    /** 已存储日志的start index */
    private long      mStart;
    /** 本机存储日志的 index */
    private long      mIndex;
    /** 已存储日志的最大任期号 */
    private long      mTerm;
    /** 当前状态机候选人 */
    private long      mCandidate;
    /** 集群中已知的最大的被提交的日志index */
    private long      mCommit;
    /** 已被应用到状态机日志index */
    private long      mApplied;
    /** 集群节点信息 */
    private Set<Long> mNodeSet;

    LogMeta()
    {
        super(null);
    }

    LogMeta(RandomAccessFile file)
    {
        super((file));
    }

    LogMeta load()
    {
        loadFromFile();
        return this;
    }

    @Override
    public int decode(byte[] data)
    {
        LogMeta json = JsonUtil.readValue(data, getClass());
        Objects.requireNonNull(json);
        mStart = json.getStart();
        mTerm = json.getTerm();
        mCandidate = json.getCandidate();
        mNodeSet = json.getNodeSet();
        return mLength = data.length;
    }

    @Override
    public byte[] encode()
    {
        byte[] data = JsonUtil.writeValueAsBytes(this);
        Objects.requireNonNull(data);
        mLength = data.length;
        return data;
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

    public long getStart()
    {
        return mStart;
    }

    public void setStart(long start)
    {
        mStart = start;
    }

    public long getTerm()
    {
        return mTerm;
    }

    public void setTerm(long term)
    {
        mTerm = term;
    }

    public long getCandidate()
    {
        return mCandidate;
    }

    public void setCandidate(long candidate)
    {
        mCandidate = candidate;
    }

    public long getCommit()
    {
        return mCommit;
    }

    public void setCommit(long commit)
    {
        mCommit = commit;
    }

    public long getApplied()
    {
        return mApplied;
    }

    public void setApplied(long applied)
    {
        mApplied = applied;
    }

    public Set<Long> getNodeSet()
    {
        return mNodeSet;
    }

    public void setNodeSet(Set<Long> nodeSet)
    {
        mNodeSet = nodeSet;
    }

    public long getIndex()
    {
        return mIndex;
    }

    public void setIndex(long index)
    {
        this.mIndex = index;
    }
}
