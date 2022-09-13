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

import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.knight.raft.features.IRaftMachine;
import com.isahl.chess.knight.raft.features.IRaftMapper;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.message.InnerProtocol;

import java.io.Serial;

import static com.isahl.chess.knight.raft.model.RaftState.*;
import static java.lang.String.format;

/**
 * @author william.d.zk
 * @date 2019/12/10
 */
@ISerialGenerator(parent = IProtocol.CLUSTER_KNIGHT_RAFT_SERIAL)
public class RaftMachine
        extends InnerProtocol
        implements IRaftMachine
{
    @Serial
    private static final long serialVersionUID = -3632621828854975482L;

    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    private long mTerm;                     // 触发选举时 term > index-term
    private long mIndex;                    // 本地日志 index，leader-index >= commit-index 其他状态：follower-index <= commit-index
    private long mIndexTerm;                // 本地日志对应的 term
    private long mMatchIndex;               // leader: 记录 follower 已经接收的记录
    private long mCandidate;                // candidate
    private long mLeader;                   // leader
    private long mCommit;                   // 集群中已知的最大 commit-index
    private long mAccept;                   // 本地已被应用的 applied-index
    private int  mState = OUTSIDE.getCode(); // 集群节点状态 默认从 OUTSIDE 开始

    @Override
    public int length()
    {              // peer @ pKey
        return 8 + // term
               8 + // index
               8 + // index-term
               8 + // match-index
               8 + // applied
               8 + // commit
               8 + // candidate
               8 + // leader
               1 + // state
               super.length();
    }

    private RaftMachine(long peerId)
    {
        this(peerId, Operation.OP_MODIFY);
    }

    private RaftMachine(long peerId, Operation operation)
    {
        super(operation, Strategy.RETAIN);
        pKey = peerId;
        mMatchIndex = INDEX_NAN;
        mIndex = INDEX_NAN;
    }

    public RaftMachine(ByteBuf input)
    {
        super(input);
    }

    public static RaftMachine createBy(long peer, Operation operation)
    {
        return new RaftMachine(peer, operation);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mTerm = input.getLong();
        mIndex = input.getLong();
        mIndexTerm = input.getLong();
        mMatchIndex = input.getLong();
        mAccept = input.getLong();
        mCommit = input.getLong();
        mCandidate = input.getLong();
        mLeader = input.getLong();
        mState = input.get();
        remain -= 65;// 8x8 +1;
        return remain;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mTerm)
                    .putLong(mIndex)
                    .putLong(mIndexTerm)
                    .putLong(mMatchIndex)
                    .putLong(mAccept)
                    .putLong(mCommit)
                    .putLong(mCandidate)
                    .putLong(mLeader)
                    .put(mState);
    }

    @Override
    public long term()
    {
        return mTerm;
    }

    @Override
    public long index()
    {
        return mIndex;
    }

    @Override
    public long indexTerm()
    {
        return mIndexTerm;
    }

    @Override
    public long candidate()
    {
        return mCandidate;
    }

    @Override
    public long leader()
    {
        return mLeader;
    }

    @Override
    public long peer()
    {
        return primaryKey();
    }

    @Override
    public int state()
    {
        return mState;
    }

    @Override
    public long commit()
    {
        return mCommit;
    }

    @Override
    public long accept()
    {
        return mAccept;
    }

    @Override
    public long matchIndex()
    {
        return mMatchIndex;
    }

    @Override
    public void term(long term)
    {
        mTerm = term;
    }

    @Override
    public void index(long index)
    {
        mIndex = index;
    }

    @Override
    public void indexTerm(long term)
    {
        mIndexTerm = term;
    }

    @Override
    public void matchIndex(long matchIndex)
    {
        mMatchIndex = matchIndex;
    }

    @Override
    public void candidate(long candidate)
    {
        mCandidate = candidate;
        if(candidate == peer()) {approve(CANDIDATE);}
    }

    @Override
    public void leader(long leader)
    {
        mLeader = leader;
        if(leader == peer()) {approve(LEADER);}
        // else 保持不变即可
    }

    @Override
    public void approve(RaftState state)
    {
        mState &= ~MASK.getCode();
        mState |= state.getCode();
    }

    @Override
    public void outside()
    {
        mState = OUTSIDE.getCode();
    }

    public void gate()
    {
        mState |= GATE.getCode();
    }

    @Override
    public void confirm()
    {
        mState &= ~JOINT.getCode();
    }

    public void modify(RaftState state)
    {
        approve(state);
        modify();
    }

    @Override
    public void modify()
    {
        mState |= JOINT.getCode();
    }

    @Override
    public void commit(long commit)
    {
        mCommit = commit;
    }

    @Override
    public void accept(long accept)
    {
        mAccept = accept;
    }

    /*
    @Override
    public void beLeader(IRaftMapper mapper)
    {
        mState = LEADER.getCode();
        mLeader = peer();
        mMatchIndex = commit();
        mapper.updateTerm(term());
        mCandidate = peer();
    }

    @Override
    public void beCandidate(IRaftMapper mapper)
    {
        mState = CANDIDATE.getCode();
        mLeader = INVALID_PEER_ID;
        mapper.updateTerm(++mTerm);
        mCandidate = peer();
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
        RaftMachine candidate = new RaftMachine(peer(), OP_INSERT);
        candidate.term(mTerm + 1);
        candidate.index(mIndex);
        candidate.indexTerm(mIndexTerm);
        candidate.state(CANDIDATE);
        candidate.candidate(peer());
        candidate.leader(INVALID_PEER_ID);
        candidate.commit(mCommit);
        return candidate;
    }

    public RaftMachine createFollower()
    {
        RaftMachine follower = new RaftMachine(peer(), OP_MODIFY);
        follower.term(mTerm);
        follower.index(mIndex);
        follower.indexTerm(mIndexTerm);
        follower.commit(mCommit);
        follower.candidate(INVALID_PEER_ID);
        follower.leader(INVALID_PEER_ID);
        follower.state(FOLLOWER);
        return follower;
    }
*/
    @Override
    public void follow(long term, long leader, IRaftMapper mapper)
    {
        approve(FOLLOWER);
        mTerm = term;
        mLeader = leader;
        mCandidate = leader;
        mapper.updateTerm(mTerm);
    }

    @Override
    public void commit(long index, IRaftMapper mapper)
    {
        mCommit = index;
        mapper.updateCommit(mCommit);
        mapper.flush();
        _Logger.debug("commit: [%d@%d]", mCommit, mTerm);
    }

    @Override
    public void accept(long index, long indexTerm)
    {
        mAccept = mIndex = index;
        mIndexTerm = indexTerm;
    }

    @Override
    public void rollBack(long index, long indexTerm, IRaftMapper mapper)
    {
        _Logger.debug("rollback to [ %d@%d ]", index, indexTerm);
        accept(index, indexTerm);
        commit(index, mapper);
    }

    @Override
    public void reset()
    {
        mIndex = 0;
        mIndexTerm = 0;
        mMatchIndex = INDEX_NAN;
        mTerm = 0;
        mAccept = 0;
        mCommit = 0;
    }

    @Override
    public String toString()
    {
        return format("""
                              {
                              \t\tpeer:%#x
                              \t\tstate:%s
                              \t\tterm:%d
                              \t\tindex:%d
                              \t\tindex_term:%d
                              \t\tmatch_index:%d
                              \t\tcommit:%d
                              \t\taccept:%d
                              \t\tleader:%#x
                              \t\tcandidate:%#x
                              }
                              """, peer(), RaftState.roleOf(state()), term(), index(), indexTerm(), matchIndex(), commit(), accept(), leader(), candidate());
    }

    @Override
    public String toPrimary()
    {
        return format("P:#%x,%d@%d,C:%d,A:%d[← %#x]", peer(), index(), term(), commit(), accept(), candidate());
    }

    public void from(IRaftMachine source)
    {
        mTerm = source.term();
        mIndex = source.index();
        mIndexTerm = source.indexTerm();
        mMatchIndex = source.matchIndex();
        mCandidate = source.candidate();
        mLeader = source.leader();
        mCommit = source.commit();
        mAccept = source.accept();
        mState = source.state();
    }
}
