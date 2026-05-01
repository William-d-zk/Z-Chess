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

import com.isahl.chess.knight.raft.features.IRaftMachine;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Leader 转让管理器
 *
 * <p>管理 Leader 转让过程的状态和超时
 *
 * @author william.d.zk
 */
public class LeadershipTransferManager {
  private final Logger _Logger =
      LoggerFactory.getLogger("cluster.knight." + getClass().getSimpleName());

  /** 转让状态 */
  public enum State {
    /** 空闲状态 */
    IDLE,

    /** 正在转让中 */
    TRANSFERRING,

    /** 等待目标节点追上日志 */
    WAITING_CATCH_UP,

    /** 转让成功 */
    SUCCESS,

    /** 转让失败 */
    FAILED,

    /** 转让超时 */
    TIMEOUT
  }

  /** 转让事务 */
  public static class TransferTransaction {
    private final long mId;
    private final long mTargetPeer;
    private final long mSourceLeader;
    private final long mStartTerm;
    private final Instant mStartTime;
    private final Duration mTimeout;

    private volatile State mState;
    private volatile String mErrorMessage;
    private volatile int mRetryCount;

    public TransferTransaction(
        long id, long targetPeer, long sourceLeader, long startTerm, Duration timeout) {
      mId = id;
      mTargetPeer = targetPeer;
      mSourceLeader = sourceLeader;
      mStartTerm = startTerm;
      mStartTime = Instant.now();
      mTimeout = timeout;
      mState = State.TRANSFERRING;
      mRetryCount = 0;
    }

    public long getId() {
      return mId;
    }

    public long getTargetPeer() {
      return mTargetPeer;
    }

    public long getSourceLeader() {
      return mSourceLeader;
    }

    public long getStartTerm() {
      return mStartTerm;
    }

    public State getState() {
      return mState;
    }

    public void setState(State state) {
      mState = state;
    }

    public String getErrorMessage() {
      return mErrorMessage;
    }

    public void setErrorMessage(String message) {
      mErrorMessage = message;
    }

    public int getRetryCount() {
      return mRetryCount;
    }

    public void incrementRetry() {
      mRetryCount++;
    }

    public boolean isTimeout() {
      return Duration.between(mStartTime, Instant.now()).compareTo(mTimeout) > 0;
    }

    public Duration getElapsedTime() {
      return Duration.between(mStartTime, Instant.now());
    }

    public boolean isTerminal() {
      return mState == State.SUCCESS || mState == State.FAILED || mState == State.TIMEOUT;
    }

    @Override
    public String toString() {
      return String.format(
          "TransferTransaction{id=%d, target=%#x, state=%s, elapsed=%s, retries=%d}",
          mId, mTargetPeer, mState, getElapsedTime(), mRetryCount);
    }
  }

  private final AtomicReference<TransferTransaction> mCurrentTransaction = new AtomicReference<>();
  private volatile Duration mDefaultTimeout = Duration.ofSeconds(30);
  private volatile int mMaxRetries = 3;

  /**
   * 开始 Leader 转让
   *
   * @param id 事务ID
   * @param targetPeer 目标节点
   * @param sourceLeader 当前 Leader
   * @param startTerm 当前 term
   * @return true 如果成功开始
   */
  public boolean startTransfer(long id, long targetPeer, long sourceLeader, long startTerm) {
    return startTransfer(id, targetPeer, sourceLeader, startTerm, mDefaultTimeout);
  }

  /** 开始 Leader 转让（带超时） */
  public boolean startTransfer(
      long id, long targetPeer, long sourceLeader, long startTerm, Duration timeout) {
    TransferTransaction newTx =
        new TransferTransaction(id, targetPeer, sourceLeader, startTerm, timeout);
    if (mCurrentTransaction.compareAndSet(null, newTx)) {
      _Logger.info("Started leadership transfer: %s", newTx);
      return true;
    }
    _Logger.warn(
        "Failed to start transfer %d, another transfer in progress: %s",
        id, mCurrentTransaction.get());
    return false;
  }

