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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT 5.0 遗嘱消息处理器
 *
 * <p>遗嘱消息特性: - 客户端连接时注册遗嘱 - 客户端异常断开时自动发布遗嘱消息 - 正常断开（DISCONNECT）不发布遗嘱
 *
 * @author william.d.zk
 */
public class LastWillHandler {

  private static final Logger _Logger =
      LoggerFactory.getLogger("protocol.bishop." + LastWillHandler.class.getSimpleName());

  /** 客户端 ID -> 遗嘱消息 */
  private final ConcurrentMap<String, LastWillMessage> _lastWills = new ConcurrentHashMap<>();

  /** 消息发布回调 */
  private BiConsumer<String, byte[]> _publishCallback;

  /** 设置消息发布回调 */
  public void setPublishCallback(BiConsumer<String, byte[]> callback) {
    _publishCallback = callback;
  }

  /**
   * 注册遗嘱消息
   *
   * @param clientId 客户端 ID
   * @param topic 遗嘱主题
   * @param payload 遗嘱内容
   * @param qos QoS 等级
   * @param retain 是否保留
   */
  public void registerLastWill(
      String clientId, String topic, byte[] payload, int qos, boolean retain) {
    LastWillMessage will = new LastWillMessage(topic, payload, qos, retain);
    _lastWills.put(clientId, will);

    _Logger.info(
        "Last will registered: clientId=%s, topic=%s, qos=%d, retain=%s",
        clientId, topic, qos, retain);
  }

  /**
   * 客户端正常断开（取消遗嘱）
   *
   * @param clientId 客户端 ID
   */
  public void onNormalDisconnect(String clientId) {
    LastWillMessage removed = _lastWills.remove(clientId);
    if (removed != null) {
      _Logger.info("Last will cancelled (normal disconnect): clientId=%s", clientId);
    }
  }

  /**
   * 客户端异常断开（发布遗嘱）
   *
   * @param clientId 客户端 ID
   */
  public void onAbnormalDisconnect(String clientId) {
    LastWillMessage will = _lastWills.remove(clientId);
    if (will != null) {
      _Logger.info("Publishing last will: clientId=%s, topic=%s", clientId, will.getTopic());

      if (_publishCallback != null) {
        _publishCallback.accept(will.getTopic(), will.getPayload());
      }
    } else {
      _Logger.debug("No last will found for client: clientId=%s", clientId);
    }
  }

  /**
   * 清除客户端遗嘱
   *
   * @param clientId 客户端 ID
   */
  public void removeLastWill(String clientId) {
    _lastWills.remove(clientId);
  }

  /** 获取注册的遗嘱数量 */
  public int size() {
    return _lastWills.size();
  }

  /** 遗嘱消息内部类 */
  public static class LastWillMessage {
    private final String topic;
    private final byte[] payload;
    private final int qos;
    private final boolean retain;

    public LastWillMessage(String topic, byte[] payload, int qos, boolean retain) {
      this.topic = topic;
      this.payload = payload;
      this.qos = qos;
      this.retain = retain;
    }

    public String getTopic() {
      return topic;
    }

    public byte[] getPayload() {
      return payload;
    }

    public int getQos() {
      return qos;
    }

    public boolean isRetain() {
      return retain;
    }
  }
}
