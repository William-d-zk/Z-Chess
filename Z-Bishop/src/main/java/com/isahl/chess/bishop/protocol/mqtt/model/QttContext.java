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

import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V3_1_1;
import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V5_0;

import com.isahl.chess.bishop.protocol.ProtocolContext;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * MQTT 协议上下文
 *
 * <p>管理 MQTT 连接的状态和配置，包括：
 *
 * <ul>
 *   <li>协议版本（v3.1.1 / v5.0）
 *   <li>v5 协商的属性（Receive Maximum、Session Expiry 等）
 *   <li>主题别名管理器
 *   <li>流量控制配额
 * </ul>
 *
 * @author william.d.zk
 * @date 2019-05-25
 * @updated 2024 - 增加 MQTT v5.0 支持
 */
public class QttContext extends ProtocolContext<QttFrame> {
  private static final Logger _Logger =
      Logger.getLogger("protocol.bishop." + QttContext.class.getSimpleName());

  private static final IPair SUPPORT_VERSION =
      new Pair<>(new String[] {"5.0", "3.1.1"}, new int[] {VERSION_V5_0, VERSION_V3_1_1});

  public QttContext(INetworkOption option, ISort.Mode mode, ISort.Type type) {
    super(option, mode, type);
  }

  public static IPair getSupportVersion() {
    return SUPPORT_VERSION;
  }

  public static boolean isNoSupportVersion(int version) {
    int[] supportedVersions = SUPPORT_VERSION.getSecond();
    return IntStream.of(supportedVersions).noneMatch(v -> v == version);
  }

  // ==================== 基础版本信息 ====================

  private int mVersion;

  public void setVersion(int version) {
    mVersion = version;
  }

  public int getVersion() {
    return mVersion;
  }

  /** 检查是否为 MQTT v5.0 */
  public boolean isV5() {
    return mVersion == VERSION_V5_0;
  }

  // ==================== MQTT v5.0 协商属性 ====================

  /** CONNACK 协商的属性集合 */
  private QttPropertySet _ConnAckProperties;

  /** 会话过期时间（秒） */
  private long _SessionExpiryInterval;

  /** 服务端接收最大值（流量控制） */
  private int _ReceiveMaximum = 65535; // 默认值

  /** 最大报文大小限制 */
  private int _MaximumPacketSize;

  /** 服务端支持的最大 QoS */
  private int _MaximumQoS = 2;

  /** 特性标志 */
  private boolean _RetainAvailable = true;

  private boolean _WildcardSubscriptionAvailable = true;
  private boolean _SubscriptionIdentifierAvailable = true;
  private boolean _SharedSubscriptionAvailable = true;

  /** 初始化 v5 上下文（在 CONNACK 后调用） */
  public void initV5Context(QttPropertySet connAckProps) {
    if (!isV5() || connAckProps == null) {
      return;
    }

    _ConnAckProperties = connAckProps;

    // 提取常用属性
    Integer receiveMax = connAckProps.getReceiveMaximum();
    if (receiveMax != null) {
      _ReceiveMaximum = receiveMax;
      // 初始化流量控制配额
      _OutboundQuota = new AtomicInteger(_ReceiveMaximum);
    }

    Integer maxPacketSize = connAckProps.getMaximumPacketSize();
    if (maxPacketSize != null) {
      _MaximumPacketSize = maxPacketSize;
    }

    Integer maxQoS = connAckProps.getMaximumQoS();
    if (maxQoS != null) {
      _MaximumQoS = maxQoS;
    }

    Long sessionExpiry = connAckProps.getSessionExpiryInterval();
    if (sessionExpiry != null) {
      _SessionExpiryInterval = sessionExpiry;
    }

    Boolean retainAvailable = connAckProps.isRetainAvailable();
    if (retainAvailable != null) {
      _RetainAvailable = retainAvailable;
    }

    Boolean wildcardAvailable = connAckProps.isWildcardSubscriptionAvailable();
    if (wildcardAvailable != null) {
      _WildcardSubscriptionAvailable = wildcardAvailable;
    }

    Boolean subIdAvailable = connAckProps.isSubscriptionIdentifierAvailable();
    if (subIdAvailable != null) {
      _SubscriptionIdentifierAvailable = subIdAvailable;
    }

    Boolean sharedSubAvailable = connAckProps.isSharedSubscriptionAvailable();
    if (sharedSubAvailable != null) {
      _SharedSubscriptionAvailable = sharedSubAvailable;
    }

    // 初始化主题别名管理器
    Integer topicAliasMax = connAckProps.getTopicAliasMaximum();
    if (topicAliasMax != null && topicAliasMax > 0) {
      _TopicAliasManager = new QttTopicAlias(topicAliasMax);
    }

    _Logger.debug(
        "V5 context initialized: recvMax=%d, maxPacket=%d, topicAliasMax=%d",
        _ReceiveMaximum, _MaximumPacketSize, topicAliasMax != null ? topicAliasMax : 0);
  }

