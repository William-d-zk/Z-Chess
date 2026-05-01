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

package com.isahl.chess.pawn.endpoint.device.service;

import com.isahl.chess.king.base.cron.ScheduleHandler;
import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.cron.features.ICancelable;
import com.isahl.chess.king.base.features.IValid;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MQTT v5.0 消息过期服务
 *
 * <p>管理 MQTT 消息的过期时间，包括：
 *
 * <ul>
 *   <li>保留消息的过期清理
 *   <li>离线消息（Will Delay）的延迟发送
 *   <li>消息过期事件的回调处理
 * </ul>
 *
 * @author william.d.zk
 * @see com.isahl.chess.bishop.protocol.mqtt.model.QttProperty#MESSAGE_EXPIRY_INTERVAL
 */
@Service
public class QttMessageExpiryService implements IValid {
  private static final Logger _Logger =
      LoggerFactory.getLogger("endpoint.pawn." + QttMessageExpiryService.class.getSimpleName());

  private final TimeWheel _TimeWheel;

  /** 过期任务映射 (messageId -> ExpiryTask) */
  private final Map<String, ExpiryTask> _ExpiryTasks = new ConcurrentHashMap<>();

  /** 统计：过期消息数量 */
  private final AtomicLong _ExpiredMessageCount = new AtomicLong(0);

  /** 统计：取消过期任务数量 */
  private final AtomicLong _CancelledExpiryCount = new AtomicLong(0);

  @Autowired
  public QttMessageExpiryService(TimeWheel timeWheel) {
    _TimeWheel = timeWheel;
  }

  /** 消息过期任务 */
  public static class ExpiryTask {
    final String messageId;
    final String topic;
    final long expiryTimeMillis;
    final ICancelable cancelable;

    ExpiryTask(String messageId, String topic, long expiryTimeMillis, ICancelable cancelable) {
      this.messageId = messageId;
      this.topic = topic;
      this.expiryTimeMillis = expiryTimeMillis;
      this.cancelable = cancelable;
    }

    ICancelable getCancelable() {
      return cancelable;
    }
  }

  // ==================== 消息过期调度 ====================

  /**
   * 调度消息过期任务
   *
   * @param messageId 消息唯一标识
   * @param topic 主题
   * @param expiryIntervalSeconds 过期时间间隔（秒）
   * @param onExpiry 过期回调
   * @return true 如果成功调度
   */
  public boolean scheduleMessageExpiry(
      String messageId, String topic, long expiryIntervalSeconds, Consumer<String> onExpiry) {
    if (expiryIntervalSeconds <= 0) {
      // 0 或负数表示立即过期或不设置过期
      return false;
    }

    // 取消已存在的任务
    cancelMessageExpiry(messageId);

    Duration delay = Duration.ofSeconds(expiryIntervalSeconds);
    long expiryTime = System.currentTimeMillis() + delay.toMillis();

    ScheduleHandler<QttMessageExpiryService> handler =
        new ScheduleHandler<>(
            delay,
            false, // 不重复执行
            (service) -> {
              _ExpiryTasks.remove(messageId);
              _ExpiredMessageCount.incrementAndGet();
              _Logger.debug("Message expired: %s (topic: %s)", messageId, topic);
              if (onExpiry != null) {
                try {
                  onExpiry.accept(messageId);
                } catch (Exception e) {
                  _Logger.warn("Error in expiry callback for message: %s", e, messageId);
                }
              }
            },
            5 // 默认优先级
            );

    // 提交到时间轮并获取取消句柄
    ICancelable cancelable = _TimeWheel.acquire(this, handler);

    ExpiryTask task = new ExpiryTask(messageId, topic, expiryTime, cancelable);
    _ExpiryTasks.put(messageId, task);

    _Logger.debug("Scheduled message expiry: %s in %d seconds", messageId, expiryIntervalSeconds);
    return true;
  }

  /**
   * 取消消息过期任务
   *
   * <p>在消息被成功投递后调用
   *
   * @param messageId 消息标识
   * @return true 如果成功取消
   */
  public boolean cancelMessageExpiry(String messageId) {
    ExpiryTask task = _ExpiryTasks.remove(messageId);
    if (task != null && task.getCancelable() != null) {
      task.getCancelable().cancel();
      _CancelledExpiryCount.incrementAndGet();
      _Logger.debug("Cancelled message expiry: %s", messageId);
      return true;
    }
    return false;
  }

  /** 检查消息是否已设置过期任务 */
  public boolean hasExpiryTask(String messageId) {
    return _ExpiryTasks.containsKey(messageId);
  }

  /**
   * 获取消息剩余过期时间（秒）
   *
   * @return 剩余秒数，-1 表示未设置过期
   */
  public long getRemainingExpiryTime(String messageId) {
    ExpiryTask task = _ExpiryTasks.get(messageId);
    if (task == null) {
      return -1;
    }
    long remaining = task.expiryTimeMillis - System.currentTimeMillis();
    return Math.max(0, remaining / 1000);
  }

