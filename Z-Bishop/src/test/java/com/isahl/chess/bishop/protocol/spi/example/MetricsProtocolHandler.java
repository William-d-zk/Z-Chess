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

package com.isahl.chess.bishop.protocol.spi.example;

import com.isahl.chess.bishop.protocol.spi.ProtocolContext;
import com.isahl.chess.bishop.protocol.spi.ProtocolHandler;
import com.isahl.chess.bishop.protocol.spi.ProtocolResult;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 指标收集协议处理器示例
 *
 * <p>示例处理器，收集消息处理的指标统计。
 *
 * @author william.d.zk
 * @since 1.1.2
 */
public class MetricsProtocolHandler implements ProtocolHandler {

  private static final Logger _Logger =
      LoggerFactory.getLogger(
          "protocol.bishop.spi.example." + MetricsProtocolHandler.class.getSimpleName());

  /** 消息类型计数器 */
  private final Map<String, AtomicLong> _messageCounters = new ConcurrentHashMap<>();

  /** 总字节数 */
  private final AtomicLong _totalBytes = new AtomicLong(0);

  /** 总处理时间 */
  private final AtomicLong _totalProcessingTime = new AtomicLong(0);

  @Override
  public boolean supports(IProtocol message) {
    return true;
  }

  @Override
  public void handle(IProtocol message, ProtocolContext context) {
    // 记录消息类型计数
    String messageType = message.getClass().getSimpleName();
    _messageCounters.computeIfAbsent(messageType, k -> new AtomicLong(0)).incrementAndGet();

    // 记录字节数
    _totalBytes.addAndGet(message.sizeOf());

    _Logger.debug(
        "[Metrics] Counted message: {} (total: {})", messageType, getMessageCount(messageType));
  }

  @Override
  public void afterHandle(IProtocol message, ProtocolContext context, ProtocolResult result) {
    // 记录处理时间
    _totalProcessingTime.addAndGet(context.getElapsedTime());

    _Logger.debug(
        "[Metrics] Total processed: {} messages, {} bytes",
        getTotalMessageCount(),
        _totalBytes.get());
  }

  @Override
  public int getPriority() {
    // 较低优先级，在其他处理器之后执行
    return 200;
  }

  @Override
  public String getName() {
    return "MetricsProtocolHandler";
  }

  @Override
  public String getDescription() {
    return "Collects metrics about protocol messages";
  }

  // ==================== 指标查询 ====================

  /** 获取指定消息类型的计数 */
  public long getMessageCount(String messageType) {
    AtomicLong counter = _messageCounters.get(messageType);
    return counter != null ? counter.get() : 0;
  }

  /** 获取总消息计数 */
  public long getTotalMessageCount() {
    return _messageCounters.values().stream().mapToLong(AtomicLong::get).sum();
  }

  /** 获取总字节数 */
  public long getTotalBytes() {
    return _totalBytes.get();
  }

  /** 获取平均处理时间 */
  public double getAverageProcessingTime() {
    long total = getTotalMessageCount();
    return total > 0 ? (double) _totalProcessingTime.get() / total : 0;
  }

  /** 重置所有指标 */
  public void reset() {
    _messageCounters.clear();
    _totalBytes.set(0);
    _totalProcessingTime.set(0);
    _Logger.info("[Metrics] All metrics reset");
  }

  /** 获取指标报告 */
  public String getReport() {
    StringBuilder report = new StringBuilder();
    report.append("=== Protocol Metrics Report ===\n");
    report.append(String.format("Total Messages: %d\n", getTotalMessageCount()));
    report.append(String.format("Total Bytes: %d\n", getTotalBytes()));
    report.append(String.format("Avg Processing Time: %.2f ms\n", getAverageProcessingTime()));
    report.append("Message Type Breakdown:\n");
    _messageCounters.forEach(
        (type, count) -> report.append(String.format("  %s: %d\n", type, count.get())));
    return report.toString();
  }
}
