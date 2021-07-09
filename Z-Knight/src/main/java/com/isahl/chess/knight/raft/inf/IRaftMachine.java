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

package com.isahl.chess.knight.raft.inf;

import com.isahl.chess.king.base.inf.IReset;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.knight.raft.model.RaftState;
import com.isahl.chess.queen.db.inf.IStorage;

import java.util.Set;

/**
 * @author william.d.zk
 * @date 2019/12/10
 */
public interface IRaftMachine
        extends IReset
{
    long INDEX_NAN = -1;
    long TERM_NAN  = -1;
    long MIN_START = 1;

    /**
     * @return 当前任期 自然数
     */
    long getTerm();

    /**
     * @return 当前任期的 log-index
     */
    long getIndex();

    long getIndexTerm();

    /**
     * @return 候选人 Id
     */
    long getCandidate();

    /**
     * @return 主节点 Id
     */
    long getLeader();

    /**
     * @return 节点的全局ID
     */
    long getPeerId();

    /**
     * @return 节点状态
     */
    RaftState getState();

    /**
     * @return 已知最大的已提交日志的索引值
     */
    long getCommit();

    /**
     * @return 最后一条被应用到状态机的索引值
     */
    long getApplied();

    long getMatchIndex();

    /**
     * @return 集群中所有节点
     */
    Set<RaftNode> getPeerSet();

    /**
     * @return 跨集群网关
     */
    Set<RaftNode> getGateSet();

    void appendLog(long index, long indexTerm, IRaftMapper dao);

    void rollBack(long index, long indexTerm, IRaftMapper dao);

    void commit(long index, IRaftMapper dao);

    void apply(IRaftMapper dao);

    void beLeader(IRaftMapper dao);

    void beCandidate(IRaftMapper dao);

    void beElector(long candidate, long term, IRaftMapper dao);

    void follow(long leader, long term, long commit, IRaftMapper dao);

    void follow(long term, IRaftMapper dao);

    <T extends IRaftMachine & IStorage> T createCandidate();

    <T extends IRaftMachine & IStorage> T createLeader();

    <T extends IRaftMachine & IStorage> T createFollower();

    <T extends IRaftMachine & IStorage> T createLearner();
}
