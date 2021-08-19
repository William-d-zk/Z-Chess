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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.knight.raft.model.RaftNode;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import static com.isahl.chess.knight.raft.inf.IRaftMachine.MIN_START;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LogMeta
        extends BaseMeta
{
    private final static int _SERIAL = INTERNAL_SERIAL + 1;

    /**
     * 存储日志的 start index，由于有 snapshot的存在 start之前的日志将被抛弃，
     * <p>
     * ``` 1 ``` 为首条日志index
     */
    private long          mStart;
    /**
     * 本机存储日志的 index
     */
    private long          mIndex;
    /**
     * 本机存储日志的 index-term
     */
    private long          mIndexTerm;
    /**
     * 已存储的最大任期号
     */
    private long          mTerm;
    /**
     * 当前状态机候选人
     */
    private long          mCandidate;
    /**
     * 集群中已知的最大的被提交的日志index
     */
    private long          mCommit;
    /**
     * 已被应用到状态机日志index
     */
    private long          mApplied;
    /**
     * 集群节点信息
     */
    private Set<RaftNode> mPeerSet;
    /**
     * 集群跨分区网关
     */
    private Set<RaftNode> mGateSet;

    @Override
    public void reset()
    {
        mStart = MIN_START;
        mIndex = 0;
        mIndexTerm = 0;
        mTerm = 0;
        mCandidate = 0;
        mCommit = 0;
        mApplied = 0;
        if(mPeerSet != null) { mPeerSet.clear(); }
        if(mGateSet != null) { mGateSet.clear(); }
        flush();
    }

    @JsonCreator
    public LogMeta(
            @JsonProperty("start")
                    long start,
            @JsonProperty("term")
                    long term,
            @JsonProperty("index")
                    long index,
            @JsonProperty("index_term")
                    long indexTerm,
            @JsonProperty("candidate")
                    long candidate,
            @JsonProperty("commit")
                    long commit,
            @JsonProperty("applied")
                    long applied,
            @JsonProperty("peer_set")
                    Set<RaftNode> peerSet,
            @JsonProperty("gate_set")
                    Set<RaftNode> gateSet)
    {
        mStart = start;
        mTerm = term;
        mIndex = index;
        mIndexTerm = indexTerm;
        mCandidate = candidate;
        mCommit = commit;
        mApplied = applied;
        mPeerSet = peerSet;
        mGateSet = gateSet;
    }

    private LogMeta()
    {
        mStart = MIN_START;
    }

    public static LogMeta loadFromFile(RandomAccessFile file)
    {
        try {
            if(file.length() > 0) {
                file.seek(0);
                int mLength = file.readInt();
                if(mLength > 0) {
                    byte[] data = new byte[mLength];
                    file.read(data);
                    LogMeta logMeta = JsonUtil.readValue(data, LogMeta.class);
                    if(logMeta != null) {
                        logMeta.setFile(file);
                        logMeta.decode(data);
                        return logMeta;
                    }
                }
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return new LogMeta().setFile(file);
    }

    @JsonIgnore
    public LogMeta setFile(RandomAccessFile source)
    {
        mFile = source;
        return this;
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

    public Set<RaftNode> getPeerSet()
    {
        return mPeerSet;
    }

    public void setPeerSet(Collection<RaftNode> peers)
    {
        if(peers != null && !peers.isEmpty()) {
            mPeerSet = new TreeSet<>(peers);
        }
    }

    public Set<RaftNode> getGateSet()
    {
        return mGateSet;
    }

    public void setGateSet(Collection<RaftNode> gates)
    {
        if(gates != null && !gates.isEmpty()) {
            mGateSet = new TreeSet<>(gates);
        }
    }

    public long getIndex()
    {
        return mIndex;
    }

    public void setIndex(long index)
    {
        this.mIndex = index;
    }

    public void setIndexTerm(long term)
    {
        mIndexTerm = term;
    }

    public long getIndexTerm()
    {
        return mIndexTerm;
    }

}
