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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import static com.isahl.chess.knight.raft.features.IRaftMachine.MIN_START;

@ISerialGenerator(parent = IProtocol.CLUSTER_KNIGHT_CONSISTENT_SERIAL,
                  serial = IProtocol.CLUSTER_KNIGHT_CONSISTENT_SERIAL + 1)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LogMeta
        extends BaseMeta
{

    private static Logger LOG = Logger.getLogger("cluster.knight." + LogMeta.class.getSimpleName());

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
    public int length()
    {
        int length = 8 + // start
                     8 + // term
                     8 + // index
                     8 + // index term
                     8 + // candidate
                     8 + // commit
                     8; // applied
        length++;
        if(mPeerSet != null && !mPeerSet.isEmpty()) {
            for(RaftNode node : mPeerSet) {length += node.length();}
        }
        length++;
        if(mGateSet != null && !mGateSet.isEmpty()) {
            for(RaftNode node : mGateSet) {length += node.length();}
        }
        return length + super.length();
    }

    @Override
    public ByteBuffer encode()
    {
        ByteBuffer output = super.encode()
                                 .putLong(getStart())
                                 .putLong(getTerm())
                                 .putLong(getIndex())
                                 .putLong(getIndexTerm())
                                 .putLong(getCandidate())
                                 .putLong(getCommit())
                                 .putLong(getApplied());
        if(mPeerSet != null && !mPeerSet.isEmpty()) {
            output.put((byte) mPeerSet.size());
            for(RaftNode node : mPeerSet) {
                output.put(node.encode()
                               .flip());
            }
        }
        else {
            output.put((byte) 0);
        }
        if(!mGateSet.isEmpty()) {
            output.put((byte) mGateSet.size());
            for(RaftNode node : mGateSet) {
                output.put(node.encode()
                               .flip());
            }
        }
        else {
            output.put((byte) 0);
        }
        return output;
    }

    @Override
    public void decode(ByteBuffer input)
    {
        super.decode(input);
        setStart(input.getLong());
        setTerm(input.getLong());
        setIndex(input.getLong());
        setIndexTerm(input.getLong());
        setCandidate(input.getLong());
        setCommit(input.getLong());
        setApplied(input.getLong());
        int pc = input.get() & 0xFF;
        if(pc > 0) {
            mPeerSet = new TreeSet<>();
            for(int i = 0; i < pc; i++) {
                RaftNode peer = new RaftNode();
                peer.decode(input);
            }
        }
        int gc = input.get() & 0xFF;
        if(gc > 0) {
            mGateSet = new TreeSet<>();
            for(int i = 0; i < gc; i++) {
                RaftNode gate = new RaftNode();
                gate.decode(input);
            }
        }
    }

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
        if(mPeerSet != null) {mPeerSet.clear();}
        if(mGateSet != null) {mGateSet.clear();}
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
        super(Operation.OP_INSERT, Strategy.RETAIN);
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

    public LogMeta()
    {
        super();
        mStart = MIN_START;
    }

    public static Factory<LogMeta> _Factory = serial->{
        LogMeta meta = new LogMeta();
        if(meta.serial() == serial) {return meta;}
        return null;
    };

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
