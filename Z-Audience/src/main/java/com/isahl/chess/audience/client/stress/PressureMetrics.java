/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
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

package com.isahl.chess.audience.client.stress;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 压力测试性能指标收集器
 * 线程安全，支持高并发场景下的性能统计
 */
public class PressureMetrics
{
    // ==================== 连接指标 ====================
    private final LongAdder totalConnections = new LongAdder();
    private final LongAdder activeConnections = new LongAdder();
    private final LongAdder failedConnections = new LongAdder();
    private final LongAdder closedConnections = new LongAdder();

    // ==================== 请求指标 ====================
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();
    private final LongAdder timeoutRequests = new LongAdder();

    // ==================== 延迟指标（纳秒级精度）====================
    private final LongAdder totalLatencyNanos = new LongAdder();
    private final AtomicLong minLatencyNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatencyNanos = new AtomicLong(Long.MIN_VALUE);

    // ==================== 吞吐量指标 ====================
    private final LongAdder totalBytesSent = new LongAdder();
    private final LongAdder totalBytesReceived = new LongAdder();

    // ==================== 时间记录 ====================
    private volatile Instant testStartTime;
    private volatile Instant testEndTime;
    private volatile boolean running = false;

    // ==================== 连接指标操作 ====================

    public void recordConnectionEstablished()
    {
        totalConnections.increment();
        activeConnections.increment();
    }

    public void recordConnectionFailed()
    {
        failedConnections.increment();
    }

    public void recordConnectionClosed()
    {
        activeConnections.decrement();
        closedConnections.increment();
    }

    // ==================== 请求指标操作 ====================

    public void recordRequestStart()
    {
        totalRequests.increment();
    }

    public void recordRequestSuccess(long latencyNanos)
    {
        successRequests.increment();
        recordLatency(latencyNanos);
    }

    public void recordRequestFailed()
    {
        failedRequests.increment();
    }

    public void recordRequestTimeout()
    {
        timeoutRequests.increment();
    }

    // ==================== 延迟指标操作 ====================

    private void recordLatency(long latencyNanos)
    {
        totalLatencyNanos.add(latencyNanos);

        // 更新最小延迟
        long currentMin;
        while((currentMin = minLatencyNanos.get()) > latencyNanos) {
            if(minLatencyNanos.compareAndSet(currentMin, latencyNanos)) {
                break;
            }
        }

        // 更新最大延迟
        long currentMax;
        while((currentMax = maxLatencyNanos.get()) < latencyNanos) {
            if(maxLatencyNanos.compareAndSet(currentMax, latencyNanos)) {
                break;
            }
        }
    }

    // ==================== 吞吐量指标操作 ====================

    public void recordBytesSent(long bytes)
    {
        totalBytesSent.add(bytes);
    }

    public void recordBytesReceived(long bytes)
    {
        totalBytesReceived.add(bytes);
    }

    // ==================== 测试生命周期 ====================

    public void startTest()
    {
        testStartTime = Instant.now();
        running = true;
    }

    public void endTest()
    {
        testEndTime = Instant.now();
        running = false;
    }

    public boolean isRunning()
    {
        return running;
    }

    // ==================== 统计计算 ====================

    public Snapshot getSnapshot()
    {
        return new Snapshot(this);
    }

    /**
     * 获取测试持续时间（毫秒）
     */
    public long getDurationMillis()
    {
        if(testStartTime == null) {
            return 0;
        }
        Instant end = testEndTime != null ? testEndTime : Instant.now();
        return end.toEpochMilli() - testStartTime.toEpochMilli();
    }

    // ==================== Getters ====================

    public long getTotalConnections()
    {
        return totalConnections.sum();
    }

    public long getActiveConnections()
    {
        return activeConnections.sum();
    }

    public long getFailedConnections()
    {
        return failedConnections.sum();
    }

    public long getClosedConnections()
    {
        return closedConnections.sum();
    }

    public long getTotalRequests()
    {
        return totalRequests.sum();
    }

    public long getSuccessRequests()
    {
        return successRequests.sum();
    }

    public long getFailedRequests()
    {
        return failedRequests.sum();
    }

    public long getTimeoutRequests()
    {
        return timeoutRequests.sum();
    }

    public long getTotalLatencyNanos()
    {
        return totalLatencyNanos.sum();
    }

    public long getMinLatencyNanos()
    {
        return minLatencyNanos.get() == Long.MAX_VALUE ? 0 : minLatencyNanos.get();
    }

    public long getMaxLatencyNanos()
    {
        return maxLatencyNanos.get() == Long.MIN_VALUE ? 0 : maxLatencyNanos.get();
    }

    public long getTotalBytesSent()
    {
        return totalBytesSent.sum();
    }

    public long getTotalBytesReceived()
    {
        return totalBytesReceived.sum();
    }

    // ==================== 指标快照类 ====================

    /**
     * 性能指标快照，用于报告生成
     */
    public static class Snapshot
    {
        public final long timestamp = System.currentTimeMillis();

        // 连接指标
        public final long totalConnections;
        public final long activeConnections;
        public final long failedConnections;
        public final long closedConnections;

        // 请求指标
        public final long totalRequests;
        public final long successRequests;
        public final long failedRequests;
        public final long timeoutRequests;

