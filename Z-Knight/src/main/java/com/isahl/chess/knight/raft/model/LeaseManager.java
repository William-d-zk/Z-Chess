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

/**
 * Leader 租约管理器 - 实现 Lease Read 机制
 *
 * <p>租约机制原理: 1. Leader 通过定期心跳维护租约 2. 在租约期内，Leader 确信没有其他 Leader 能当选 3. 因此可以在租约期内直接处理读请求，无需等待 commit
 *
 * <p>租约有效期计算: - 租约开始时间: 收到多数派确认的时间 - 租约有效期: 租约开始时间 + 租约时长 - 租约时长通常设置为: election_timeout +
 * clock_drift_margin
 *
 * <p>安全性保证: - 在租约期内，其他节点不可能选举成功（因为 Leader 的心跳会阻止） - 因此读到的数据一定是强一致的
 *
 * @author william.d.zk
 */
public class LeaseManager {
  private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

  /** 租约状态 */
  public enum LeaseState {
    /** 无有效租约 */
    INVALID,

    /** 租约有效 */
    VALID,

    /** 租约即将过期（需要续期） */
    EXPIRING,

    /** 租约已过期 */
    EXPIRED
  }

  /** 租约信息 */
  public static class Lease {
    private final long mLeaderId;
    private final long mTerm;
    private final long mStartTime;
    private final long mDurationMs;
    private volatile long mLastRenewTime;
    private volatile int mConfirmCount;

    public Lease(long leaderId, long term, long durationMs) {
      mLeaderId = leaderId;
      mTerm = term;
      mDurationMs = durationMs;
      mStartTime = System.currentTimeMillis();
      mLastRenewTime = mStartTime;
      mConfirmCount = 0;
    }

    public long getLeaderId() {
      return mLeaderId;
    }

    public long getTerm() {
      return mTerm;
    }

    public long getStartTime() {
      return mStartTime;
    }

    public long getDurationMs() {
      return mDurationMs;
    }

    /** 获取租约过期时间 */
    public long getExpireTime() {
      return mLastRenewTime + mDurationMs;
    }

    /** 检查租约是否有效 */
    public boolean isValid() {
      return System.currentTimeMillis() < getExpireTime();
    }

    /** 获取剩余时间 */
    public long getRemainingMs() {
      long remaining = getExpireTime() - System.currentTimeMillis();
      return Math.max(0, remaining);
    }

    /** 检查是否即将过期（少于25%时间） */
    public boolean isExpiring() {
      return getRemainingMs() < mDurationMs / 4;
    }

    /** 续期 */
    public void renew() {
      mLastRenewTime = System.currentTimeMillis();
    }

    /** 增加确认计数 */
    public void addConfirm() {
      mConfirmCount++;
    }

    public int getConfirmCount() {
      return mConfirmCount;
    }

    @Override
    public String toString() {
      return String.format(
          "Lease{leader=%#x, term=%d, remaining=%dms, confirms=%d}",
          mLeaderId, mTerm, getRemainingMs(), mConfirmCount);
    }
  }

  private volatile Lease mCurrentLease;
  private volatile long mDefaultLeaseDurationMs = 5000; // 默认5秒
  private volatile boolean mEnabled = true;

  /** 创建新租约 */
  public Lease createLease(long leaderId, long term) {
    return createLease(leaderId, term, mDefaultLeaseDurationMs);
  }

  /** 创建新租约（指定时长） */
  public Lease createLease(long leaderId, long term, long durationMs) {
    Lease lease = new Lease(leaderId, term, durationMs);
    mCurrentLease = lease;
    _Logger.info("Created new lease: %s", lease);
    return lease;
  }

  /** 续期当前租约 */
  public boolean renewLease() {
    Lease lease = mCurrentLease;
    if (lease != null && lease.isValid()) {
      lease.renew();
      _Logger.debug("Renewed lease: %s", lease);
      return true;
    }
    return false;
  }

  /** 确认收到心跳响应，增加确认计数 */
  public boolean confirmHeartbeat(long term) {
    Lease lease = mCurrentLease;
    if (lease != null && lease.getTerm() == term && lease.isValid()) {
      lease.addConfirm();
      return true;
    }
    return false;
  }

  /** 获取当前租约状态 */
  public LeaseState getLeaseState() {
    Lease lease = mCurrentLease;
    if (lease == null) {
      return LeaseState.INVALID;
    }
    if (!lease.isValid()) {
      return LeaseState.EXPIRED;
    }
    if (lease.isExpiring()) {
      return LeaseState.EXPIRING;
    }
    return LeaseState.VALID;
  }

  /** 检查是否可以执行 Lease Read */
  public boolean canLeaseRead(long term) {
    if (!mEnabled) {
      return false;
    }

    Lease lease = mCurrentLease;
    if (lease == null) {
      return false;
    }

    // 检查 term 是否匹配
    if (lease.getTerm() != term) {
      return false;
    }

    // 检查租约是否有效
    return lease.isValid();
  }

  /** 获取当前租约 */
  public Lease getCurrentLease() {
    return mCurrentLease;
  }

  /** 使租约失效 */
  public void invalidateLease(String reason) {
    Lease lease = mCurrentLease;
    if (lease != null) {
      _Logger.info("Invalidating lease: %s, reason: %s", lease, reason);
      // 将过期时间设为0
      lease.mLastRenewTime = 0;
    }
  }

  /** 清除租约 */
  public void clearLease() {
    mCurrentLease = null;
  }

  /** 设置默认租约时长 */
  public void setDefaultLeaseDuration(long durationMs) {
    mDefaultLeaseDurationMs = durationMs;
  }

  public long getDefaultLeaseDuration() {
    return mDefaultLeaseDurationMs;
  }

  /** 启用/禁用 Lease Read */
  public void setEnabled(boolean enabled) {
    mEnabled = enabled;
    _Logger.info("Lease read %s", enabled ? "enabled" : "disabled");
  }

  public boolean isEnabled() {
    return mEnabled;
  }

  /** 获取租约统计信息 */
  public String getStats() {
    Lease lease = mCurrentLease;
    if (lease == null) {
      return "Lease: none";
    }
    return String.format("Lease: %s, state=%s", lease, getLeaseState());
  }

  @Override
  public String toString() {
    return String.format(
        "LeaseManager{enabled=%s, duration=%dms, current=%s}",
        mEnabled, mDefaultLeaseDurationMs, mCurrentLease);
  }
}
