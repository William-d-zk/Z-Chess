/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.retry;

/** 指数退避重试策略 */
public class ExponentialBackoffRetry implements RetryPolicy {

  private final int maxRetries;
  private final long initialDelayMs;
  private final long maxDelayMs;
  private final double multiplier;

  public ExponentialBackoffRetry(int maxRetries, long initialDelayMs) {
    this(maxRetries, initialDelayMs, Long.MAX_VALUE, 2.0);
  }

  public ExponentialBackoffRetry(int maxRetries, long initialDelayMs, long maxDelayMs) {
    this(maxRetries, initialDelayMs, maxDelayMs, 2.0);
  }

  public ExponentialBackoffRetry(
      int maxRetries, long initialDelayMs, long maxDelayMs, double multiplier) {
    this.maxRetries = maxRetries;
    this.initialDelayMs = initialDelayMs;
    this.maxDelayMs = maxDelayMs;
    this.multiplier = multiplier;
  }

  @Override
  public boolean shouldRetry(int attempt, Throwable exception) {
    return attempt < maxRetries;
  }

  @Override
  public long getDelay(int attempt) {
    long delay = (long) (initialDelayMs * Math.pow(multiplier, attempt));
    return Math.min(delay, maxDelayMs);
  }
}
