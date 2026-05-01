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

package com.isahl.chess.bishop.mqtt.v5;

import com.isahl.chess.bishop.protocol.mqtt.model.QttProperty;
import com.isahl.chess.bishop.protocol.mqtt.model.QttPropertySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT 5.0 消息过期处理器 使用 DelayQueue 管理过期消息
 *
 * @author william.d.zk
 */
public class MessageExpiryHandler {
  private static final Logger _Logger =
      LoggerFactory.getLogger("protocol.bishop." + MessageExpiryHandler.class.getSimpleName());

  private final DelayQueue<ExpirableMessage> _expiryQueue = new DelayQueue<>();

  /**
   * 注册消息进行过期监控
   *
   * @param propertySet MQTT 5.0 属性集合
   * @param expiryCallback 过期回调
   */
  public void registerMessage(QttPropertySet propertySet, Runnable expiryCallback) {
    if (propertySet == null) {
      return;
    }

    // 获取消息过期时间属性
    Long expiryInterval = propertySet.getProperty(QttProperty.MESSAGE_EXPIRY_INTERVAL);
    if (expiryInterval == null || expiryInterval <= 0) {
      return; // 永不过期的消息不需要监控
    }

    ExpirableMessage expirable = new ExpirableMessage(propertySet, expiryCallback, expiryInterval);
    _expiryQueue.add(expirable);

    _Logger.trace("Registered message for expiry monitoring: expiry={}s", expiryInterval);
  }

  /** 处理到期消息（应由定时任务调用） */
  public void processExpiredMessages() {
    ExpirableMessage expirable;
    while ((expirable = _expiryQueue.poll()) != null) {
      _Logger.debug("Message expired");
      expirable.getCallback().run();
    }
  }

  /** 获取待过期消息数量 */
  public int getPendingCount() {
    return _expiryQueue.size();
  }

  /** 清理已取消的消息 */
  public void cleanup() {
    _expiryQueue.clear();
  }

  /** 可过期消息包装 */
  private static class ExpirableMessage implements Delayed {
    private final QttPropertySet propertySet;
    private final Runnable callback;
    private final long expiryTime;

    ExpirableMessage(QttPropertySet propertySet, Runnable callback, long expiryIntervalSeconds) {
      this.propertySet = propertySet;
      this.callback = callback;
      this.expiryTime = System.currentTimeMillis() + (expiryIntervalSeconds * 1000);
    }

    public QttPropertySet getPropertySet() {
      return propertySet;
    }

    public Runnable getCallback() {
      return callback;
    }

    @Override
    public long getDelay(TimeUnit unit) {
      long delay = expiryTime - System.currentTimeMillis();
      return unit.convert(delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
      return Long.compare(
          this.getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
    }
  }
}
