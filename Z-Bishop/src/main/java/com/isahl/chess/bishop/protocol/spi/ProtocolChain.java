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

package com.isahl.chess.bishop.protocol.spi;

import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 协议处理链
 *
 * <p>管理协议处理器的执行流程，支持：
 *
 * <ul>
 *   <li>按优先级顺序执行处理器
 *   <li>支持中断处理链
 *   <li>支持跳过当前处理器
 *   <li>统计处理耗时
 * </ul>
 *
 * @author william.d.zk
 * @since 1.1.2
 */
public class ProtocolChain {

  private static final Logger _Logger =
      LoggerFactory.getLogger("protocol.bishop.spi." + ProtocolChain.class.getSimpleName());

  /** 处理器列表 */
  private final List<ProtocolHandler> _handlers;

  /** 处理器加载器 */
  private final ProtocolLoader _loader;

  /** 统计信息 */
  private final ChainStatistics _statistics = new ChainStatistics();

  public ProtocolChain() {
    this(new ProtocolLoader());
  }

  public ProtocolChain(ProtocolLoader loader) {
    this._loader = loader;
    this._handlers = loader.loadHandlers();
  }

  // ==================== 处理方法 ====================

  /**
   * 处理消息
   *
   * @param message 消息
   * @param session 会话
   * @return 处理结果
   */
  public ProtocolResult process(IProtocol message, ISession session) {
    ProtocolContext context = new ProtocolContext(session, message);

    _statistics.incrementTotalCount();
    long startTime = System.currentTimeMillis();

    try {
      for (ProtocolHandler handler : _handlers) {
        // 检查是否支持此消息
        if (!handler.supports(message)) {
          continue;
        }

        // 执行前置钩子
        if (!handler.beforeHandle(message, context)) {
          _Logger.trace("Handler {} beforeHandle returned false, skipping", handler.getName());
          continue;
        }

        // 执行处理
        ProtocolResult result = executeHandler(handler, message, context);

        // 执行后置钩子
        handler.afterHandle(message, context, result);

        // 检查结果
        if (result == ProtocolResult.COMPLETED) {
          context.markCompleted();
          _Logger.trace("Handler {} marked as completed, stopping chain", handler.getName());
          break;
        }

        if (result == ProtocolResult.FAILED) {
          context.markFailed("Handler " + handler.getName() + " failed");
          _Logger.warn("Handler {} failed, stopping chain", handler.getName());
          break;
        }

        // 如果上下文标记为完成，也停止
        if (!context.shouldContinue()) {
          break;
        }
      }

      _statistics.incrementSuccessCount();
      return context.getResult();

    } catch (Exception e) {
      _Logger.warn("Error processing message: {}", e.getMessage());
      _statistics.incrementFailureCount();
      context.markFailed(e.getMessage());
      return ProtocolResult.failed(e.getMessage());
    } finally {
      long elapsedTime = System.currentTimeMillis() - startTime;
      _statistics.addProcessingTime(elapsedTime);
      _Logger.trace("Message processing completed in {} ms", elapsedTime);
    }
  }

  /** 执行单个处理器 */
  private ProtocolResult executeHandler(
      ProtocolHandler handler, IProtocol message, ProtocolContext context) {
    try {
      _Logger.trace("Executing handler: {}", handler.getName());
      handler.handle(message, context);
      return ProtocolResult.CONTINUE;
    } catch (Exception e) {
      _Logger.warn("Handler {} threw exception: {}", handler.getName(), e.getMessage());
      return ProtocolResult.failed(e.getMessage());
    }
  }

  // ==================== 处理器管理 ====================

  /** 添加处理器 */
  public void addHandler(ProtocolHandler handler) {
    _loader.addHandler(handler);
    // 重新加载以更新排序
    _handlers.clear();
    _handlers.addAll(_loader.getHandlers());
  }

  /** 移除处理器 */
  public void removeHandler(ProtocolHandler handler) {
    _loader.removeHandler(handler);
    _handlers.remove(handler);
  }

  /** 获取处理器数量 */
  public int getHandlerCount() {
    return _handlers.size();
  }

  // ==================== 统计信息 ====================

  /** 获取统计信息 */
  public ChainStatistics getStatistics() {
    return _statistics;
  }

  @Override
  public String toString() {
    return String.format("ProtocolChain{handlers=%d, stats=%s}", _handlers.size(), _statistics);
  }

  // ==================== 统计信息类 ====================

  /** 处理链统计信息 */
  public static class ChainStatistics {
    private long _totalCount = 0;
    private long _successCount = 0;
    private long _failureCount = 0;
    private long _totalProcessingTime = 0;
    private long _maxProcessingTime = 0;
    private long _minProcessingTime = Long.MAX_VALUE;

    public synchronized void incrementTotalCount() {
      _totalCount++;
    }

    public synchronized void incrementSuccessCount() {
      _successCount++;
    }

    public synchronized void incrementFailureCount() {
      _failureCount++;
    }

    public synchronized void addProcessingTime(long time) {
      _totalProcessingTime += time;
      _maxProcessingTime = Math.max(_maxProcessingTime, time);
      _minProcessingTime = Math.min(_minProcessingTime, time);
    }

    public synchronized long getTotalCount() {
      return _totalCount;
    }

    public synchronized long getSuccessCount() {
      return _successCount;
    }

    public synchronized long getFailureCount() {
      return _failureCount;
    }

    public synchronized double getAverageProcessingTime() {
      return _totalCount > 0 ? (double) _totalProcessingTime / _totalCount : 0;
    }

    public synchronized long getMaxProcessingTime() {
      return _maxProcessingTime;
    }

    public synchronized long getMinProcessingTime() {
      return _minProcessingTime == Long.MAX_VALUE ? 0 : _minProcessingTime;
    }

    @Override
    public synchronized String toString() {
      return String.format(
          "ChainStatistics{total=%d, success=%d, failure=%d, avgTime=%.2fms}",
          _totalCount, _successCount, _failureCount, getAverageProcessingTime());
    }
  }
}
