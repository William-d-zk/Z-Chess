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
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pipeline 复制管理器
 *
 * <p>管理 Leader 向 Followers 的并发日志复制，允许连续发送多个 AppendEntries 而不等待每个响应，提高吞吐量。
 *
 * <p>核心概念: - InflightWindow: 每个 Follower 的 inflight 日志窗口 - MaxInflight: 最大并发 inflight 日志条目数 -
 * NextIndex: 下一个要发送的日志索引
 *
 * @author william.d.zk
 */
public class PipelineReplicationManager {
  private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

  /** 单个 Follower 的 Inflight 窗口 */
  public static class InflightWindow {
    /** Inflight 条目 */
    public static class Entry {
      private final long mIndex;
      private final long mTerm;
      private final long mSendTime;
      private volatile boolean mAcked;

      public Entry(long index, long term) {
        mIndex = index;
        mTerm = term;
        mSendTime = System.currentTimeMillis();
        mAcked = false;
      }

      public long getIndex() {
        return mIndex;
      }

      public long getTerm() {
        return mTerm;
      }

      public long getSendTime() {
        return mSendTime;
      }

      public boolean isAcked() {
        return mAcked;
      }

      public void ack() {
        mAcked = true;
      }

      public long getLatency() {
        return System.currentTimeMillis() - mSendTime;
      }

      @Override
      public String toString() {
        return String.format(
            "Entry{%d@%d, acked=%s, latency=%dms}", mIndex, mTerm, mAcked, getLatency());
      }
    }

    private final long mPeerId;
    private final Queue<Entry> mInflightQueue;
    private volatile int mMaxInflight;
    private volatile long mNextIndex;
    private volatile long mMatchIndex;
    private volatile boolean mPaused;
    private volatile long mLastSendTime;

    public InflightWindow(long peerId, int maxInflight, long nextIndex) {
      mPeerId = peerId;
      mMaxInflight = maxInflight;
      mNextIndex = nextIndex;
      mMatchIndex = 0;
      mInflightQueue = new LinkedList<>();
      mPaused = false;
      mLastSendTime = 0;
    }

    /** 检查是否可以发送更多日志 */
    public boolean canSend() {
      if (mPaused) {
        return false;
      }
      // 清理已确认的条目
      cleanupAcked();
      return mInflightQueue.size() < mMaxInflight;
    }

    /** 获取下一个要发送的索引 */
    public long getNextIndex() {
      return mNextIndex;
    }

    /** 添加 inflight 条目 */
    public void addInflight(long index, long term) {
      mInflightQueue.offer(new Entry(index, term));
      mNextIndex = index + 1;
      mLastSendTime = System.currentTimeMillis();
    }

    /** 批量添加 inflight 条目 */
    public void addInflightBatch(long startIndex, long endIndex, long term) {
      for (long i = startIndex; i <= endIndex; i++) {
        mInflightQueue.offer(new Entry(i, term));
      }
      mNextIndex = endIndex + 1;
      mLastSendTime = System.currentTimeMillis();
    }

    /** 确认索引及之前的所有条目 */
    public void ack(long index) {
      for (Entry entry : mInflightQueue) {
        if (entry.getIndex() <= index) {
          entry.ack();
        }
      }
      if (index > mMatchIndex) {
        mMatchIndex = index;
      }
      // 清理已确认的
      cleanupAcked();
    }

    /**
     * 处理拒绝（日志不匹配）
     *
     * @return 建议的下一个索引
     */
    public long reject(long index, long indexTerm) {
      // 清空所有 inflight，需要重新发送
      mInflightQueue.clear();
      mPaused = false;

      // 回退 nextIndex
      if (index > 0) {
        mNextIndex = index;
      } else {
        mNextIndex = Math.max(1, mNextIndex - 1);
      }

      return mNextIndex;
    }

    /** 清理已确认的条目 */
    private void cleanupAcked() {
      while (!mInflightQueue.isEmpty() && mInflightQueue.peek().isAcked()) {
        mInflightQueue.poll();
      }
    }

    /** 暂停发送（如遇到错误） */
    public void pause() {
      mPaused = true;
    }

    /** 恢复发送 */
    public void resume() {
      mPaused = false;
    }

    /** 检查是否暂停 */
    public boolean isPaused() {
      return mPaused;
    }

    /** 获取当前 inflight 数量 */
    public int getInflightCount() {
      cleanupAcked();
      return mInflightQueue.size();
    }

    /** 获取 match index */
    public long getMatchIndex() {
      return mMatchIndex;
    }

    /** 获取第一个 inflight 索引 */
    public long getFirstInflightIndex() {
      Entry first = mInflightQueue.peek();
      return first != null ? first.getIndex() : mNextIndex;
    }

    /** 获取最后发送时间 */
    public long getLastSendTime() {
      return mLastSendTime;
    }

    /** 检查是否有超时的 inflight */
    public boolean hasTimeout(long timeoutMs) {
      long now = System.currentTimeMillis();
      for (Entry entry : mInflightQueue) {
        if (!entry.isAcked() && (now - entry.getSendTime()) > timeoutMs) {
          return true;
        }
      }
      return false;
    }

