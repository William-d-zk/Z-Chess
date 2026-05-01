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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT 5.0 保留消息存储
 *
 * <p>保留消息特性: - 每个主题只保留最后一条保留消息 - 新订阅者订阅时会立即收到保留消息 - 发送空 payload 可删除保留消息
 *
 * @author william.d.zk
 */
public class RetainedMessageStore {

  private static final Logger _Logger =
      LoggerFactory.getLogger("protocol.bishop." + RetainedMessageStore.class.getSimpleName());

  /** 主题 -> 保留消息 */
  private final Map<String, RetainedMessage> _retainedMessages = new ConcurrentHashMap<>();

  /**
   * 存储保留消息
   *
   * @param topic 主题
   * @param payload 消息内容（null 或空表示删除）
   * @param qos QoS 等级
   */
  public void store(String topic, byte[] payload, int qos) {
    if (payload == null || payload.length == 0) {
      _retainedMessages.remove(topic);
      _Logger.info("Retained message deleted: topic=%s", topic);
    } else {
      RetainedMessage retained = new RetainedMessage(topic, payload, qos);
      _retainedMessages.put(topic, retained);
      _Logger.info(
          "Retained message stored: topic=%s, qos=%d, size=%d", topic, qos, payload.length);
    }
  }

  /**
   * 获取主题的保留消息
   *
   * @param topic 主题
   * @return 保留消息，不存在返回 null
   */
  public RetainedMessage get(String topic) {
    return _retainedMessages.get(topic);
  }

  /**
   * 获取匹配主题的所有保留消息
   *
   * @param topicFilter 主题过滤器（支持通配符 + 和 #）
   * @return 匹配的保留消息列表
   */
  public List<RetainedMessage> getMatching(String topicFilter) {
    List<RetainedMessage> result = new ArrayList<>();

    for (RetainedMessage msg : _retainedMessages.values()) {
      if (matches(msg.getTopic(), topicFilter)) {
        result.add(msg);
      }
    }

    _Logger.debug("Found %d retained messages matching: %s", result.size(), topicFilter);
    return result;
  }

  /** 检查主题是否匹配过滤器 */
  private boolean matches(String topic, String filter) {
    if (topic.equals(filter)) {
      return true;
    }

    String[] topicParts = topic.split("/");
    String[] filterParts = filter.split("/");

    return matches(topicParts, filterParts, 0, 0);
  }

  private boolean matches(String[] topic, String[] filter, int tIndex, int fIndex) {
    if (fIndex >= filter.length) {
      return tIndex >= topic.length;
    }

    if (filter[fIndex].equals("#")) {
      return true;
    }

    if (tIndex >= topic.length) {
      return false;
    }

    if (filter[fIndex].equals("+")) {
      return matches(topic, filter, tIndex + 1, fIndex + 1);
    }

    if (!topic[tIndex].equals(filter[fIndex])) {
      return false;
    }

    return matches(topic, filter, tIndex + 1, fIndex + 1);
  }

  /** 获取保留消息数量 */
  public int size() {
    return _retainedMessages.size();
  }

  /** 清除所有保留消息 */
  public void clear() {
    _retainedMessages.clear();
    _Logger.info("All retained messages cleared");
  }

  /** 保留消息内部类 */
  public static class RetainedMessage {
    private final String topic;
    private final byte[] payload;
    private final int qos;
    private final long timestamp;

    public RetainedMessage(String topic, byte[] payload, int qos) {
      this.topic = topic;
      this.payload = Arrays.copyOf(payload, payload.length);
      this.qos = qos;
      this.timestamp = System.currentTimeMillis();
    }

    public String getTopic() {
      return topic;
    }

    public byte[] getPayload() {
      return Arrays.copyOf(payload, payload.length);
    }

    public int getQos() {
      return qos;
    }

    public long getTimestamp() {
      return timestamp;
    }
  }
}