  // ==================== 保留消息过期 ====================

  /**
   * 设置保留消息的过期时间
   *
   * @param topic 主题
   * @param expirySeconds 过期时间（秒）
   * @param onRetainExpired 过期回调（用于删除保留消息）
   */
  public void setRetainedMessageExpiry(
      String topic, long expirySeconds, Consumer<String> onRetainExpired) {
    if (expirySeconds <= 0) {
      return;
    }

    String retainKey = "retain:" + topic;
    scheduleMessageExpiry(
        retainKey,
        topic,
        expirySeconds,
        (key) -> {
          if (onRetainExpired != null) {
            onRetainExpired.accept(topic);
          }
        });

    _Logger.debug(
        "Set retained message expiry for topic: %s, expiry: %d seconds", topic, expirySeconds);
  }

  /** 取消保留消息的过期任务 */
  public void cancelRetainedMessageExpiry(String topic) {
    String retainKey = "retain:" + topic;
    cancelMessageExpiry(retainKey);
  }

  // ==================== 遗嘱延迟 ====================

  /**
   * 调度遗嘱延迟发送
   *
   * @param sessionId 会话标识
   * @param willDelaySeconds 遗嘱延迟时间（秒）
   * @param onWillDelayExpired 延迟到期回调
   */
  public void scheduleWillDelay(
      long sessionId, long willDelaySeconds, Consumer<Long> onWillDelayExpired) {
    if (willDelaySeconds <= 0) {
      // 立即执行
      if (onWillDelayExpired != null) {
        onWillDelayExpired.accept(sessionId);
      }
      return;
    }

    String willKey = "will:" + sessionId;

    // 取消已存在的遗嘱延迟任务
    cancelMessageExpiry(willKey);

    Duration delay = Duration.ofSeconds(willDelaySeconds);

    ScheduleHandler<QttMessageExpiryService> handler =
        new ScheduleHandler<>(
            delay,
            false,
            (service) -> {
              _ExpiryTasks.remove(willKey);
              _Logger.debug("Will delay expired for session: %#x", sessionId);
              if (onWillDelayExpired != null) {
                try {
                  onWillDelayExpired.accept(sessionId);
                } catch (Exception e) {
                  _Logger.warn("Error in will delay callback for session: %#x", e, sessionId);
                }
              }
            },
            5);

    // 提交到时间轮并获取取消句柄
    ICancelable cancelable = _TimeWheel.acquire(this, handler);

    long expiryTime = System.currentTimeMillis() + delay.toMillis();
    ExpiryTask task = new ExpiryTask(willKey, "will", expiryTime, cancelable);
    _ExpiryTasks.put(willKey, task);

    _Logger.debug(
        "Scheduled will delay for session: %#x, delay: %d seconds", sessionId, willDelaySeconds);
  }

  /**
   * 取消遗嘱延迟
   *
   * <p>在客户端重新连接时调用
   */
  public void cancelWillDelay(long sessionId) {
    String willKey = "will:" + sessionId;
    if (cancelMessageExpiry(willKey)) {
      _Logger.debug("Cancelled will delay for session: %#x", sessionId);
    }
  }

  // ==================== 批量清理 ====================

  /** 清理会话相关的所有过期任务 */
  public void clearSessionTasks(long sessionId) {
    // 清理遗嘱延迟任务
    cancelWillDelay(sessionId);

    // 清理该会话相关的其他任务（如果有）
    String sessionPrefix = "session:" + sessionId + ":";
    java.util.List<String> keysToRemove = new java.util.ArrayList<>();
    for (String key : _ExpiryTasks.keySet()) {
      if (key.startsWith(sessionPrefix)) {
        keysToRemove.add(key);
      }
    }
    for (String key : keysToRemove) {
      ExpiryTask task = _ExpiryTasks.remove(key);
      if (task != null && task.getCancelable() != null) {
        task.getCancelable().cancel();
      }
    }
  }

  /** 获取活跃过期任务数量 */
  public int getActiveExpiryTaskCount() {
    return _ExpiryTasks.size();
  }

  // ==================== 统计信息 ====================

  /** 获取统计信息 */
  public String getStatistics() {
    return String.format(
        "MessageExpiryStats{active=%d, expired=%d, cancelled=%d}",
        getActiveExpiryTaskCount(), _ExpiredMessageCount.get(), _CancelledExpiryCount.get());
  }

  /** 重置统计 */
  public void resetStatistics() {
    _ExpiredMessageCount.set(0);
    _CancelledExpiryCount.set(0);
  }

  // ==================== IValid 接口实现 ====================

  @Override
  public boolean isValid() {
    return true;
  }
}
