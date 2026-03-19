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

package com.isahl.chess.pawn.endpoint.device.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT 配置类
 *
 * <p>配置项前缀：z.chess.mqtt
 *
 * @author william.d.zk
 */
@Configuration
@ConfigurationProperties(prefix = "z.chess.mqtt")
public class MqttConfig {
  // ==================== 特性开关 ====================

  /** 是否启用 MQTT v5 支持 */
  private boolean enabled = true;

  /** 是否启用主题别名 */
  private boolean topicAliasEnabled = true;

  /** 是否启用消息过期 */
  private boolean messageExpiryEnabled = true;

  /** 是否启用流量控制 */
  private boolean flowControlEnabled = true;

  /** 是否启用共享订阅 */
  private boolean sharedSubscriptionEnabled = true;

  /** 是否启用增强认证 */
  private boolean enhancedAuthEnabled = false;

  // ==================== 会话配置 ====================

  /**
   * 默认会话过期时间（秒）
   *
   * <p>0 表示会话随连接断开而结束
   */
  private long defaultSessionExpiryInterval = 0;

  /** 最大会话过期时间（秒） */
  private long maxSessionExpiryInterval = 604800; // 7 天

  // ==================== 流量控制配置 ====================

  /**
   * 服务端接收最大值（Receive Maximum）
   *
   * <p>限制同时处理的 QoS 1/2 消息数量
   */
  private int serverReceiveMaximum = 65535;

  /**
   * 要求客户端的接收最大值
   *
   * <p>0 表示不限制客户端
   */
  private int clientReceiveMaximum = 0;

  // ==================== 主题别名配置 ====================

  /** 服务端主题别名最大值 */
  private int serverTopicAliasMaximum = 65535;

  /**
   * 客户端主题别名最大值
   *
   * <p>0 表示不接受客户端的主题别名
   */
  private int clientTopicAliasMaximum = 0;

  // ==================== 报文大小配置 ====================

  /**
   * 最大报文大小（字节）
   *
   * <p>0 表示不限制
   */
  private int maximumPacketSize = 268435455; // 约 256MB

  /** 默认最大报文大小（当客户端未指定时） */
  private int defaultMaximumPacketSize = 65536; // 64KB

  // ==================== 特性支持配置 ====================

  /** 是否支持保留消息 */
  private boolean retainAvailable = true;

  /** 最大 QoS 等级（0/1/2） */
  private int maximumQoS = 2;

  /** 是否支持通配符订阅 */
  private boolean wildcardSubscriptionAvailable = true;

  /** 是否支持订阅标识符 */
  private boolean subscriptionIdentifierAvailable = true;

  /** 是否支持共享订阅 */
  private boolean sharedSubscriptionAvailable = true;

  // ==================== 遗嘱配置 ====================

  /** 最大遗嘱延迟时间（秒） */
  private long maxWillDelayInterval = 86400; // 1 天

  /** 默认遗嘱延迟时间（秒） */
  private long defaultWillDelayInterval = 0;

  // ==================== 增强认证配置 ====================

  /** 支持的认证方法列表 */
  private List<String> supportedAuthMethods = Arrays.asList();

  /** 默认认证方法 */
  private String defaultAuthMethod = null;

  /** 认证超时时间（秒） */
  private int authTimeoutSeconds = 60;

  /** 是否允许重新认证 */
  private boolean reauthenticationAllowed = true;

  // ==================== 性能调优 ====================

  /** 主题别名缓存大小 */
  private int topicAliasCacheSize = 1000;

  /** 消息过期检查间隔（毫秒） */
  private long messageExpiryCheckInterval = 1000;

  /** 最大待处理消息数（流量控制） */
  private int maxPendingMessages = 1000;

  // ==================== Getter & Setter ====================

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isTopicAliasEnabled() {
    return enabled && topicAliasEnabled;
  }

  public void setTopicAliasEnabled(boolean topicAliasEnabled) {
    this.topicAliasEnabled = topicAliasEnabled;
  }

