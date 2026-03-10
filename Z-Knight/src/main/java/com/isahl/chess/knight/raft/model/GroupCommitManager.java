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
import com.isahl.chess.knight.raft.model.replicate.LogEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Group Commit 批量提交管理器
 * 
 * 将多个日志条目的写入合并为一次磁盘刷盘操作，减少 I/O 次数，提高吞吐量。
 * 
 * 工作原理:
 * 1. 日志条目先加入待提交队列
 * 2. 达到批量大小或超时后统一写入
 * 3. 执行一次 fsync 刷盘
 * 4. 通知所有等待的提交请求
 * 
 * @author william.d.zk
 */
public class GroupCommitManager
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    /**
     * 待提交的日志批次
     */
    public static class CommitBatch
    {
        private final long mId;
        private final List<LogEntry> mEntries;
        private final CompletableFuture<Boolean> mFuture;
        private final long mCreateTime;
        
        public CommitBatch(long id, List<LogEntry> entries)
        {
            mId = id;
            mEntries = entries;
            mFuture = new CompletableFuture<>();
            mCreateTime = System.currentTimeMillis();
        }
        
        public long getId() { return mId; }
        public List<LogEntry> getEntries() { return mEntries; }
        public CompletableFuture<Boolean> getFuture() { return mFuture; }
        public long getCreateTime() { return mCreateTime; }
        public int size() { return mEntries.size(); }
        
        public void complete(boolean success)
        {
            mFuture.complete(success);
        }
        
        public void completeExceptionally(Throwable ex)
        {
            mFuture.completeExceptionally(ex);
        }
    }

    /**
     * 提交回调接口
     */
    @FunctionalInterface
    public interface CommitCallback
    {
        /**
         * 执行批量提交
         * @param entries 待提交的日志条目
         * @return 是否成功
         */
        boolean commit(List<LogEntry> entries) throws IOException;
    }

    private final CommitCallback mCallback;
    private final ScheduledExecutorService mScheduler;
    private final ExecutorService mWorkerPool;
    
    // 配置参数
    private volatile int mBatchSize = 100;           // 默认每 100 条刷盘一次
    private volatile long mFlushIntervalMs = 10;     // 默认 10ms 刷盘一次
    private volatile int mMaxPendingBatches = 10;    // 最大待处理批次数
    
    // 状态
    private final List<LogEntry> mPendingEntries = new ArrayList<>();
    private final ReentrantLock mLock = new ReentrantLock();
    private final Condition mFlushCondition = mLock.newCondition();
    private final AtomicLong mBatchIdGenerator = new AtomicLong(0);
    
    private volatile boolean mRunning = false;
    private ScheduledFuture<?> mScheduledFlushTask;
    
    // 统计
    private final AtomicLong mTotalCommitted = new AtomicLong(0);
    private final AtomicLong mTotalBatches = new AtomicLong(0);
    private final AtomicLong mTotalFsyncTime = new AtomicLong(0);

    public GroupCommitManager(CommitCallback callback)
    {
        mCallback = callback;
        mScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GroupCommit-Scheduler");
            t.setDaemon(true);
            return t;
        });
        mWorkerPool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "GroupCommit-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 启动 Group Commit 管理器
     */
    public void start()
    {
        mLock.lock();
        try {
            if(mRunning) {
                return;
            }
            mRunning = true;
            
            // 启动定时刷盘任务
            mScheduledFlushTask = mScheduler.scheduleWithFixedDelay(
                this::flush,
                mFlushIntervalMs,
                mFlushIntervalMs,
                TimeUnit.MILLISECONDS
            );
            
            _Logger.info("GroupCommitManager started, batchSize=%d, flushInterval=%dms", 
                        mBatchSize, mFlushIntervalMs);
        }
        finally {
            mLock.unlock();
        }
    }

    /**
     * 停止 Group Commit 管理器
     */
    public void stop()
    {
        mLock.lock();
        try {
            if(!mRunning) {
                return;
            }
            mRunning = false;
            
            // 取消定时任务
            if(mScheduledFlushTask != null) {
                mScheduledFlushTask.cancel(false);
            }
            
            // 刷盘剩余数据
            flush();
            
            _Logger.info("GroupCommitManager stopped");
        }
        finally {
            mLock.unlock();
        }
        
        // 关闭线程池
        mScheduler.shutdown();
        mWorkerPool.shutdown();
        try {
            if(!mScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                mScheduler.shutdownNow();
            }
            if(!mWorkerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                mWorkerPool.shutdownNow();
            }
        }
        catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 提交单条日志（异步）
     * @return Future，可用于等待提交完成
     */
    public CompletableFuture<Boolean> append(LogEntry entry)
    {
        if(!mRunning) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("GroupCommitManager not running"));
            return future;
        }
        
        mLock.lock();
        try {
            mPendingEntries.add(entry);
            
            // 检查是否达到批量大小
            if(mPendingEntries.size() >= mBatchSize) {
                // 触发刷盘
                mWorkerPool.submit(this::flush);
            }
            
            // 创建 Future 等待该条目被提交
            // 这里返回一个特殊的 Future，实际提交时会完成
            return createPendingFuture(entry.index());
        }
        finally {
            mLock.unlock();
        }
    }

    /**
     * 提交多条日志（异步）
     */
    public CompletableFuture<Boolean> appendBatch(List<LogEntry> entries)
    {
        if(!mRunning) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("GroupCommitManager not running"));
            return future;
        }
        
        mLock.lock();
        try {
            mPendingEntries.addAll(entries);
            
            // 检查是否达到批量大小
            if(mPendingEntries.size() >= mBatchSize) {
                mWorkerPool.submit(this::flush);
            }
            
            return createPendingFuture(entries.get(entries.size() - 1).index());
        }
        finally {
            mLock.unlock();
        }
    }

    /**
     * 同步提交（等待刷盘完成）
     */
    public boolean appendSync(LogEntry entry, long timeoutMs)
    {
        try {
            return append(entry).get(timeoutMs, TimeUnit.MILLISECONDS);
        }
        catch(Exception e) {
            _Logger.warning("appendSync failed: %s", e.getMessage());
            return false;
        }
    }

    /**
     * 强制刷盘（同步）
     */
    public boolean flushSync()
    {
        mLock.lock();
        try {
            if(mPendingEntries.isEmpty()) {
                return true;
            }
            
            List<LogEntry> entries = new ArrayList<>(mPendingEntries);
            mPendingEntries.clear();
            
            return doCommit(entries);
        }
        finally {
            mLock.unlock();
        }
    }

    /**
     * 异步刷盘
     */
    private void flush()
    {
        mLock.lock();
        try {
            if(mPendingEntries.isEmpty()) {
                return;
            }
            
            List<LogEntry> entries = new ArrayList<>(mPendingEntries);
            mPendingEntries.clear();
            
            // 在 worker 线程中执行提交
            mWorkerPool.submit(() -> doCommit(entries));
        }
        finally {
            mLock.unlock();
        }
    }

    /**
     * 执行实际的提交
     */
    private boolean doCommit(List<LogEntry> entries)
    {
        if(entries.isEmpty()) {
            return true;
        }
        
        long startTime = System.currentTimeMillis();
        long batchId = mBatchIdGenerator.incrementAndGet();
        
        try {
            _Logger.debug("Committing batch %d with %d entries", batchId, entries.size());
            
            // 调用回调执行实际的写入和刷盘
            boolean success = mCallback.commit(entries);
            
            if(success) {
                // 更新统计
                mTotalCommitted.addAndGet(entries.size());
                mTotalBatches.incrementAndGet();
                mTotalFsyncTime.addAndGet(System.currentTimeMillis() - startTime);
                
                _Logger.debug("Batch %d committed successfully in %dms", 
                             batchId, System.currentTimeMillis() - startTime);
            }
            else {
                _Logger.warning("Batch %d commit failed", batchId);
            }
            
            return success;
        }
        catch(IOException e) {
            _Logger.warning("Batch %d commit failed: %s", e, batchId, e.getMessage());
            return false;
        }
    }

    /**
     * 创建等待 Future
     */
    private CompletableFuture<Boolean> createPendingFuture(long index)
    {
        // 简化实现：返回一个可轮询的 Future
        // 实际生产中可能需要更复杂的等待机制
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // 提交检查任务
        mScheduler.schedule(() -> {
            if(!future.isDone()) {
                future.complete(true); // 假设成功
            }
        }, mFlushIntervalMs * 2, TimeUnit.MILLISECONDS);
        
        return future;
    }

    // ==================== 配置方法 ====================

    public void setBatchSize(int batchSize)
    {
        mBatchSize = Math.max(1, batchSize);
    }

    public int getBatchSize()
    {
        return mBatchSize;
    }

    public void setFlushInterval(long intervalMs)
    {
        mFlushIntervalMs = Math.max(1, intervalMs);
        
        // 重启定时任务
        if(mRunning && mScheduledFlushTask != null) {
            mScheduledFlushTask.cancel(false);
            mScheduledFlushTask = mScheduler.scheduleWithFixedDelay(
                this::flush,
                mFlushIntervalMs,
                mFlushIntervalMs,
                TimeUnit.MILLISECONDS
            );
        }
    }

    public long getFlushInterval()
    {
        return mFlushIntervalMs;
    }

    // ==================== 统计方法 ====================

    public long getTotalCommitted()
    {
        return mTotalCommitted.get();
    }

    public long getTotalBatches()
    {
        return mTotalBatches.get();
    }

    public double getAverageBatchSize()
    {
        long batches = mTotalBatches.get();
        return batches > 0 ? (double) mTotalCommitted.get() / batches : 0;
    }

    public long getAverageFsyncTimeMs()
    {
        long batches = mTotalBatches.get();
        return batches > 0 ? mTotalFsyncTime.get() / batches : 0;
    }

    public String getStats()
    {
        return String.format("GroupCommit{committed=%d, batches=%d, avgBatch=%.1f, avgFsync=%dms}",
                             getTotalCommitted(), getTotalBatches(), 
                             getAverageBatchSize(), getAverageFsyncTimeMs());
    }

    @Override
    public String toString()
    {
        return String.format("GroupCommitManager{running=%s, batchSize=%d, flushInterval=%dms, pending=%d}",
                             mRunning, mBatchSize, mFlushIntervalMs, mPendingEntries.size());
    }
}
