/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Modbus 监控指标
 */
public class ModbusMetrics {
    
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final Map<Integer, AtomicLong> functionCodeCounts = new ConcurrentHashMap<>();
    
    public void recordRequest(int functionCode, long durationMs, boolean success) {
        requestCount.incrementAndGet();
        if (success) {
            successCount.incrementAndGet();
        } else {
            failureCount.incrementAndGet();
        }
        
        functionCodeCounts.computeIfAbsent(functionCode, k -> new AtomicLong(0))
                         .incrementAndGet();
    }
    
    public void recordError(String errorType) {
        errorCount.incrementAndGet();
    }
    
    public void recordConnection() {
        totalConnections.incrementAndGet();
        activeConnections.incrementAndGet();
    }
    
    public void recordDisconnection() {
        activeConnections.decrementAndGet();
    }
    
    public long getRequestCount() { return requestCount.get(); }
    public long getSuccessCount() { return successCount.get(); }
    public long getFailureCount() { return failureCount.get(); }
    public long getErrorCount() { return errorCount.get(); }
    public long getTotalConnections() { return totalConnections.get(); }
    public long getActiveConnections() { return activeConnections.get(); }
    
    public Map<Integer, Long> getFunctionCodeDistribution() {
        return functionCodeCounts.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().get()
            ));
    }
}