  // ----- 属性访问器 -----

  public QttPropertySet getConnAckProperties() {
    return _ConnAckProperties;
  }

  public long getSessionExpiryInterval() {
    return _SessionExpiryInterval;
  }

  public void setSessionExpiryInterval(long seconds) {
    _SessionExpiryInterval = seconds;
  }

  public int getReceiveMaximum() {
    return _ReceiveMaximum;
  }

  public void setReceiveMaximum(int value) {
    _ReceiveMaximum = value;
    if (_OutboundQuota == null) {
      _OutboundQuota = new AtomicInteger(value);
    } else {
      _OutboundQuota.set(value);
    }
  }

  public int getMaximumPacketSize() {
    return _MaximumPacketSize;
  }

  public void setMaximumPacketSize(int size) {
    _MaximumPacketSize = size;
  }

  public int getMaximumQoS() {
    return _MaximumQoS;
  }

  public void setMaximumQoS(int qos) {
    _MaximumQoS = qos;
  }

  public boolean isRetainAvailable() {
    return _RetainAvailable;
  }

  public void setRetainAvailable(boolean available) {
    _RetainAvailable = available;
  }

  public boolean isWildcardSubscriptionAvailable() {
    return _WildcardSubscriptionAvailable;
  }

  public void setWildcardSubscriptionAvailable(boolean available) {
    _WildcardSubscriptionAvailable = available;
  }

  public boolean isSubscriptionIdentifierAvailable() {
    return _SubscriptionIdentifierAvailable;
  }

  public void setSubscriptionIdentifierAvailable(boolean available) {
    _SubscriptionIdentifierAvailable = available;
  }

  public boolean isSharedSubscriptionAvailable() {
    return _SharedSubscriptionAvailable;
  }

  public void setSharedSubscriptionAvailable(boolean available) {
    _SharedSubscriptionAvailable = available;
  }

  // ==================== 主题别名管理器 ====================

  private QttTopicAlias _TopicAliasManager;

  /** 获取主题别名管理器 */
  public QttTopicAlias getTopicAliasManager() {
    if (_TopicAliasManager == null && isV5()) {
      // 延迟初始化，使用默认值
      _TopicAliasManager = new QttTopicAlias(0);
    }
    return _TopicAliasManager;
  }

  /** 设置主题别名管理器 */
  public void setTopicAliasManager(QttTopicAlias manager) {
    _TopicAliasManager = manager;
  }

  /** 检查主题别名是否可用 */
  public boolean isTopicAliasEnabled() {
    return _TopicAliasManager != null && _TopicAliasManager.isEnabled();
  }

  // ==================== 流量控制（Receive Maximum）====================

  /**
   * 出站配额计数器（用于 QoS > 0 的消息）
   *
   * <p>表示当前还可以发送的 QoS 1/2 消息数量
   */
  private AtomicInteger _OutboundQuota;

