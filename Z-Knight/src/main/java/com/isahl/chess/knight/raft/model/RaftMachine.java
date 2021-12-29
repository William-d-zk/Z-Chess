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

package com.isahl.chess.knight.raft.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.knight.raft.features.IRaftMachine;
import com.isahl.chess.knight.raft.features.IRaftMapper;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.message.InnerProtocol;

import java.util.*;

import static com.isahl.chess.king.env.ZUID.INVALID_PEER_ID;
import static com.isahl.chess.knight.raft.model.RaftState.*;
import static com.isahl.chess.queen.db.model.IStorage.Operation.*;

/**
 * @author william.d.zk
 * @date 2019/12/10
 */
@ISerialGenerator(parent = IProtocol.CLUSTER_KNIGHT_RAFT_SERIAL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RaftMachine
        extends InnerProtocol
        implements IRaftMachine
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + RaftMachine.class.getSimpleName());

    private long          mTerm;      // 触发选举时 mTerm > mIndexTerm
    private long          mIndex;     // 本地日志Index，Leader：mIndex >= mCommit 其他状态：mIndex <= mCommit
    private long          mIndexTerm; // 本地日志对应的Term
    private long          mMatchIndex;// Leader: 记录 follower 已经接收的记录
    private long          mCandidate;
    private long          mLeader;
    private long          mCommit;    // 集群中已知的最大CommitIndex
    private long          mApplied;   // 本地已被应用的Index
    private int           mState = FOLLOWER.getCode();
    private Set<RaftNode> mPeerSet;
    private Set<RaftNode> mGateSet;

    @Override
    public int length()
    {   //peer @ pKey
        int length = 8 + // term
                     8 + // index
                     8 + // index-term
                     8 + // match-index
                     8 + // applied
                     8 + // commit
                     8 + // candidate
                     8 + // leader
                     1 + // state
                     1 + // peer-set-size
                     1;  // gate-set-size
        if(mPeerSet != null && !mPeerSet.isEmpty()) {
            for(RaftNode node : mPeerSet) {
                length += node.sizeOf();
            }
        }
        if(mGateSet != null && !mGateSet.isEmpty()) {
            for(RaftNode gate : mGateSet) {
                length += gate.sizeOf();
            }
        }
        return length + super.length();
    }

    @JsonCreator
    public RaftMachine(
            @JsonProperty("peer_id")
                    long peerId)
    {
        this(peerId, Operation.OP_MODIFY);
    }

    public RaftMachine(long peerId, Operation operation)
    {
        super(operation, Strategy.RETAIN);
        pKey = peerId;
        mMatchIndex = INDEX_NAN;
        mIndex = INDEX_NAN;
    }

    public RaftMachine()
    {
        super(Operation.OP_MODIFY, Strategy.RETAIN);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mTerm = input.getLong();
        mIndex = input.getLong();
        mIndexTerm = input.getLong();
        mMatchIndex = input.getLong();
        mApplied = input.getLong();
        mCommit = input.getLong();
        mCandidate = input.getLong();
        mLeader = input.getLong();
        mState = input.get();
        remain -= 67;// 8x8 +1 +1 +1;
        int pLength = buildSet(input, mPeerSet = new TreeSet<>());
        int gLength = buildSet(input, mGateSet = new TreeSet<>());
        if(pLength == 0) {
            mPeerSet = null;
        }
        if(gLength == 0) {
            mGateSet = null;
        }
        return remain - pLength - gLength;
    }

    private int buildSet(ByteBuf input, Set<RaftNode> set)
    {
        int size = input.get() & 0xFF;
        if(size > 0) {
            int position = input.readerIdx();
            for(int i = 0; i < size; i++) {
                RaftNode node = new RaftNode();
                node.decode(input);
                set.add(node);
            }
            return input.readerIdx() - position;
        }
        return 0;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output = super.suffix(output)
                      .putLong(mTerm)
                      .putLong(mIndex)
                      .putLong(mIndexTerm)
                      .putLong(mMatchIndex)
                      .putLong(mApplied)
                      .putLong(mCommit)
                      .putLong(mCandidate)
                      .putLong(mLeader)
                      .put(mState);
        output.put(mPeerSet == null ? 0 : mPeerSet.size());
        if(mPeerSet != null && mPeerSet.size() > 0) {
            for(RaftNode node : mPeerSet) {
                output.put(node.encode());
            }
        }
        output.put(mGateSet == null ? 0 : mGateSet.size());
        if(mGateSet != null && mGateSet.size() > 0) {
            for(RaftNode node : mGateSet) {
                output.put(node.encode());
            }
        }
        return output;
    }

    @Override
    public long getTerm()
    {
        return mTerm;
    }

    @Override
    public long getIndex()
    {
        return mIndex;
    }

    @Override
    public long getIndexTerm()
    {
        return mIndexTerm;
    }

    @Override
    public long getCandidate()
    {
        return mCandidate;
    }

    @Override
    public long getLeader()
    {
        return mLeader;
    }

    @Override
    public long getPeerId()
    {
        return primaryKey();
    }

    @Override
    public RaftState getState()
    {
        return RaftState.valueOf(mState);
    }

    @Override
    public long getCommit()
    {
        return mCommit;
    }

    @Override
    public long getApplied()
    {
        return mApplied;
    }

    @Override
    public long getMatchIndex()
    {
        return mMatchIndex;
    }

    @Override
    public Set<RaftNode> getPeerSet()
    {
        return mPeerSet;
    }

    @Override
    public Set<RaftNode> getGateSet()
    {
        return mGateSet;
    }

    public void setTerm(long term)
    {
        mTerm = term;
    }

    public void setIndex(long index)
    {
        mIndex = index;
    }

    public void setIndexTerm(long term)
    {
        mIndexTerm = term;
    }

    public void setMatchIndex(long matchIndex)
    {
        mMatchIndex = matchIndex;
    }

    public void setCandidate(long candidate)
    {
        mCandidate = candidate;
    }

    public void setLeader(long leader)
    {
        mLeader = leader;
    }

    public void setState(RaftState state)
    {
        mState = state.getCode();
    }

    public void setCommit(long commit)
    {
        mCommit = commit;
    }

    public void setApplied(long applied)
    {
        mApplied = applied;
    }

    public void setPeerSet(Set<RaftNode> peerSet)
    {
        mPeerSet = peerSet;
    }

    public void setGateSet(Set<RaftNode> gateSet)
    {
        mGateSet = gateSet;
    }

    public final void appendPeer(RaftNode... peers)
    {
        if(mPeerSet == null) {
            mPeerSet = new TreeSet<>();
        }
        appendGraph(mPeerSet, peers);
    }

    public final void appendGate(RaftNode... gates)
    {
        if(mGateSet == null) {
            mGateSet = new TreeSet<>();
        }
        appendGraph(mGateSet, gates);
    }

    private void appendGraph(Set<RaftNode> set, RaftNode... a)
    {
        Objects.requireNonNull(set);
        if(a == null || a.length == 0) {return;}
        mPeerSet.addAll(Arrays.asList(a));
    }

    @Override
    public void beLeader(IRaftMapper mapper)
    {
        mState = LEADER.getCode();
        mLeader = getPeerId();
        mMatchIndex = getCommit();
        mapper.updateTerm(getTerm());
        mCandidate = getPeerId();
    }

    @Override
    public void beCandidate(IRaftMapper mapper)
    {
        mState = CANDIDATE.getCode();
        mLeader = INVALID_PEER_ID;
        mapper.updateTerm(++mTerm);
        mCandidate = getPeerId();
    }

    @Override
    public void beElector(long candidate, long term, IRaftMapper mapper)
    {
        mState = ELECTOR.getCode();
        mLeader = INVALID_PEER_ID;
        mapper.updateTerm(mTerm = term);
        mCandidate = candidate;
    }

    @Override
    public void beFollower(long term, IRaftMapper mapper)
    {
        mState = FOLLOWER.getCode();
        mLeader = INVALID_PEER_ID;
        mapper.updateTerm(mTerm = term);
        mCandidate = INVALID_PEER_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RaftMachine createCandidate()
    {
        RaftMachine candidate = new RaftMachine(getPeerId(), OP_INSERT);
        candidate.setTerm(mTerm + 1);
        candidate.setIndex(mIndex);
        candidate.setIndexTerm(mIndexTerm);
        candidate.setState(CANDIDATE);
        candidate.setCandidate(getPeerId());
        candidate.setLeader(INVALID_PEER_ID);
        candidate.setCommit(mCommit);
        return candidate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RaftMachine createLeader()
    {
        RaftMachine leader = new RaftMachine(getPeerId(), OP_APPEND);
        leader.setTerm(mTerm);
        leader.setIndex(mIndex);
        leader.setIndexTerm(mIndexTerm);
        leader.setCommit(mCommit);
        leader.setLeader(getPeerId());
        leader.setCandidate(getPeerId());
        leader.setState(LEADER);
        return leader;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RaftMachine createFollower()
    {
        RaftMachine follower = new RaftMachine(getPeerId(), OP_MODIFY);
        follower.setTerm(mTerm);
        follower.setIndex(mIndex);
        follower.setIndexTerm(mIndexTerm);
        follower.setCommit(mCommit);
        follower.setCandidate(INVALID_PEER_ID);
        follower.setLeader(INVALID_PEER_ID);
        follower.setState(FOLLOWER);
        return follower;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RaftMachine createLearner()
    {
        RaftMachine follower = new RaftMachine(getPeerId(), OP_MODIFY);
        follower.setTerm(mTerm);
        follower.setIndex(mIndex);
        follower.setIndexTerm(mIndexTerm);
        follower.setCommit(mCommit);
        follower.setState(LEARNER);
        follower.setCandidate(INVALID_PEER_ID);
        follower.setLeader(INVALID_PEER_ID);
        return follower;
    }

    @Override
    public void follow(long term, long leader, IRaftMapper mapper)
    {
        mState = FOLLOWER.getCode();
        mTerm = term;
        mLeader = leader;
        mCandidate = leader;
        mapper.updateTerm(mTerm);
    }

    @Override
    public void apply(IRaftMapper mapper)
    {
        mapper.updateLogApplied(mApplied = mIndex);
        _Logger.debug("apply : %d | [index %d commit %d]", mApplied, mIndex, mCommit);
    }

    @Override
    public void commit(long index, IRaftMapper mapper)
    {
        if(mCommit != index) {
            mCommit = index;
            mapper.updateLogCommit(mCommit);
            mapper.flush();
            _Logger.debug("%s commit[%d] ", getState(), mCommit);
        }
    }

    @Override
    public void append(long index, long indexTerm, IRaftMapper mapper)
    {
        mIndex = index;
        mMatchIndex = index;
        mIndexTerm = indexTerm;
        mapper.updateLogIndexAndTerm(mIndex, mIndexTerm);
    }

    @Override
    public void rollBack(long index, long indexTerm, IRaftMapper mapper)
    {
        append(index, indexTerm, mapper);
        apply(mapper);
        commit(index, mapper);
    }

    @Override
    public void reset()
    {
        mState = FOLLOWER.getCode();
        mIndex = 0;
        mIndexTerm = 0;
        mMatchIndex = INDEX_NAN;
        mTerm = 0;
        mApplied = 0;
        mCommit = 0;
    }

    @Override
    public String toString()
    {
        return String.format("""
                                     {
                                     \t\tpeerId:%#x
                                     \t\tstate:%s
                                     \t\tterm:%d
                                     \t\tindex:%d
                                     \t\tindex_term:%d
                                     \t\tmatch_index:%d
                                     \t\tcommit:%d
                                     \t\tapplied:%d
                                     \t\tleader:%#x
                                     \t\tcandidate:%#x
                                     \t\tpeers:%s
                                     \t\tgates:%s
                                     }
                                     """,
                             getPeerId(),
                             getState(),
                             getTerm(),
                             getIndex(),
                             getIndexTerm(),
                             getMatchIndex(),
                             getCommit(),
                             getApplied(),
                             getLeader(),
                             getCandidate(),
                             set2String(mPeerSet),
                             set2String(mGateSet));
    }

    public String toSimple()
    {
        return String.format("""
                                     { my peer:[%#x] term:%d last:%d@%d commit:%d
                                     \t\tleader:%#x
                                     \t\tcondidate:%#x
                                     }
                                     """,
                             getPeerId(),
                             getTerm(),
                             getIndex(),
                             getIndexTerm(),
                             getCommit(),
                             getLeader(),
                             getCandidate());
    }

    private <T> String set2String(Collection<T> set)
    {
        if(set != null) {
            StringBuilder sb = new StringBuilder("[\n");
            for(T t : set) {
                sb.append("\t\t\t<")
                  .append(String.format(" %s ", t))
                  .append(">\n");
            }
            sb.append("\t\t]");
            return sb.toString();
        }
        return null;
    }

    @JsonIgnore
    @Override
    public boolean isEqualState(RaftState state)
    {
        return mState == state.getCode();
    }
}
