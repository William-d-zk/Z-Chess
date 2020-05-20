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

package com.tgx.chess.knight.raft.model;

import static com.tgx.chess.king.topology.ZUID.INVALID_PEER_ID;
import static com.tgx.chess.knight.raft.RaftState.CANDIDATE;
import static com.tgx.chess.knight.raft.RaftState.ELECTOR;
import static com.tgx.chess.knight.raft.RaftState.FOLLOWER;
import static com.tgx.chess.knight.raft.RaftState.LEADER;
import static com.tgx.chess.queen.db.inf.IStorage.Operation.OP_APPEND;
import static com.tgx.chess.queen.db.inf.IStorage.Operation.OP_INSERT;
import static com.tgx.chess.queen.db.inf.IStorage.Operation.OP_NULL;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.knight.json.JsonUtil;
import com.tgx.chess.knight.raft.IRaftDao;
import com.tgx.chess.knight.raft.IRaftMachine;
import com.tgx.chess.knight.raft.RaftState;
import com.tgx.chess.queen.db.inf.IStorage;

/**
 * @author william.d.zk
 * @date 2019/12/10
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class RaftMachine
        implements
        IRaftMachine,
        IStorage
{
    private static final Logger _Logger = Logger.getLogger("cluster.knight." + RaftMachine.class.getSimpleName());

    private final static int                   RAFT_MACHINE_SERIAL = DB_SERIAL + 3;
    private final long                         _PeerId;
    private long                               mTerm;      //触发选举时 mTerm > mIndexTerm
    private long                               mIndex;     //本地日志Index，Leader：mIndex >= mCommit 其他状态：mIndex <= mCommit
    private long                               mIndexTerm; //本地日志对应的Term
    private long                               mMatchIndex;//Leader: 记录 follower 已经接收的记录
    private long                               mCandidate;
    private long                               mLeader;
    private long                               mCommit;    //集群中已知的最大CommitIndex
    private long                               mApplied;   //本地已被应用的Index
    private int                                mState;
    private Set<Triple<Long,
                       String,
                       Integer>>               mPeerSet;
    private Set<Triple<Long,
                       String,
                       Integer>>               mGateSet;
    @JsonIgnore
    private Operation                          mOperation          = OP_NULL;
    @JsonIgnore
    private int                                mLength;

    @Override
    public int dataLength()
    {
        return mLength;
    }

    @Override
    public int decode(byte[] data)
    {
        RaftMachine json = JsonUtil.readValue(data, getClass());
        Objects.requireNonNull(json);
        assert (_PeerId == json.getPeerId());
        mTerm = json.getTerm();
        mIndex = json.getIndex();
        mIndexTerm = json.getIndexTerm();
        mCandidate = json.getCandidate();
        mLeader = json.getLeader();
        mCommit = json.getCommit();
        mApplied = json.getApplied();
        mState = json.getState()
                     .getCode();
        mPeerSet = json.getPeerSet();
        mGateSet = json.getGateSet();
        mLength = data.length;
        return mLength;
    }

    @Override
    public byte[] encode()
    {
        byte[] payload = JsonUtil.writeValueAsBytes(this);
        Objects.requireNonNull(payload);
        mLength = payload.length;
        return payload;
    }

    @Override
    public int serial()
    {
        return RAFT_MACHINE_SERIAL;
    }

    @Override
    @JsonIgnore
    public long primaryKey()
    {
        return _PeerId;
    }

    @JsonIgnore
    public void setOperation(Operation op)
    {
        mOperation = op;
    }

    @Override
    @JsonIgnore
    public Operation operation()
    {
        return mOperation;
    }

    @Override
    @JsonIgnore
    public Strategy strategy()
    {
        return Strategy.RETAIN;
    }

    @JsonCreator
    public RaftMachine(@JsonProperty("peer_id") long peerId)
    {
        _PeerId = peerId;
        mMatchIndex = INDEX_NAN;
        mIndex = INDEX_NAN;
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
        return _PeerId;
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
    public Set<Triple<Long,
                      String,
                      Integer>> getPeerSet()
    {
        return mPeerSet;
    }

    @Override
    public Set<Triple<Long,
                      String,
                      Integer>> getGateSet()
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

    public void setPeerSet(Set<Triple<Long,
                                      String,
                                      Integer>> peerSet)
    {
        mPeerSet = peerSet;
    }

    public void setGateSet(Set<Triple<Long,
                                      String,
                                      Integer>> gateSet)
    {
        mGateSet = gateSet;
    }

    @SafeVarargs
    public final void appendPeer(Triple<Long,
                                        String,
                                        Integer>... peers)
    {
        if (mPeerSet == null) {
            mPeerSet = new TreeSet<>(Comparator.comparing(ITriple::getFirst));
        }
        appendGraph(mPeerSet, peers);
    }

    @SafeVarargs
    public final void appendGate(Triple<Long,
                                        String,
                                        Integer>... gates)
    {
        if (mGateSet == null) {
            mGateSet = new TreeSet<>(Comparator.comparing(ITriple::getFirst));
        }
        appendGraph(mGateSet, gates);
    }

    @SafeVarargs
    private final void appendGraph(Set<Triple<Long,
                                              String,
                                              Integer>> set,
                                   Triple<Long,
                                          String,
                                          Integer>... a)
    {
        Objects.requireNonNull(set);
        if (a == null || a.length == 0) { return; }
        mPeerSet.addAll(Arrays.asList(a));
    }

    @Override
    public void apply(IRaftDao dao)
    {
        //TODO 分摊集群读写压力，需要在follower apply的时候notify 那些在follower上订阅了延迟一致的consumer
        if (mIndex < mApplied) { throw new IllegalStateException(); }
        mApplied = Long.min(mIndex, mCommit);
        dao.updateLogApplied(mApplied);
        _Logger.debug("apply => %d | [index %d commit %d]", mApplied, mIndex, mCommit);
    }

    @Override
    public void commit(long index, IRaftDao dao)
    {
        /*
         只有Leader 能执行此操作
         */
        mCommit = index;
        dao.updateLogCommit(mCommit);
        mApplied = mCommit;
        dao.updateLogApplied(mApplied);
    }

    @Override
    public void beLeader(IRaftDao dao)
    {
        mState = LEADER.getCode();
        mLeader = _PeerId;
        mCandidate = _PeerId;
        mMatchIndex = mCommit;
        dao.updateTerm(mTerm);
        dao.updateCandidate(_PeerId);
    }

    @Override
    public void beCandidate(IRaftDao dao)
    {
        mState = CANDIDATE.getCode();
        mLeader = INVALID_PEER_ID;
        mCandidate = _PeerId;
        dao.updateTerm(++mTerm);
        dao.updateCandidate(_PeerId);
    }

    @Override
    public void beElector(long candidate, long term, IRaftDao dao)
    {
        mState = ELECTOR.getCode();
        mLeader = INVALID_PEER_ID;
        mCandidate = candidate;
        mTerm = term;
        dao.updateTerm(mTerm);
        dao.updateCandidate(mCandidate);
    }

    @Override
    public void follow(long leader, long term, long commit, IRaftDao dao)
    {
        mState = FOLLOWER.getCode();
        mTerm = term;
        mLeader = leader;
        mCandidate = leader;
        mCommit = commit;
        dao.updateTerm(mTerm);
        dao.updateCandidate(mLeader);
        dao.updateLogCommit(mCommit);
    }

    @Override
    public void follow(long term, IRaftDao dao)
    {
        mState = FOLLOWER.getCode();
        mTerm = term;
        mLeader = INVALID_PEER_ID;
        mCandidate = INVALID_PEER_ID;
        dao.updateTerm(mTerm);
        dao.updateCandidate(mCandidate);
    }

    public void follow(IRaftDao dao)
    {
        follow(mTerm, dao);
    }

    @Override
    public RaftMachine createCandidate()
    {
        RaftMachine candidate = new RaftMachine(_PeerId);
        candidate.setTerm(mTerm + 1);
        candidate.setIndex(mIndex);
        candidate.setIndexTerm(mIndexTerm);
        candidate.setState(CANDIDATE);
        candidate.setCandidate(_PeerId);
        candidate.setLeader(INVALID_PEER_ID);
        candidate.setCommit(mCommit);
        candidate.setOperation(OP_INSERT);
        return candidate;
    }

    @Override
    public RaftMachine createLeader()
    {
        RaftMachine leader = new RaftMachine(_PeerId);
        leader.setTerm(mTerm);
        leader.setIndex(mIndex);
        leader.setIndexTerm(mIndexTerm);
        leader.setCommit(mCommit);
        leader.setLeader(_PeerId);
        leader.setCandidate(_PeerId);
        leader.setState(LEADER);
        leader.setOperation(OP_APPEND);
        return leader;
    }

    @Override
    public void appendLog(long index, long indexTerm, IRaftDao dao)
    {
        mIndex = index;
        mMatchIndex = index;
        mIndexTerm = indexTerm;
        dao.updateLogIndexAndTerm(mIndex, mIndexTerm);
    }

    @Override
    public void reset()
    {
        mIndex = 0;
        mMatchIndex = INDEX_NAN;
        mIndexTerm = 0;
        mApplied = 0;
        mCommit = 0;
    }

    @Override
    public String toString()
    {
        return String.format("\n{\n"
                             + "\t\tpeerId:%#x\n"
                             + "\t\tstate:%s\n"
                             + "\t\tterm:%d\n"
                             + "\t\tindex:%d\n"
                             + "\t\tindex_term:%d\n"
                             + "\t\tmatch_index:%d\n"
                             + "\t\tcommit:%d\n"
                             + "\t\tapplied:%d\n"
                             + "\t\tleader:%#x\n"
                             + "\t\tcandidate:%#x\n"
                             + "\t\tpeers:%s\n}",
                             _PeerId,
                             getState(),
                             mTerm,
                             mIndex,
                             mIndexTerm,
                             mMatchIndex,
                             mCommit,
                             mApplied,
                             mLeader,
                             mCandidate,
                             mPeerSet);
    }

}