  /**
   * 入站配额计数器（用于 QoS > 0 的消息）
   *
   * <p>表示对端还可以发送的 QoS 1/2 消息数量
   */
  private AtomicInteger _InboundQuota;

  /**
   * 检查是否可以发送 QoS > 0 的消息
   *
   * <p>根据 MQTT v5.0 流量控制机制，需要确保不超过 Receive Maximum
   *
   * @return true 如果可以发送
   */
  public boolean canSendQoSMessage() {
    if (!isV5() || _OutboundQuota == null) {
      return true; // v3.1.1 或无限制
    }
    return _OutboundQuota.get() > 0;
  }

  /**
   * 尝试获取发送配额
   *
   * <p>在发送 QoS 1/2 消息前调用
   *
   * @return true 如果成功获取配额
   */
  public boolean acquireSendQuota() {
    if (!isV5() || _OutboundQuota == null) {
      return true;
    }
    int remaining = _OutboundQuota.decrementAndGet();
    if (remaining < 0) {
      // 超出配额，恢复并返回失败
      _OutboundQuota.incrementAndGet();
      _Logger.warning("Send quota exceeded, receiveMaximum=%d", _ReceiveMaximum);
      return false;
    }
    return true;
  }

  /**
   * 释放发送配额
   *
   * <p>在收到 PUBACK/PUBCOMP 后调用
   */
  public void releaseSendQuota() {
    if (!isV5() || _OutboundQuota == null) {
      return;
    }
    int newValue = _OutboundQuota.incrementAndGet();
    if (newValue > _ReceiveMaximum) {
      // 防止溢出，恢复到最大值
      _OutboundQuota.set(_ReceiveMaximum);
    }
  }

  /** 获取当前剩余发送配额 */
  public int getRemainingSendQuota() {
    if (_OutboundQuota == null) {
      return isV5() ? 0 : -1; // -1 表示无限制
    }
    return _OutboundQuota.get();
  }

  /** 设置入站配额（对端的 Receive Maximum） */
  public void setInboundQuota(int quota) {
    if (_InboundQuota == null) {
      _InboundQuota = new AtomicInteger(quota);
    } else {
      _InboundQuota.set(quota);
    }
  }

  /**
   * 检查是否可以接收 QoS > 0 的消息
   *
   * <p>根据对端的 Receive Maximum
   */
  public boolean canReceiveQoSMessage() {
    if (!isV5() || _InboundQuota == null) {
      return true;
    }
    return _InboundQuota.get() > 0;
  }

  /** 获取当前入站配额 */
  public int getInboundQuota() {
    return _InboundQuota != null ? _InboundQuota.get() : -1;
  }

  // ==================== 报文大小检查 ====================

  /**
   * 检查报文大小是否超过限制
   *
   * @param packetSize 报文大小
   * @return true 如果报文大小合法
   */
  public boolean isPacketSizeValid(int packetSize) {
    if (!isV5() || _MaximumPacketSize <= 0) {
      return true; // 无限制
    }
    return packetSize <= _MaximumPacketSize;
  }

  /** 获取允许的最大报文大小 */
  public int getAllowedMaxPacketSize() {
    return _MaximumPacketSize > 0 ? _MaximumPacketSize : Integer.MAX_VALUE;
  }

  // ==================== 生命周期方法 ====================

  @Override
  public void ready() {
    advanceOutState(ENCODE_PAYLOAD);
    advanceInState(DECODE_FRAME);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("QttContext{version=%d", mVersion));
    if (isV5()) {
      sb.append(
          String.format(
              ", recvMax=%d, maxPacket=%d, quota=%d/%d",
              _ReceiveMaximum, _MaximumPacketSize, getRemainingSendQuota(), _ReceiveMaximum));
      if (_TopicAliasManager != null) {
        sb.append(String.format(", alias=%s", _TopicAliasManager));
      }
    }
    sb.append("}");
    return sb.toString();
  }
}