  public boolean isMessageExpiryEnabled() {
    return enabled && messageExpiryEnabled;
  }

  public void setMessageExpiryEnabled(boolean messageExpiryEnabled) {
    this.messageExpiryEnabled = messageExpiryEnabled;
  }

  public boolean isFlowControlEnabled() {
    return enabled && flowControlEnabled;
  }

  public void setFlowControlEnabled(boolean flowControlEnabled) {
    this.flowControlEnabled = flowControlEnabled;
  }

  public boolean isSharedSubscriptionEnabled() {
    return enabled && sharedSubscriptionEnabled;
  }

  public void setSharedSubscriptionEnabled(boolean sharedSubscriptionEnabled) {
    this.sharedSubscriptionEnabled = sharedSubscriptionEnabled;
  }

  public boolean isEnhancedAuthEnabled() {
    return enabled && enhancedAuthEnabled;
  }

  public void setEnhancedAuthEnabled(boolean enhancedAuthEnabled) {
    this.enhancedAuthEnabled = enhancedAuthEnabled;
  }

  public long getDefaultSessionExpiryInterval() {
    return defaultSessionExpiryInterval;
  }

  public void setDefaultSessionExpiryInterval(long defaultSessionExpiryInterval) {
    this.defaultSessionExpiryInterval = defaultSessionExpiryInterval;
  }

  public long getMaxSessionExpiryInterval() {
    return maxSessionExpiryInterval;
  }

  public void setMaxSessionExpiryInterval(long maxSessionExpiryInterval) {
    this.maxSessionExpiryInterval = maxSessionExpiryInterval;
  }

  public int getServerReceiveMaximum() {
    return serverReceiveMaximum;
  }

  public void setServerReceiveMaximum(int serverReceiveMaximum) {
    this.serverReceiveMaximum = serverReceiveMaximum;
  }

  public int getClientReceiveMaximum() {
    return clientReceiveMaximum;
  }

  public void setClientReceiveMaximum(int clientReceiveMaximum) {
    this.clientReceiveMaximum = clientReceiveMaximum;
  }

  public int getServerTopicAliasMaximum() {
    return serverTopicAliasMaximum;
  }

  public void setServerTopicAliasMaximum(int serverTopicAliasMaximum) {
    this.serverTopicAliasMaximum = serverTopicAliasMaximum;
  }

  public int getClientTopicAliasMaximum() {
    return clientTopicAliasMaximum;
  }

  public void setClientTopicAliasMaximum(int clientTopicAliasMaximum) {
    this.clientTopicAliasMaximum = clientTopicAliasMaximum;
  }

  public int getMaximumPacketSize() {
    return maximumPacketSize;
  }

  public void setMaximumPacketSize(int maximumPacketSize) {
    this.maximumPacketSize = maximumPacketSize;
  }

  public int getDefaultMaximumPacketSize() {
    return defaultMaximumPacketSize;
  }

  public void setDefaultMaximumPacketSize(int defaultMaximumPacketSize) {
    this.defaultMaximumPacketSize = defaultMaximumPacketSize;
  }

  public boolean isRetainAvailable() {
    return enabled && retainAvailable;
  }

  public void setRetainAvailable(boolean retainAvailable) {
    this.retainAvailable = retainAvailable;
  }

  public int getMaximumQoS() {
    return enabled ? maximumQoS : 1; // v3.1.1 只支持到 QoS 1
  }

  public void setMaximumQoS(int maximumQoS) {
    this.maximumQoS = Math.min(2, Math.max(0, maximumQoS));
  }

  public boolean isWildcardSubscriptionAvailable() {
    return enabled && wildcardSubscriptionAvailable;
  }

  public void setWildcardSubscriptionAvailable(boolean wildcardSubscriptionAvailable) {
    this.wildcardSubscriptionAvailable = wildcardSubscriptionAvailable;
  }

  public boolean isSubscriptionIdentifierAvailable() {
    return enabled && subscriptionIdentifierAvailable;
  }

  public void setSubscriptionIdentifierAvailable(boolean subscriptionIdentifierAvailable) {
    this.subscriptionIdentifierAvailable = subscriptionIdentifierAvailable;
  }

