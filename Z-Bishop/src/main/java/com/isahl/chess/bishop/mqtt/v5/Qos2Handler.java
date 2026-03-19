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

import com.isahl.chess.king.base.log.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * MQTT 5.0 QoS 2 消息处理器 实现 Exactly-Once 投递保证
 *
 * <p>QoS 2 流程: 1. PUBLISH (QoS=2) -> 2. PUBREC -> 3. PUBREL -> 4. PUBCOMP
 *
 * @author william.d.zk
 */
public class Qos2Handler {

  private static final Logger _Logger =
      Logger.getLogger("protocol.bishop." + Qos2Handler.class.getSimpleName());

  /** 等待 PUBREC 的消息 (已发送 PUBLISH) */
  private final ConcurrentMap<Integer, Qos2Message> _pendingPubrec = new ConcurrentHashMap<>();

  /** 等待 PUBREL 的消息 (已发送 PUBREC) */
  private final ConcurrentMap<Integer, Qos2Message> _pendingPubrel = new ConcurrentHashMap<>();

  /** 等待 PUBCOMP 的消息 (已发送 PUBREL) */
  private final ConcurrentMap<Integer, Qos2Message> _pendingPubcomp = new ConcurrentHashMap<>();

  /** 已处理的 Message ID，用于去重 */
  private final ConcurrentMap<Integer, Long> _processedMessageIds = new ConcurrentHashMap<>();

  /**
   * 处理接收到的 QoS 2 PUBLISH
   *
   * @param messageId 消息 ID
   * @param payload 消息内容
   * @return PUBREC 消息 ID
   */
  public int handlePublish(int messageId, byte[] payload) {
    if (isDuplicate(messageId)) {
      _Logger.debug("Duplicate QoS 2 PUBLISH received: messageId=%d", messageId);
      return messageId;
    }

    _pendingPubrec.put(messageId, new Qos2Message(messageId, payload));
    markAsProcessed(messageId);

    _Logger.info("QoS 2 PUBLISH received, sending PUBREC: messageId=%d", messageId);
    return messageId;
  }

  /**
   * 处理接收到的 PUBREC
   *
   * @param messageId 消息 ID
   * @return 是否需要发送 PUBREL
   */
  public boolean handlePubrec(int messageId) {
    Qos2Message msg = _pendingPubrec.remove(messageId);
    if (msg != null) {
      _pendingPubrel.put(messageId, msg);
      _Logger.info("QoS 2 PUBREC received, sending PUBREL: messageId=%d", messageId);
      return true;
    }

    _Logger.warning("QoS 2 PUBREC received for unknown message: messageId=%d", messageId);
    return false;
  }

  /**
   * 处理接收到的 PUBREL
   *
   * @param messageId 消息 ID
   * @return 是否需要发送 PUBCOMP
   */
  public boolean handlePubrel(int messageId) {
    Qos2Message msg = _pendingPubrel.remove(messageId);
    if (msg != null) {
      _pendingPubcomp.put(messageId, msg);
      markAsProcessed(messageId);
      _Logger.info("QoS 2 PUBREL received, sending PUBCOMP: messageId=%d", messageId);
      return true;
    }

    _Logger.warning("QoS 2 PUBREL received for unknown message: messageId=%d", messageId);
    return false;
  }

  /**
   * 处理接收到的 PUBCOMP
   *
   * @param messageId 消息 ID
   * @return 完成的 QoS 2 消息，如果没有找到返回 null
   */
  public Qos2Message handlePubcomp(int messageId) {
    Qos2Message msg = _pendingPubcomp.remove(messageId);
    if (msg != null) {
      _Logger.info("QoS 2 PUBCOMP received, message complete: messageId=%d", messageId);
      return msg;
    }

    _Logger.warning("QoS 2 PUBCOMP received for unknown message: messageId=%d", messageId);
    return null;
  }

  /**
   * 发起 QoS 2 消息发送（作为发送方）
   *
   * @param messageId 消息 ID
   * @param payload 消息内容
   */
  public void initiateQos2Publish(int messageId, byte[] payload) {
    _pendingPubrec.put(messageId, new Qos2Message(messageId, payload));
    _Logger.debug("QoS 2 PUBLISH initiated: messageId=%d", messageId);
  }

  /** 检查是否是重复消息 */
  private boolean isDuplicate(int messageId) {
    return _processedMessageIds.containsKey(messageId);
  }

  /** 标记消息为已处理 */
  private void markAsProcessed(int messageId) {
    _processedMessageIds.put(messageId, System.currentTimeMillis());
    cleanupOldMessages();
  }

  /** 清理旧的处理记录（超过 5 分钟） */
  private void cleanupOldMessages() {
    long now = System.currentTimeMillis();
    long threshold = 5 * 60 * 1000; // 5 minutes

    _processedMessageIds.entrySet().removeIf(entry -> (now - entry.getValue()) > threshold);
  }

  /** 获取待处理消息数量 */
  public int getPendingCount() {
    return _pendingPubrec.size() + _pendingPubrel.size() + _pendingPubcomp.size();
  }

  /** QoS 2 消息内部类 */
  public static class Qos2Message {
    private final int messageId;
    private final byte[] payload;
    private final long timestamp;

    public Qos2Message(int messageId, byte[] payload) {
      this.messageId = messageId;
      this.payload = payload;
      this.timestamp = System.currentTimeMillis();
    }

    public int getMessageId() {
      return messageId;
    }

    public byte[] getPayload() {
      return payload;
    }

    public long getTimestamp() {
      return timestamp;
    }
  }
}
