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

package com.isahl.chess.bishop.protocol.mqtt.model;

import com.isahl.chess.king.base.log.Logger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MQTT v5.0 主题别名管理器
 *
 * <p>主题别名机制允许将较长的主题名称替换为短整数（2字节），从而减少网络带宽。 MQTT v5.0 支持双向主题别名：
 *
 * <ul>
 *   <li>客户端→服务器方向：客户端发布消息时可以使用别名
 *   <li>服务器→客户端方向：服务器下发消息时可以使用别名
 * </ul>
 *
 * <p>主题别名规则：
 *
 * <ul>
 *   <li>别名值从 1 开始，0 表示不使用别名
 *   <li>客户端和服务器各自维护自己的别名映射表
 *   <li>第一次发送主题时必须使用完整主题名，同时可以携带别名
 *   <li>后续可以使用别名替代主题名
 *   <li>别名生命周期与会话相同
 * </ul>
 *
 * @author william.d.zk
 * @see QttProperty#TOPIC_ALIAS
 * @see QttProperty#TOPIC_ALIAS_MAXIMUM
 */
public class QttTopicAlias {
  private static final Logger _Logger =
      Logger.getLogger("protocol.bishop." + QttTopicAlias.class.getSimpleName());

  /**
   * 客户端→服务器方向映射 (Alias -> Topic)
   *
   * <p>服务器用于解析客户端发来的别名
   */
  private final Map<Integer, String> _ClientToServer = new ConcurrentHashMap<>();

  /**
   * 服务器→客户端方向映射 (Topic -> Alias)
   *
   * <p>服务器为下发消息创建别名映射
   */
  private final Map<String, Integer> _ServerToClient = new ConcurrentHashMap<>();

  /** 反向映射 (Alias -> Topic)，用于服务器端别名解析 */
  private final Map<Integer, String> _ServerToClientReverse = new ConcurrentHashMap<>();

  /** 下一个待分配的别名 */
  private final AtomicInteger _NextAliasId = new AtomicInteger(1);

  /** 允许的最大别名值 */
  private final int _MaxAliasId;

  /** 是否启用主题别名 */
  private final boolean _Enabled;

  /** 统计：成功使用别名的次数 */
  private final AtomicInteger _AliasHitCount = new AtomicInteger(0);

  /** 统计：新创建别名的次数 */
  private final AtomicInteger _AliasCreatedCount = new AtomicInteger(0);

  /**
   * 创建主题别名管理器
   *
   * @param maxAliasId 最大别名值（由 CONNECT/CONNACK 的 TopicAliasMaximum 协商确定）
   */
  public QttTopicAlias(int maxAliasId) {
    _MaxAliasId = maxAliasId;
    _Enabled = maxAliasId > 0;
    if (_Enabled) {
      _Logger.debug("Topic alias enabled, max alias id: %d", maxAliasId);
    }
  }

  /** 检查主题别名是否启用 */
  public boolean isEnabled() {
    return _Enabled;
  }

  /** 获取最大别名值 */
  public int getMaxAliasId() {
    return _MaxAliasId;
  }

  // ==================== 客户端→服务器方向 ====================

  /**
   * 注册客户端→服务器方向的别名映射
   *
   * <p>当服务器接收到客户端发送的 PUBLISH（包含 Topic Alias）时调用
   *
   * @param alias 别名值（必须 > 0）
   * @param topic 主题名称
   * @throws IllegalArgumentException 如果别名超出范围
   */
  public void registerClientToServer(int alias, String topic) {
    if (!_Enabled) {
      return;
    }
    if (alias <= 0 || alias > _MaxAliasId) {
      throw new IllegalArgumentException(
          String.format("Topic alias out of range: %d (max: %d)", alias, _MaxAliasId));
    }
    if (topic == null || topic.isEmpty()) {
      throw new IllegalArgumentException("Topic cannot be null or empty");
    }

    String existing = _ClientToServer.put(alias, topic);
    if (existing == null) {
      _AliasCreatedCount.incrementAndGet();
      _Logger.debug("Registered C2S alias: %d -> %s", alias, topic);
    }
  }

  /**
   * 解析客户端发来的主题别名
   *
   * @param alias 别名值
   * @return 对应的主题名称，未找到返回 null
   */
  public String resolveClientToServer(int alias) {
    if (!_Enabled || alias <= 0) {
      return null;
    }
    String topic = _ClientToServer.get(alias);
    if (topic != null) {
      _AliasHitCount.incrementAndGet();
    }
    return topic;
  }