  public boolean isSharedSubscriptionAvailable() {
    return enabled && sharedSubscriptionAvailable;
  }

  public void setSharedSubscriptionAvailable(boolean sharedSubscriptionAvailable) {
    this.sharedSubscriptionAvailable = sharedSubscriptionAvailable;
  }

  public long getMaxWillDelayInterval() {
    return maxWillDelayInterval;
  }

  public void setMaxWillDelayInterval(long maxWillDelayInterval) {
    this.maxWillDelayInterval = maxWillDelayInterval;
  }

  public long getDefaultWillDelayInterval() {
    return defaultWillDelayInterval;
  }

  public void setDefaultWillDelayInterval(long defaultWillDelayInterval) {
    this.defaultWillDelayInterval = defaultWillDelayInterval;
  }

  public List<String> getSupportedAuthMethods() {
    return supportedAuthMethods;
  }

  public void setSupportedAuthMethods(List<String> supportedAuthMethods) {
    this.supportedAuthMethods = supportedAuthMethods;
  }

  public String getDefaultAuthMethod() {
    return defaultAuthMethod;
  }

  public void setDefaultAuthMethod(String defaultAuthMethod) {
    this.defaultAuthMethod = defaultAuthMethod;
  }

  public int getAuthTimeoutSeconds() {
    return authTimeoutSeconds;
  }

  public void setAuthTimeoutSeconds(int authTimeoutSeconds) {
    this.authTimeoutSeconds = authTimeoutSeconds;
  }

  public boolean isReauthenticationAllowed() {
    return enabled && reauthenticationAllowed;
  }

  public void setReauthenticationAllowed(boolean reauthenticationAllowed) {
    this.reauthenticationAllowed = reauthenticationAllowed;
  }

  public int getTopicAliasCacheSize() {
    return topicAliasCacheSize;
  }

  public void setTopicAliasCacheSize(int topicAliasCacheSize) {
    this.topicAliasCacheSize = topicAliasCacheSize;
  }

  public long getMessageExpiryCheckInterval() {
    return messageExpiryCheckInterval;
  }

  public void setMessageExpiryCheckInterval(long messageExpiryCheckInterval) {
    this.messageExpiryCheckInterval = messageExpiryCheckInterval;
  }

  public int getMaxPendingMessages() {
    return maxPendingMessages;
  }

  public void setMaxPendingMessages(int maxPendingMessages) {
    this.maxPendingMessages = maxPendingMessages;
  }

  // ==================== 便捷方法 ====================

  /** 检查是否支持指定的认证方法 */
  public boolean isAuthMethodSupported(String method) {
    return enabled
        && enhancedAuthEnabled
        && supportedAuthMethods != null
        && supportedAuthMethods.contains(method);
  }

  /** 获取有效的主题别名最大值（取客户端和服务器配置的最小值） */
  public int getEffectiveTopicAliasMaximum(int clientRequested) {
    if (!isTopicAliasEnabled()) {
      return 0;
    }
    int max = Math.min(serverTopicAliasMaximum, clientRequested);
    return Math.max(0, max);
  }

  /** 获取有效的会话过期时间 */
  public long getEffectiveSessionExpiryInterval(long clientRequested) {
    if (clientRequested == 0) {
      return 0;
    }
    if (clientRequested > maxSessionExpiryInterval) {
      return maxSessionExpiryInterval;
    }
    return clientRequested;
  }

  @Override
  public String toString() {
    return String.format(
        "MqttConfig{enabled=%s, alias=%s, expiry=%s, flow=%s, shared=%s, auth=%s, "
            + "recvMax=%d, topicAliasMax=%d, maxQoS=%d}",
        enabled,
        topicAliasEnabled,
        messageExpiryEnabled,
        flowControlEnabled,
        sharedSubscriptionEnabled,
        enhancedAuthEnabled,
        serverReceiveMaximum,
        serverTopicAliasMaximum,
        maximumQoS);
  }
}