  /** 标记为等待追上日志 */
  public boolean enterWaitingCatchUp() {
    TransferTransaction tx = mCurrentTransaction.get();
    if (tx == null || tx.getState() != State.TRANSFERRING) {
      return false;
    }
    tx.setState(State.WAITING_CATCH_UP);
    _Logger.debug("Transfer %d: waiting for target to catch up", tx.getId());
    return true;
  }

  /** 标记转让成功 */
  public boolean markSuccess() {
    TransferTransaction tx = mCurrentTransaction.get();
    if (tx == null) {
      return false;
    }
    tx.setState(State.SUCCESS);
    _Logger.info("Leadership transfer %d completed successfully", tx.getId());
    return true;
  }

  /** 标记转让失败 */
  public boolean markFailed(String reason) {
    TransferTransaction tx = mCurrentTransaction.get();
    if (tx == null) {
      return false;
    }
    tx.setState(State.FAILED);
    tx.setErrorMessage(reason);
    _Logger.warn("Leadership transfer %d failed: %s", tx.getId(), reason);
    return true;
  }

  /** 标记转让超时 */
  public boolean markTimeout() {
    TransferTransaction tx = mCurrentTransaction.get();
    if (tx == null) {
      return false;
    }
    tx.setState(State.TIMEOUT);
    _Logger.warn("Leadership transfer %d timeout after %s", tx.getId(), tx.getElapsedTime());
    return true;
  }

  /** 完成并清理当前转让事务 */
  public TransferTransaction complete() {
    TransferTransaction tx = mCurrentTransaction.getAndSet(null);
    if (tx != null) {
      _Logger.info("Leadership transfer %d completed (state=%s)", tx.getId(), tx.getState());
    }
    return tx;
  }

  /** 检查是否需要重试 */
  public boolean shouldRetry() {
    TransferTransaction tx = mCurrentTransaction.get();
    if (tx == null) {
      return false;
    }
    if (tx.getRetryCount() < mMaxRetries && !tx.isTimeout()) {
      tx.incrementRetry();
      tx.setState(State.TRANSFERRING);
      return true;
    }
    return false;
  }

  /** 获取当前转让事务 */
  public TransferTransaction getCurrentTransaction() {
    return mCurrentTransaction.get();
  }

  /** 是否有进行中的转让 */
  public boolean hasActiveTransfer() {
    TransferTransaction tx = mCurrentTransaction.get();
    if (tx == null) {
      return false;
    }
    // 检查是否超时
    if (tx.isTimeout() && !tx.isTerminal()) {
      markTimeout();
      return false;
    }
    return !tx.isTerminal();
  }

  /** 是否正在等待目标节点追上 */
  public boolean isWaitingCatchUp() {
    TransferTransaction tx = mCurrentTransaction.get();
    return tx != null && tx.getState() == State.WAITING_CATCH_UP;
  }

  /** 设置默认超时时间 */
  public void setDefaultTimeout(Duration timeout) {
    mDefaultTimeout = timeout;
  }

  /** 获取默认超时时间 */
  public Duration getDefaultTimeout() {
    return mDefaultTimeout;
  }

  /** 设置最大重试次数 */
  public void setMaxRetries(int maxRetries) {
    mMaxRetries = maxRetries;
  }

  /** 获取最大重试次数 */
  public int getMaxRetries() {
    return mMaxRetries;
  }

  /** 选择最佳的转让目标 选择规则：日志最新（index 最大）的 Follower */
  public static long selectBestTarget(Iterable<? extends IRaftMachine> peers, long selfPeer) {
    long bestPeer = 0;
    long bestIndex = -1;

    for (IRaftMachine peer : peers) {
      // 跳过自己
      if (peer.peer() == selfPeer) {
        continue;
      }

      // 选择日志最新的
      if (peer.index() > bestIndex) {
        bestIndex = peer.index();
        bestPeer = peer.peer();
      }
    }

    return bestPeer;
  }
}
