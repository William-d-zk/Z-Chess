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

package com.tgx.chess.cluster.raft.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tgx.chess.cluster.raft.IRaftMachine;
import com.tgx.chess.cluster.raft.IRaftNode.RaftState;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.util.Triple;

/**
 * @author william.d.zk
 * @date 2019/12/10
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class RaftMachine
        implements
        IRaftMachine
{
    private final long                         _PeerId;
    private long                               mTerm;//触发选举时 mTerm>mIndexTerm
    private long                               mIndex;//本地日志Index，Leader：mIndex>=mCommit 其他状态：mIndex<=mCommit
    private long                               mIndexTerm; //本地日志对应的Term
    private long                               mCandidate;
    private long                               mLeader;
    private long                               mCommit;   //集群中已知的最大CommitIndex
    private long                               mApplied;  //本地已被应用的Index
    private int                                mState;
    private Set<Triple<Long,
                       String,
                       Integer>>               mPeerSet;
    private Set<Triple<Long,
                       String,
                       Integer>>               mGateSet;

    @JsonCreator
    public RaftMachine(@JsonProperty("peer_id") long peerId)
    {
        _PeerId = peerId;
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
        append(mPeerSet, peers);
    }

    @SafeVarargs
    public final void appendGate(Triple<Long,
                                        String,
                                        Integer>... gates)
    {
        if (mGateSet == null) {
            mGateSet = new TreeSet<>(Comparator.comparing(ITriple::getFirst));
        }
        append(mGateSet, gates);
    }

    @SafeVarargs
    private final void append(Set<Triple<Long,
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
    public long increaseTerm()
    {
        return ++mTerm;
    }

    @Override
    public void increaseApplied()
    {
        mApplied++;
    }

    @Override
    public String toString()
    {
        return "RaftMachine{"
               + "_PeerId="
               + _PeerId
               + ", mTerm="
               + mTerm
               + ", mIndex="
               + mIndex
               + ", mIndexTerm="
               + mIndexTerm
               + ", mCandidate="
               + mCandidate
               + ", mLeader="
               + mLeader
               + ", mCommit="
               + mCommit
               + ", mApplied="
               + mApplied
               + ", mState="
               + RaftState.valueOf(mState)
               + ", mPeerSet="
               + mPeerSet
               + '}';
    }
}
