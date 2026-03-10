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

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.knight.raft.features.IRaftMachine;

import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Learner 节点管理器
 * 
 * Learner 是一种特殊的 Raft 节点：
 * - 接收日志复制（用于数据备份或扩展读）
 * - 不参与选举投票
 * - 不参与 commit 多数派计算
 * - 可以位于异地，延迟不影响集群性能
 * 
 * 使用场景：
 * 1. 异地备份 - 将数据复制到异地数据中心
 * 2. 读扩展 - 增加只读副本分担读压力
 * 3. 数据审计 - 独立的审计节点
 * 
 * @author william.d.zk
 */
public class LearnerManager
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    private final NavigableMap<Long, IRaftMachine> _Learners = new ConcurrentSkipListMap<>();
    private final String _Name;

    public LearnerManager(String name)
    {
        _Name = name;
    }

    /**
     * 添加 Learner 节点
     */
    public void addLearner(IRaftMachine learner)
    {
        if(learner == null) {
            return;
        }
        
        _Learners.put(learner.peer(), learner);
        _Logger.info("Added learner node: %#x", learner.peer());
    }

    /**
     * 移除 Learner 节点
     */
    public void removeLearner(long peerId)
    {
        IRaftMachine removed = _Learners.remove(peerId);
        if(removed != null) {
            _Logger.info("Removed learner node: %#x", peerId);
        }
    }

    /**
     * 获取 Learner 节点
     */
    public IRaftMachine getLearner(long peerId)
    {
        return _Learners.get(peerId);
    }

    /**
     * 获取所有 Learner
     */
    public Map<Long, IRaftMachine> getAllLearners()
    {
        return new ConcurrentSkipListMap<>(_Learners);
    }

    /**
     * 检查是否是 Learner
     */
    public boolean isLearner(long peerId)
    {
        return _Learners.containsKey(peerId);
    }

    /**
     * 获取 Learner 数量
     */
    public int getLearnerCount()
    {
        return _Learners.size();
    }

    /**
     * 清空所有 Learner
     */
    public void clear()
    {
        _Learners.clear();
        _Logger.info("Cleared all learners");
    }

    /**
     * 更新 Learner 状态
     */
    public void updateLearner(IRaftMachine update)
    {
        IRaftMachine learner = _Learners.get(update.peer());
        if(learner != null) {
            // 更新索引等信息
            if(learner instanceof RaftMachine) {
                ((RaftMachine) learner).from(update);
            }
        }
    }

    /**
     * 检查是否有 Learner
     */
    public boolean hasLearners()
    {
        return !_Learners.isEmpty();
    }

    public String name()
    {
        return _Name;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(_Name + "{Learners:");
        _Learners.forEach((k, v) -> sb.append(String.format(" %#x", k)));
        return sb.append('}').toString();
    }
}