        // 延迟指标（毫秒）
        public final double avgLatencyMs;
        public final long minLatencyMs;
        public final long maxLatencyMs;

        // 吞吐量指标
        public final double qps;
        public final double throughputMbps;

        // 成功率
        public final double successRate;

        // 测试持续时间
        public final long durationMillis;

        Snapshot(PressureMetrics metrics)
        {
            this.totalConnections = metrics.getTotalConnections();
            this.activeConnections = metrics.getActiveConnections();
            this.failedConnections = metrics.getFailedConnections();
            this.closedConnections = metrics.getClosedConnections();

            this.totalRequests = metrics.getTotalRequests();
            this.successRequests = metrics.getSuccessRequests();
            this.failedRequests = metrics.getFailedRequests();
            this.timeoutRequests = metrics.getTimeoutRequests();

            // 计算延迟（转换为毫秒）
            long successCount = this.successRequests;
            this.avgLatencyMs = successCount > 0 
                ? (metrics.getTotalLatencyNanos() / successCount) / 1_000_000.0 
                : 0;
            this.minLatencyMs = metrics.getMinLatencyNanos() / 1_000_000;
            this.maxLatencyMs = metrics.getMaxLatencyNanos() / 1_000_000;

            // 计算 QPS
            this.durationMillis = metrics.getDurationMillis();
            this.qps = durationMillis > 0 
                ? (this.successRequests * 1000.0 / durationMillis) 
                : 0;

            // 计算吞吐量（Mbps）
            long totalBytes = metrics.getTotalBytesSent() + metrics.getTotalBytesReceived();
            this.throughputMbps = durationMillis > 0 
                ? (totalBytes * 8.0 / 1000 / 1000 / (durationMillis / 1000.0)) 
                : 0;

            // 计算成功率
            this.successRate = this.totalRequests > 0 
                ? (this.successRequests * 100.0 / this.totalRequests) 
                : 0;
        }

        @Override
        public String toString()
        {
            return String.format(
                "PressureMetrics[duration=%ds, connections=%d/%d/%d, requests=%d/%d/%d, " +
                "qps=%.1f, latency=%.2f/%d/%d ms, success=%.2f%%, throughput=%.2f Mbps]",
                durationMillis / 1000,
                activeConnections, totalConnections, failedConnections,
                totalRequests, successRequests, failedRequests,
                qps, avgLatencyMs, minLatencyMs, maxLatencyMs,
                successRate, throughputMbps
            );
        }

        /**
         * 生成格式化报告
         */
        public String toReport()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("╔══════════════════════════════════════════════════════════╗\n");
            sb.append("║           压力测试性能指标报告                           ║\n");
            sb.append("╠══════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║ 测试时长: %-10d 秒                                  ║\n", durationMillis / 1000));
            sb.append(String.format("║                                                        ║\n"));
            sb.append(String.format("║ 【连接指标】                                          ║\n"));
            sb.append(String.format("║   总连接数:     %-6d                                ║\n", totalConnections));
            sb.append(String.format("║   活跃连接:     %-6d                                ║\n", activeConnections));
            sb.append(String.format("║   失败连接:     %-6d                                ║\n", failedConnections));
            sb.append(String.format("║   已关闭连接:   %-6d                                ║\n", closedConnections));
            sb.append(String.format("║                                                        ║\n"));
            sb.append(String.format("║ 【请求指标】                                          ║\n"));
            sb.append(String.format("║   总请求数:     %-8d                              ║\n", totalRequests));
            sb.append(String.format("║   成功请求:     %-8d                              ║\n", successRequests));
            sb.append(String.format("║   失败请求:     %-8d                              ║\n", failedRequests));
            sb.append(String.format("║   超时请求:     %-8d                              ║\n", timeoutRequests));
            sb.append(String.format("║   成功率:       %-6.2f %%                            ║\n", successRate));
            sb.append(String.format("║                                                        ║\n"));
            sb.append(String.format("║ 【性能指标】                                          ║\n"));
            sb.append(String.format("║   QPS:          %-10.2f                          ║\n", qps));
            sb.append(String.format("║   平均延迟:     %-8.2f ms                          ║\n", avgLatencyMs));
            sb.append(String.format("║   最小延迟:     %-6d ms                            ║\n", minLatencyMs));
            sb.append(String.format("║   最大延迟:     %-6d ms                            ║\n", maxLatencyMs));
            sb.append(String.format("║   吞吐量:       %-8.2f Mbps                        ║\n", throughputMbps));
            sb.append("╚══════════════════════════════════════════════════════════╝");
            return sb.toString();
        }
    }

    /**
     * 重置所有指标
     */
    public void reset()
    {
        totalConnections.reset();
        activeConnections.reset();
        failedConnections.reset();
        closedConnections.reset();
        totalRequests.reset();
        successRequests.reset();
        failedRequests.reset();
        timeoutRequests.reset();
        totalLatencyNanos.reset();
        minLatencyNanos.set(Long.MAX_VALUE);
        maxLatencyNanos.set(Long.MIN_VALUE);
        totalBytesSent.reset();
        totalBytesReceived.reset();
        testStartTime = null;
        testEndTime = null;
        running = false;
    }
}
