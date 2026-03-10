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

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 成员变更管理器 - 实现 Joint Consensus 两阶段提交
 * 
 * 状态流转:
 * IDLE → PREPARING → JOINT → COMMITTING → CONFIRMED
 *                 ↓               ↓
 *               ROLLBACK ← TIMEOUT/ERROR
 * 
 * @author william.d.zk
 */
public class MembershipChangeManager
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    /**
     * 成员变更阶段
     */
    public enum Phase
    {
        /**
         * 空闲状态，没有进行中的变更
         */
        IDLE,
        
        /**
         * 准备阶段，正在收集新配置确认
         */
        PREPARING,
        
        /**
         * Joint Consensus 阶段，新旧配置同时生效
         */
        JOINT,
        
        /**
         * 提交阶段，等待新配置多数派确认
         */
        COMMITTING,
        
        /**
         * 变更完成，新配置生效
         */
        CONFIRMED,
        
        /**
         * 回滚阶段，恢复到旧配置
         */
        ROLLBACK,
        
        /**
         * 变更失败/超时
         */
        FAILED
    }

    /**
     * 变更事务
     */
    public static class ChangeTransaction
    {
        private final long           mId;
        private final long           mLeader;
        private final long[]         mOldPeers;
        private final long[]         mNewPeers;
        private final Instant        mStartTime;
        private final Duration       mTimeout;
        private final Set<Long>      mConfirmedPeers;
        private volatile Phase       mPhase;
        private volatile String      mErrorMessage;

        public ChangeTransaction(long id, long leader, Collection<Long> oldPeers, Collection<Long> newPeers, Duration timeout)
        {
            mId = id;
            mLeader = leader;
            mOldPeers = oldPeers.stream().mapToLong(Long::longValue).toArray();
            mNewPeers = newPeers.stream().mapToLong(Long::longValue).toArray();
            mStartTime = Instant.now();
            mTimeout = timeout;
            mConfirmedPeers = ConcurrentHashMap.newKeySet();
            mPhase = Phase.PREPARING;
        }

        public long getId() { return mId; }
        public long getLeader() { return mLeader; }
        public long[] getOldPeers() { return mOldPeers; }
        public long[] getNewPeers() { return mNewPeers; }
        public Phase getPhase() { return mPhase; }
        public void setPhase(Phase phase) { mPhase = phase; }
        public boolean isTimeout() { return Duration.between(mStartTime, Instant.now()).compareTo(mTimeout) > 0; }
        public Duration getElapsedTime() { return Duration.between(mStartTime, Instant.now()); }
        public String getErrorMessage() { return mErrorMessage; }
        public void setErrorMessage(String message) { mErrorMessage = message; }
        
        /**
         * 确认节点已收到配置变更
         */
        public boolean confirmPeer(long peer)
        {
            return mConfirmedPeers.add(peer);
        }
        
        /**
         * 检查是否达到旧配置多数派确认
         */
        public boolean isOldConfigMajorityConfirmed()
        {
            return mConfirmedPeers.size() > mOldPeers.length / 2;
        }
        
        /**
         * 检查是否达到新配置多数派确认
         */
        public boolean isNewConfigMajorityConfirmed()
        {
            long newConfigConfirmed = mConfirmedPeers.stream()
                                                     .filter(peer -> {
                                                         for(long p : mNewPeers) {
                                                             if(p == peer) return true;
                                                         }
                                                         return false;
                                                     })
                                                     .count();
            return newConfigConfirmed > mNewPeers.length / 2;
        }
        
        /**
         * 获取确认节点数
         */
        public int getConfirmedCount() { return mConfirmedPeers.size(); }
        
        @Override
        public String toString()
        {
            return String.format("ChangeTransaction{id=%d, phase=%s, confirmed=%d/%d, elapsed=%s}",
                                 mId, mPhase, mConfirmedPeers.size(), mNewPeers.length, getElapsedTime());
        }
    }

    private final AtomicReference<ChangeTransaction> mCurrentTransaction = new AtomicReference<>();
    private volatile Duration mDefaultTimeout = Duration.ofMinutes(5);

    /**
     * 开始新的成员变更
     * @param id 变更事务ID
     * @param leader Leader节点ID
     * @param oldPeers 旧配置节点列表
     * @param newPeers 新配置节点列表
     * @return true 如果成功开始，false 如果已有进行中的变更
     */
    public boolean startChange(long id, long leader, Collection<Long> oldPeers, Collection<Long> newPeers)
    {
        return startChange(id, leader, oldPeers, newPeers, mDefaultTimeout);
    }

    /**
     * 开始新的成员变更（带超时）
     */
    public boolean startChange(long id, long leader, Collection<Long> oldPeers, Collection<Long> newPeers, Duration timeout)
    {
        ChangeTransaction newTx = new ChangeTransaction(id, leader, oldPeers, newPeers, timeout);
        if(mCurrentTransaction.compareAndSet(null, newTx)) {
            _Logger.info("Started membership change: %s", newTx);
            return true;
        }
        _Logger.warning("Failed to start change %d, another change in progress: %s", id, mCurrentTransaction.get());
        return false;
    }

    /**
     * 进入 JOINT 阶段
     */
    public boolean enterJointPhase()
    {
        ChangeTransaction tx = mCurrentTransaction.get();
        if(tx == null) {
            _Logger.warning("Cannot enter JOINT phase: no active transaction");
            return false;
        }
        if(tx.getPhase() != Phase.PREPARING) {
            _Logger.warning("Cannot enter JOINT phase from %s", tx.getPhase());
            return false;
        }
        tx.setPhase(Phase.JOINT);
        _Logger.info("Entered JOINT phase for transaction %d", tx.getId());
        return true;
    }

    /**
     * 进入 COMMITTING 阶段
     */
    public boolean enterCommittingPhase()
    {
        ChangeTransaction tx = mCurrentTransaction.get();
        if(tx == null || tx.getPhase() != Phase.JOINT) {
            return false;
        }
        tx.setPhase(Phase.COMMITTING);
        _Logger.info("Entered COMMITTING phase for transaction %d", tx.getId());
        return true;
    }

    /**
     * 确认变更完成
     */
    public boolean confirmChange()
    {
        ChangeTransaction tx = mCurrentTransaction.get();
        if(tx == null || tx.getPhase() != Phase.COMMITTING) {
            return false;
        }
        tx.setPhase(Phase.CONFIRMED);
        _Logger.info("Membership change %d confirmed successfully", tx.getId());
        return true;
    }

    /**
     * 回滚变更
     */
    public boolean rollback(String reason)
    {
        ChangeTransaction tx = mCurrentTransaction.get();
        if(tx == null) {
            return false;
        }
        tx.setPhase(Phase.ROLLBACK);
        tx.setErrorMessage(reason);
        _Logger.warning("Rolling back membership change %d: %s", tx.getId(), reason);
        return true;
    }

    /**
     * 标记变更失败
     */
    public boolean fail(String reason)
    {
        ChangeTransaction tx = mCurrentTransaction.get();
        if(tx == null) {
            return false;
        }
        tx.setPhase(Phase.FAILED);
        tx.setErrorMessage(reason);
        _Logger.warning("Membership change %d failed: %s", tx.getId(), reason);
        return true;
    }

    /**
     * 完成并清理当前变更事务
     */
    public ChangeTransaction complete()
    {
        ChangeTransaction tx = mCurrentTransaction.getAndSet(null);
        if(tx != null) {
            _Logger.info("Membership change %d completed (phase=%s)", tx.getId(), tx.getPhase());
        }
        return tx;
    }

    /**
     * 确认节点响应
     */
    public boolean confirmPeer(long peer)
    {
        ChangeTransaction tx = mCurrentTransaction.get();
        if(tx == null) {
            return false;
        }
        return tx.confirmPeer(peer);
    }

    /**
     * 获取当前变更事务
     */
    public ChangeTransaction getCurrentTransaction()
    {
        return mCurrentTransaction.get();
    }

    /**
     * 是否有进行中的变更
     */
    public boolean hasActiveChange()
    {
        ChangeTransaction tx = mCurrentTransaction.get();
        if(tx == null) {
            return false;
        }
        // 检查是否超时
        if(tx.isTimeout() && (tx.getPhase() == Phase.PREPARING || tx.getPhase() == Phase.JOINT)) {
            _Logger.warning("Membership change %d timeout after %s", tx.getId(), tx.getElapsedTime());
            rollback("Timeout after " + tx.getElapsedTime());
            return false;
        }
        return tx.getPhase() != Phase.CONFIRMED && tx.getPhase() != Phase.FAILED && tx.getPhase() != Phase.ROLLBACK;
    }

    /**
     * 检查是否在 JOINT 阶段
     */
    public boolean isInJointPhase()
    {
        ChangeTransaction tx = mCurrentTransaction.get();
        return tx != null && tx.getPhase() == Phase.JOINT;
    }

    /**
     * 设置默认超时时间
     */
    public void setDefaultTimeout(Duration timeout)
    {
        mDefaultTimeout = timeout;
    }

    /**
     * 获取默认超时时间
     */
    public Duration getDefaultTimeout()
    {
        return mDefaultTimeout;
    }
}
