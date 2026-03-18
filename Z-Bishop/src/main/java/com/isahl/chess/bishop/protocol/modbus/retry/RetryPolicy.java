/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.retry;

/**
 * 重试策略接口
 */
public interface RetryPolicy {
    
    /**
     * 是否应该重试
     */
    boolean shouldRetry(int attempt, Throwable exception);
    
    /**
     * 获取重试延迟 (毫秒)
     */
    long getDelay(int attempt);
}
