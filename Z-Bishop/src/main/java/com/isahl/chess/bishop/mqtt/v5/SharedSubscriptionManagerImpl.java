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

import com.isahl.chess.queen.io.core.net.socket.AioSession;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MQTT 5.0 共享订阅管理器实现 使用轮询策略分发 messages 给共享组内的消费者 */
public class SharedSubscriptionManagerImpl implements SharedSubscriptionManager {

  private static final Logger _Logger =
      LoggerFactory.getLogger(SharedSubscriptionManagerImpl.class);

  /** 共享组 -> 订阅列表 -> 会话列表 结构：{ "group1": { "sensors/temp": [session1, session2] } } */
  private final Map<String, Map<String, List<AioSession<?>>>> _sharedGroups =
      new ConcurrentHashMap<>();

  /** 会话 -> 订阅列表，用于清理 */
  private final Map<String, Set<SharedSubscription>> _sessionSubscriptions =
      new ConcurrentHashMap<>();

  /** 每个共享组的轮询计数器 */
  private final Map<String, AtomicInteger> _roundRobinCounters = new ConcurrentHashMap<>();

  @Override
  public void addSubscription(
      String shareName, SharedSubscription subscription, AioSession<?> session) {
    _sharedGroups
        .computeIfAbsent(shareName, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(
            subscription.getTopic(), k -> Collections.synchronizedList(new ArrayList<>()))
        .add(session);

    String sessionId = String.valueOf(session.index());
    _sessionSubscriptions
        .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
        .add(subscription);

    _roundRobinCounters.putIfAbsent(shareName, new AtomicInteger(0));

    _Logger.debug(
        "Added shared subscription: shareName={}, topic={}, sessionId={}",
        shareName,
        subscription.getTopic(),
        sessionId);
  }

  @Override
  public void removeSubscription(String shareName, String topic, AioSession<?> session) {
    Map<String, List<AioSession<?>>> topicMap = _sharedGroups.get(shareName);
    if (topicMap != null) {
      List<AioSession<?>> sessions = topicMap.get(topic);
      if (sessions != null) {
        sessions.remove(session);
        if (sessions.isEmpty()) {
          topicMap.remove(topic);
          if (topicMap.isEmpty()) {
            _sharedGroups.remove(shareName);
            _roundRobinCounters.remove(shareName);
          }
        }
      }
    }

    String sessionId = String.valueOf(session.index());
    Set<SharedSubscription> subs = _sessionSubscriptions.get(sessionId);
    if (subs != null) {
      subs.removeIf(sub -> sub.getShareName().equals(shareName) && sub.getTopic().equals(topic));
    }

    _Logger.debug(
        "Removed shared subscription: shareName={}, topic={}, sessionId={}",
        shareName,
        topic,
        sessionId);
  }

  @Override
  public AioSession<?> selectConsumer(String shareName) {
    Map<String, List<AioSession<?>>> topicMap = _sharedGroups.get(shareName);
    if (topicMap == null || topicMap.isEmpty()) {
      return null;
    }

    // 收集所有消费者
    List<AioSession<?>> allConsumers = new ArrayList<>();
    for (List<AioSession<?>> sessions : topicMap.values()) {
      allConsumers.addAll(sessions);
    }

    if (allConsumers.isEmpty()) {
      return null;
    }

    // 轮询选择
    AtomicInteger counter = _roundRobinCounters.get(shareName);
    if (counter == null) {
      return allConsumers.get(0);
    }

    int index = Math.abs(counter.getAndIncrement() % allConsumers.size());
    AioSession<?> selected = allConsumers.get(index);

    _Logger.trace(
        "Selected consumer for share group {}: sessionId={}", shareName, selected.index());
    return selected;
  }

  @Override
  public List<AioSession<?>> getConsumers(String shareName) {
    Map<String, List<AioSession<?>>> topicMap = _sharedGroups.get(shareName);
    if (topicMap == null) {
      return Collections.emptyList();
    }

    List<AioSession<?>> allConsumers = new ArrayList<>();
    for (List<AioSession<?>> sessions : topicMap.values()) {
      allConsumers.addAll(sessions);
    }
    return Collections.unmodifiableList(allConsumers);
  }

  @Override
  public void cleanupSession(AioSession<?> session) {
    String sessionId = String.valueOf(session.index());
    Set<SharedSubscription> subs = _sessionSubscriptions.remove(sessionId);

    if (subs != null) {
      for (SharedSubscription sub : subs) {
        removeSubscription(sub.getShareName(), sub.getTopic(), session);
      }
    }

    _Logger.debug("Cleaned up session subscriptions: sessionId={}", sessionId);
  }
}