    /** 更新配置 */
    public void setMaxInflight(int maxInflight) {
      mMaxInflight = maxInflight;
    }

    public long getPeerId() {
      return mPeerId;
    }

    @Override
    public String toString() {
      cleanupAcked();
      return String.format(
          "InflightWindow{peer=%#x, next=%d, match=%d, inflight=%d/%d, paused=%s}",
          mPeerId, mNextIndex, mMatchIndex, mInflightQueue.size(), mMaxInflight, mPaused);
    }
  }

  private final Map<Long, InflightWindow> mWindows = new ConcurrentHashMap<>();
  private volatile int mDefaultMaxInflight;
  private volatile long mInflightTimeoutMs;

  /** 默认构造函数，使用默认配置值 */
  public PipelineReplicationManager() {
    this(100, 5000);
  }

  /**
   * 带配置的构造函数
   *
   * @param maxInflight 最大 inflight 数
   * @param inflightTimeoutMs inflight 超时时间（毫秒）
   */
  public PipelineReplicationManager(int maxInflight, long inflightTimeoutMs) {
    this.mDefaultMaxInflight = maxInflight;
    this.mInflightTimeoutMs = inflightTimeoutMs;
  }

  /** 获取或创建 InflightWindow */
  public InflightWindow getOrCreateWindow(long peerId, long nextIndex) {
    return mWindows.computeIfAbsent(
        peerId, id -> new InflightWindow(id, mDefaultMaxInflight, nextIndex));
  }

  /** 获取 InflightWindow */
  public InflightWindow getWindow(long peerId) {
    return mWindows.get(peerId);
  }

  /** 移除 InflightWindow */
  public void removeWindow(long peerId) {
    mWindows.remove(peerId);
  }

  /** 检查是否可以向指定 Follower 发送更多日志 */
  public boolean canSendTo(long peerId) {
    InflightWindow window = mWindows.get(peerId);
    if (window == null) {
      return true;
    }
    return window.canSend();
  }

  /** 获取下一个要发送的索引 */
  public long getNextIndex(long peerId) {
    InflightWindow window = mWindows.get(peerId);
    if (window == null) {
      return 0; // 表示需要初始化
    }
    return window.getNextIndex();
  }

  /** 确认收到响应 */
  public void onAck(long peerId, long index) {
    InflightWindow window = mWindows.get(peerId);
    if (window != null) {
      window.ack(index);
    }
  }

  /**
   * 处理拒绝响应
   *
   * @return 建议的下一个索引
   */
  public long onReject(long peerId, long index, long indexTerm) {
    InflightWindow window = mWindows.get(peerId);
    if (window != null) {
      return window.reject(index, indexTerm);
    }
    return Math.max(1, index - 1);
  }

  /** 记录发送 */
  public void recordSend(long peerId, long index, long term) {
    InflightWindow window = getOrCreateWindow(peerId, index);
    window.addInflight(index, term);
  }

  /** 批量记录发送 */
  public void recordSendBatch(long peerId, long startIndex, long endIndex, long term) {
    InflightWindow window = getOrCreateWindow(peerId, startIndex);
    window.addInflightBatch(startIndex, endIndex, term);
  }

  /** 暂停指定 Follower 的复制 */
  public void pause(long peerId) {
    InflightWindow window = mWindows.get(peerId);
    if (window != null) {
      window.pause();
    }
  }

  /** 恢复指定 Follower 的复制 */
  public void resume(long peerId) {
    InflightWindow window = mWindows.get(peerId);
    if (window != null) {
      window.resume();
    }
  }

  /** 检查是否有超时的 inflight */
  public boolean hasTimeout(long peerId) {
    InflightWindow window = mWindows.get(peerId);
    if (window != null) {
      return window.hasTimeout(mInflightTimeoutMs);
    }
    return false;
  }

  /** 获取指定窗口的 inflight 数量 */
  public int getInflightCount(long peerId) {
    InflightWindow window = mWindows.get(peerId);
    if (window != null) {
      return window.getInflightCount();
    }
    return 0;
  }

  /** 清空所有窗口 */
  public void clear() {
    mWindows.clear();
  }

  /** 设置默认最大 inflight 数 */
  public void setDefaultMaxInflight(int maxInflight) {
    mDefaultMaxInflight = maxInflight;
  }

  public int getDefaultMaxInflight() {
    return mDefaultMaxInflight;
  }

  /** 设置 inflight 超时时间 */
  public void setInflightTimeout(long timeoutMs) {
    mInflightTimeoutMs = timeoutMs;
  }

  public long getInflightTimeout() {
    return mInflightTimeoutMs;
  }

  /** 获取所有窗口状态（用于调试） */
  public Map<Long, InflightWindow> getAllWindows() {
    return new ConcurrentHashMap<>(mWindows);
  }

  @Override
  public String toString() {
    return String.format(
        "PipelineReplicationManager{windows=%d, maxInflight=%d, timeout=%dms}",
        mWindows.size(), mDefaultMaxInflight, mInflightTimeoutMs);
  }
}
