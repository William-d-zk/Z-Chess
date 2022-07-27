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
import com.isahl.chess.knight.raft.model.RaftState;
import com.isahl.chess.queen.db.model.IStorage;

/**
 * @author william.d.zk
 * @date 2019/12/10
 */
public interface IRaftMachine
        extends IReset,
                IStorage,
                Comparable<IRaftMachine>
{
    long INDEX_NAN = -1;
    long TERM_NAN  = -1;
    long MIN_START = 1;

    /**
     * @return 当前任期 自然数
     */
    long term();

    /**
     * @return 当前任期的 log-index
     */
    long index();

    long indexTerm();

    /**
     * @return 候选人 Id
     */
    long candidate();

    /**
     * @return 主节点 Id
     */
    long leader();

    /**
     * @return 节点的全局ID
     */
    long peer();

    /**
     * @return 节点状态
     */
    int state();

    /**
     * @return 已知最大的已提交日志的索引值
     */
    long commit();

    /**
     * @return 最后一条被应用到状态机的索引值
     */
    long accept();

    long matchIndex();

    void term(long term);

    void index(long index);

    void indexTerm(long term);

    void matchIndex(long matchIndex);

    void candidate(long candidate);

    void leader(long leader);

    void commit(long commit);

    void accept(long accept);

    void append(long index, long indexTerm, IRaftMapper mapper);

    void follow(long term, long leader, IRaftMapper mapper);

    void commit(long index, IRaftMapper mapper);

    void rollBack(long index, long indexTerm, IRaftMapper mapper);

    /**
     * @param state elector,candidate,leader,client,follow(重置)
     *              follow → follow(long term, long leader, IRaftMapper mapper)
     */
    void approve(RaftState state);

    void outside();

    void gate();

    /**
     * 确认集群变更完成
     */
    void confirm();

    /**
     * 进入集群变更状态
     */
    void modify();

    void modify(RaftState state);

    default boolean isInState(RaftState state) {return (state() & state.getCode()) != 0;}

    default boolean isGreaterThanState(RaftState state)
    {
        return (state() & RaftState.MASK.getCode()) - (state.getCode() & RaftState.MASK.getCode()) > 0;
    }

    default boolean isLessThanState(RaftState state) {return (state() & RaftState.MASK.getCode()) - (state.getCode() & RaftState.MASK.getCode()) < 0;}

    @Override
    default int compareTo(IRaftMachine o)
    {
        return Long.compare(peer(), o.peer());
    }

    String toPrimary();

}
