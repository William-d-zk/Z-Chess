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

import static java.lang.Math.min;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tgx.chess.bishop.io.zprotocol.raft.X7F_RaftResponse;
import com.tgx.chess.cluster.raft.IRaftMachine;
import com.tgx.chess.cluster.raft.IRaftNode;
import com.tgx.chess.cluster.raft.IRaftNode.RaftState;
import com.tgx.chess.king.base.inf.ITriple;

/**
 * @author william.d.zk
 * @date 2019/12/10
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class RaftMachine
        implements
        IRaftMachine
{
    private final long   _PeerId;
    private long         mTerm;
    private long         mIndex;
    private long         mCandidate;
    private long         mLeader;
    private long         mCommit;
    private long         mApplied;
    private int          mState;
    private Set<ITriple> mNodeSet;

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
    public Set<ITriple> getNodeSet()
    {
        return mNodeSet;
    }

    public void setTerm(long term)
    {
        mTerm = term;
    }

    public void setIndex(long index)
    {
        mIndex = index;
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

    public void setNodeSet(Set<ITriple> nodeSet)
    {
        mNodeSet = nodeSet;
    }

    public void appendNode(ITriple... nodes)
    {
        if (mNodeSet == null) {
            mNodeSet = new TreeSet<>(Comparator.comparing(ITriple::getFirst));
        }
        if (nodes == null || nodes.length == 0) { return; }
        mNodeSet.addAll(Arrays.asList(nodes));
    }

    @Override
    public void merge(IRaftMachine update, IRaftNode self)
    {
        X7F_RaftResponse response = null;
        APPLY:
        {
            if (getTerm() > update.getTerm()) {
                response = self.reject(update.getPeerId(), X7F_RaftResponse.Code.LOWER_TERM.getCode());
                break APPLY;
            }
            if (update.getTerm() > getTerm()) {
                setTerm(update.getTerm());
                if (getState() == RaftState.FOLLOWER
                    && getIndex() <= update.getIndex()
                    && update.getState() == RaftState.CANDIDATE)
                {
                    setCandidate(update.getCandidate());
                    setState(RaftState.ELECTOR);
                    response = self.stepUp(update.getPeerId());
                }
                else if (getState() != RaftState.LEARNER) {
                    setState(RaftState.FOLLOWER);
                    response = self.stepDown(update.getPeerId());
                }
            }
            else switch (getState())
            {
                case ELECTOR:
                    if (update.getState() == RaftState.CANDIDATE && getCandidate() != update.getCandidate()) {
                        response = self.reject(update.getPeerId(), X7F_RaftResponse.Code.ALREADY_VOTE.getCode());
                        break APPLY;
                    }
                    if (update.getState() == RaftState.LEADER) {
                        response = self.follow(update.getPeerId());
                    }
                    break;
                case FOLLOWER:
                    if (update.getState() == RaftState.CANDIDATE) {
                        if (getIndex() <= update.getIndex()) {
                            setCandidate(update.getCandidate());
                            setState(RaftState.ELECTOR);
                            response = self.stepUp(update.getPeerId());
                        }
                        else {
                            response = self.reject(update.getPeerId(), X7F_RaftResponse.Code.OBSOLETE.getCode());
                            break APPLY;
                        }
                    }
                    else if (update.getState() == RaftState.LEADER) {
                        response = self.reTick(update.getPeerId());
                    }
                    else {
                        response = self.reject(update.getPeerId(), X7F_RaftResponse.Code.ILLEGAL_STATE.getCode());
                        break APPLY;
                    }
                    break;
                case LEADER:
                    if (update.getState() == RaftState.LEADER) {
                        if (update.getPeerId() != getPeerId()) {
                            response = self.rejectAndStepDown(update.getPeerId(),
                                                              X7F_RaftResponse.Code.SPLIT_CLUSTER.getCode());
                        }
                        else {
                            //ignore
                            break;
                        }
                    }
                    else {
                        self.reject(update.getPeerId(), X7F_RaftResponse.Code.ILLEGAL_STATE.getCode());
                    }
                    break APPLY;
                case LEARNER:
                    break;
            }

            if (update.getState() == RaftState.LEADER && getCommit() < update.getCommit()) {
                setCommit(min(update.getCommit(), update.getIndex()));
            }
        }
        self.applyAndResponse(response);
    }
}