  /** 检查客户端→服务器方向的别名是否已注册 */
  public boolean hasClientToServerAlias(int alias) {
    return _Enabled && alias > 0 && alias <= _MaxAliasId && _ClientToServer.containsKey(alias);
  }

  // ==================== 服务器→客户端方向 ====================

  /**
   * 获取或创建服务器→客户端方向的别名
   *
   * <p>服务器下发消息时使用此方法获取主题的别名
   *
   * @param topic 主题名称
   * @return 别名值（> 0），如果不启用别名或无法分配返回 0
   */
  public int getOrCreateServerToClient(String topic) {
    if (!_Enabled || topic == null || topic.isEmpty()) {
      return 0;
    }

    // 检查是否已有别名
    Integer existingAlias = _ServerToClient.get(topic);
    if (existingAlias != null) {
      _AliasHitCount.incrementAndGet();
      return existingAlias;
    }

    // 创建新别名
    int newAlias = _NextAliasId.getAndIncrement();
    if (newAlias > _MaxAliasId) {
      // 超出最大别名限制，不再创建新别名
      _Logger.debug("Max alias limit reached, cannot create alias for: %s", topic);
      return 0;
    }

    _ServerToClient.put(topic, newAlias);
    _ServerToClientReverse.put(newAlias, topic);
    _AliasCreatedCount.incrementAndGet();

    _Logger.debug("Created S2C alias: %d -> %s", newAlias, topic);
    return newAlias;
  }

  /**
   * 解析服务器下发的主题别名
   *
   * <p>通常客户端使用此方法，服务器一般不需要
   *
   * @param alias 别名值
   * @return 对应的主题名称，未找到返回 null
   */
  public String resolveServerToClient(int alias) {
    if (!_Enabled || alias <= 0) {
      return null;
    }
    String topic = _ServerToClientReverse.get(alias);
    if (topic != null) {
      _AliasHitCount.incrementAndGet();
    }
    return topic;
  }

  /** 检查服务器→客户端方向的主题是否已有别名 */
  public boolean hasServerToClientAlias(String topic) {
    return _Enabled && topic != null && _ServerToClient.containsKey(topic);
  }

  /**
   * 获取服务器→客户端方向的别名（不创建新的）
   *
   * @param topic 主题名称
   * @return 别名值，未找到返回 0
   */
  public int getServerToClientAlias(String topic) {
    if (!_Enabled || topic == null) {
      return 0;
    }
    Integer alias = _ServerToClient.get(topic);
    return alias != null ? alias : 0;
  }

  // ==================== 通用方法 ====================

  /**
   * 清空所有别名映射
   *
   * <p>通常在会话结束时调用
   */
  public void clear() {
    _ClientToServer.clear();
    _ServerToClient.clear();
    _ServerToClientReverse.clear();
    _NextAliasId.set(1);
    _AliasHitCount.set(0);
    _AliasCreatedCount.set(0);
    _Logger.debug("Topic alias mappings cleared");
  }

  /** 获取当前已注册的 C2S 别名数量 */
  public int getClientToServerAliasCount() {
    return _ClientToServer.size();
  }

  /** 获取当前已注册的 S2C 别名数量 */
  public int getServerToClientAliasCount() {
    return _ServerToClient.size();
  }

  /** 获取别名命中次数（统计用途） */
  public int getAliasHitCount() {
    return _AliasHitCount.get();
  }

  /** 获取新创建别名次数（统计用途） */
  public int getAliasCreatedCount() {
    return _AliasCreatedCount.get();
  }

  /** 获取统计信息 */
  public String getStatistics() {
    return String.format(
        "TopicAliasStats{c2s=%d, s2c=%d, hits=%d, created=%d, max=%d}",
        getClientToServerAliasCount(),
        getServerToClientAliasCount(),
        getAliasHitCount(),
        getAliasCreatedCount(),
        _MaxAliasId);
  }

  /**
   * 检查别名是否有效
   *
   * @param alias 别名值
   * @return true 如果别名在有效范围内
   */
  public boolean isValidAlias(int alias) {
    return alias > 0 && alias <= _MaxAliasId;
  }

  @Override
  public String toString() {
    return String.format(
        "QttTopicAlias{enabled=%s, max=%d, c2s=%d, s2c=%d}",
        _Enabled, _MaxAliasId, getClientToServerAliasCount(), getServerToClientAliasCount());
  }
}
