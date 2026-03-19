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

import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V5_0;

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * MQTT v5.0 属性集合容器
 *
 * <p>管理 MQTT v5 报文中的所有属性，支持属性的编码、解码和存储。 属性采用 TLV (Type-Length-Value) 格式编码。
 *
 * @author william.d.zk
 * @see QttProperty
 */
public class QttPropertySet {
  private static final Logger _Logger =
      Logger.getLogger("protocol.bishop." + QttPropertySet.class.getSimpleName());

  /** 单值属性存储 (PropertyId -> Value) */
  private final Map<Integer, Object> _Properties = new HashMap<>();

  /** 用户属性列表 (可重复) */
  private final List<Pair<String, String>> _UserProperties = new ArrayList<>();

  /** 订阅标识符列表 (可重复) */
  private final List<Integer> _SubscriptionIds = new ArrayList<>();

  // ==================== 属性设置方法 ====================

  /**
   * 设置单值属性
   *
   * @param property 属性类型
   * @param value 属性值
   * @throws IllegalArgumentException 如果属性为可重复属性
   */
  public void setProperty(QttProperty property, Object value) {
    if (property.isRepeatable()) {
      throw new IllegalArgumentException(
          "Property " + property + " is repeatable, use add method instead");
    }
    validateValueType(property, value);
    _Properties.put(property.getId(), value);
  }

  /**
   * 获取单值属性
   *
   * @param property 属性类型
   * @param <T> 返回值类型
   * @return 属性值，不存在返回 null
   */
  @SuppressWarnings("unchecked")
  public <T> T getProperty(QttProperty property) {
    if (property.isRepeatable()) {
      throw new IllegalArgumentException(
          "Property " + property + " is repeatable, use getList method instead");
    }
    return (T) _Properties.get(property.getId());
  }

  /** 检查属性是否存在 */
  public boolean hasProperty(QttProperty property) {
    if (property.isRepeatable()) {
      return switch (property) {
        case USER_PROPERTY -> !_UserProperties.isEmpty();
        case SUBSCRIPTION_IDENTIFIER -> !_SubscriptionIds.isEmpty();
        default -> false;
      };
    }
    return _Properties.containsKey(property.getId());
  }

  /** 移除属性 */
  public void removeProperty(QttProperty property) {
    if (property.isRepeatable()) {
      switch (property) {
        case USER_PROPERTY -> _UserProperties.clear();
        case SUBSCRIPTION_IDENTIFIER -> _SubscriptionIds.clear();
      }
    } else {
      _Properties.remove(property.getId());
    }
  }

  // ==================== 可重复属性方法 ====================

  /**
   * 添加用户属性
   *
   * @param key 属性名
   * @param value 属性值
   */
  public void addUserProperty(String key, String value) {
    Objects.requireNonNull(key, "User property key cannot be null");
    Objects.requireNonNull(value, "User property value cannot be null");
    _UserProperties.add(Pair.of(key, value));
  }

  /** 获取所有用户属性 */
  public List<Pair<String, String>> getUserProperties() {
    return Collections.unmodifiableList(_UserProperties);
  }

  /** 添加订阅标识符 */
  public void addSubscriptionId(int subscriptionId) {
    if (subscriptionId <= 0) {
      throw new IllegalArgumentException(
          "Subscription identifier must be positive: " + subscriptionId);
    }
    _SubscriptionIds.add(subscriptionId);
  }

  /** 获取所有订阅标识符 */
  public List<Integer> getSubscriptionIds() {
    return Collections.unmodifiableList(_SubscriptionIds);
  }

  // ==================== 编解码方法 ====================

  /**
   * 从缓冲区解码属性
   *
   * @param input 输入缓冲区
   * @param version 协议版本
   */
  public void decode(ByteBuf input, int version) {
    if (version < VERSION_V5_0) {
      return; // v3.1.1 不支持属性
    }

    // 读取属性长度（变长编码）
    int propertiesLength = input.vLength();
    if (propertiesLength == 0) {
      return;
    }

    int endPosition = input.readerIdx() + propertiesLength;

    while (input.readerIdx() < endPosition) {
      int propertyId = input.vLength();
      QttProperty property = QttProperty.valueOf(propertyId);

      if (property == null) {
        _Logger.warning("Unknown property id: 0x%02X, skipping", propertyId);
        // 跳过未知属性：需要根据类型猜测长度，这里简化处理
        break;
      }

      decodeProperty(input, property);
    }
  }

  /** 解码单个属性 */
  private void decodeProperty(ByteBuf input, QttProperty property) {
    try {
      Object value =
          switch (property.getType()) {
            case BYTE -> input.getUnsigned();
            case TWO_BYTE_INTEGER -> input.getUnsignedShort();
            case FOUR_BYTE_INTEGER -> input.getInt();
            case VARIABLE_BYTE_INTEGER -> input.vLength();
            case UTF_8_ENCODED_STRING -> readUtf8String(input);
            case UTF_8_STRING_PAIR -> readUtf8StringPair(input);
            case BINARY_DATA -> readBinaryData(input);
          };

      if (property.isRepeatable()) {
        switch (property) {
          case USER_PROPERTY -> {
            @SuppressWarnings("unchecked")
            Pair<String, String> pair = (Pair<String, String>) value;
            _UserProperties.add(pair);
          }
          case SUBSCRIPTION_IDENTIFIER -> _SubscriptionIds.add((Integer) value);
        }
      } else {
        _Properties.put(property.getId(), value);
      }
    } catch (Exception e) {
      _Logger.warning("Failed to decode property: %s", e, property);
    }
  }

  /**
   * 编码属性到缓冲区
   *
   * @param output 输出缓冲区
   * @param version 协议版本
   * @return 输出缓冲区
   */
  public ByteBuf encode(ByteBuf output, int version) {
    if (version < VERSION_V5_0) {
      return output; // v3.1.1 不编码属性
    }

    int propertiesLength = length(version);
    if (propertiesLength == 0) {
      output.put(0); // 零长度属性
      return output;
    }

    // 写入属性长度
    output.vPutLength(propertiesLength);

    // 编码单值属性
    for (Map.Entry<Integer, Object> entry : _Properties.entrySet()) {
      QttProperty property = QttProperty.valueOf(entry.getKey());
      if (property != null) {
        encodeProperty(output, property, entry.getValue());
      }
    }

    // 编码用户属性
    for (Pair<String, String> pair : _UserProperties) {
      output.put(QttProperty.USER_PROPERTY.getId());
      writeUtf8StringPair(output, pair);
    }

    // 编码订阅标识符
    for (Integer subscriptionId : _SubscriptionIds) {
      output.put(QttProperty.SUBSCRIPTION_IDENTIFIER.getId());
      output.vPutLength(subscriptionId);
    }

    return output;
  }

  /** 编码单个属性 */
  private void encodeProperty(ByteBuf output, QttProperty property, Object value) {
    output.put(property.getId());

    switch (property.getType()) {
      case BYTE -> output.put((Byte) value);
      case TWO_BYTE_INTEGER -> output.putShort((Integer) value);
      case FOUR_BYTE_INTEGER -> output.putInt((Integer) value);
      case VARIABLE_BYTE_INTEGER -> output.vPutLength((Integer) value);
      case UTF_8_ENCODED_STRING -> writeUtf8String(output, (String) value);
      case UTF_8_STRING_PAIR -> writeUtf8StringPair(output, (Pair<String, String>) value);
      case BINARY_DATA -> writeBinaryData(output, (byte[]) value);
    }
  }

  /**
   * 计算属性总长度
   *
   * @param version 协议版本
   * @return 属性总长度（不包括长度字段本身）
   */
  public int length(int version) {
    if (version < VERSION_V5_0) {
      return 0;
    }

    int length = 0;

    // 单值属性
    for (Map.Entry<Integer, Object> entry : _Properties.entrySet()) {
      QttProperty property = QttProperty.valueOf(entry.getKey());
      if (property != null) {
        length += 1; // 属性标识符长度
        length += getValueLength(property, entry.getValue());
      }
    }

    // 用户属性
    for (Pair<String, String> pair : _UserProperties) {
      length += 1; // 标识符
      length += getUtf8StringPairLength(pair);
    }

    // 订阅标识符
    for (Integer subscriptionId : _SubscriptionIds) {
      length += 1; // 标识符
      length += ByteBuf.vSizeOf(subscriptionId); // 变长编码
    }

    return length;
  }

  // ==================== 辅助方法 ====================

  private void validateValueType(QttProperty property, Object value) {
    if (value == null) return;

    boolean valid =
        switch (property.getType()) {
          case BYTE -> value instanceof Byte || value instanceof Integer;
          case TWO_BYTE_INTEGER, FOUR_BYTE_INTEGER, VARIABLE_BYTE_INTEGER -> value
              instanceof Integer;
          case UTF_8_ENCODED_STRING -> value instanceof String;
          case UTF_8_STRING_PAIR -> value instanceof Pair;
          case BINARY_DATA -> value instanceof byte[];
        };

    if (!valid) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid value type for property %s: expected %s, got %s",
              property, property.getType(), value.getClass().getSimpleName()));
    }
  }

  private int getValueLength(QttProperty property, Object value) {
    if (value == null) return 0;

    return switch (property.getType()) {
      case BYTE -> 1;
      case TWO_BYTE_INTEGER -> 2;
      case FOUR_BYTE_INTEGER -> 4;
      case VARIABLE_BYTE_INTEGER -> ByteBuf.vSizeOf((Integer) value);
      case UTF_8_ENCODED_STRING -> getUtf8StringLength((String) value);
      case UTF_8_STRING_PAIR -> getUtf8StringPairLength((Pair<String, String>) value);
      case BINARY_DATA -> getBinaryDataLength((byte[]) value);
    };
  }

  private String readUtf8String(ByteBuf input) {
    int length = input.getUnsignedShort();
    if (length == 0) return "";
    return input.readUTF(length);
  }

  private void writeUtf8String(ByteBuf output, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    output.putShort(bytes.length);
    output.put(bytes);
  }

  private int getUtf8StringLength(String value) {
    if (value == null) return 2; // 空字符串长度
    return 2 + value.getBytes(StandardCharsets.UTF_8).length;
  }

  @SuppressWarnings("unchecked")
  private Pair<String, String> readUtf8StringPair(ByteBuf input) {
    String key = readUtf8String(input);
    String value = readUtf8String(input);
    return Pair.of(key, value);
  }

  private void writeUtf8StringPair(ByteBuf output, Pair<String, String> pair) {
    writeUtf8String(output, pair.getFirst());
    writeUtf8String(output, pair.getSecond());
  }

  private int getUtf8StringPairLength(Pair<String, String> pair) {
    return getUtf8StringLength(pair.getFirst()) + getUtf8StringLength(pair.getSecond());
  }

  private byte[] readBinaryData(ByteBuf input) {
    int length = input.getUnsignedShort();
    if (length == 0) return new byte[0];
    byte[] data = new byte[length];
    input.get(data);
    return data;
  }

  private void writeBinaryData(ByteBuf output, byte[] data) {
    output.putShort(data.length);
    output.put(data);
  }

  private int getBinaryDataLength(byte[] data) {
    return 2 + (data == null ? 0 : data.length);
  }

  // ==================== 便捷方法 ====================

  /** 获取会话过期时间（秒） */
  public Long getSessionExpiryInterval() {
    return getProperty(QttProperty.SESSION_EXPIRY_INTERVAL);
  }

  /** 设置会话过期时间（秒） */
  public void setSessionExpiryInterval(long seconds) {
    setProperty(QttProperty.SESSION_EXPIRY_INTERVAL, (int) seconds);
  }

  /** 获取接收最大值 */
  public Integer getReceiveMaximum() {
    return getProperty(QttProperty.RECEIVE_MAXIMUM);
  }

  /** 设置接收最大值 */
  public void setReceiveMaximum(int value) {
    setProperty(QttProperty.RECEIVE_MAXIMUM, value);
  }

  /** 获取主题别名最大值 */
  public Integer getTopicAliasMaximum() {
    return getProperty(QttProperty.TOPIC_ALIAS_MAXIMUM);
  }

  /** 设置主题别名最大值 */
  public void setTopicAliasMaximum(int value) {
    setProperty(QttProperty.TOPIC_ALIAS_MAXIMUM, value);
  }

  /** 获取最大报文大小 */
  public Integer getMaximumPacketSize() {
    return getProperty(QttProperty.MAXIMUM_PACKET_SIZE);
  }

  /** 设置最大报文大小 */
  public void setMaximumPacketSize(int size) {
    setProperty(QttProperty.MAXIMUM_PACKET_SIZE, size);
  }

  /** 获取消息过期时间（秒） */
  public Long getMessageExpiryInterval() {
    return getProperty(QttProperty.MESSAGE_EXPIRY_INTERVAL);
  }

  /** 设置消息过期时间（秒） */
  public void setMessageExpiryInterval(long seconds) {
    setProperty(QttProperty.MESSAGE_EXPIRY_INTERVAL, (int) seconds);
  }

  /** 获取主题别名 */
  public Integer getTopicAlias() {
    return getProperty(QttProperty.TOPIC_ALIAS);
  }

  /** 设置主题别名 */
  public void setTopicAlias(int alias) {
    setProperty(QttProperty.TOPIC_ALIAS, alias);
  }

  /** 获取内容类型 */
  public String getContentType() {
    return getProperty(QttProperty.CONTENT_TYPE);
  }

  /** 设置内容类型 */
  public void setContentType(String contentType) {
    setProperty(QttProperty.CONTENT_TYPE, contentType);
  }

  /** 获取响应主题 */
  public String getResponseTopic() {
    return getProperty(QttProperty.RESPONSE_TOPIC);
  }

  /** 设置响应主题 */
  public void setResponseTopic(String topic) {
    setProperty(QttProperty.RESPONSE_TOPIC, topic);
  }

  /** 获取关联数据 */
  public byte[] getCorrelationData() {
    return getProperty(QttProperty.CORRELATION_DATA);
  }

  /** 设置关联数据 */
  public void setCorrelationData(byte[] data) {
    setProperty(QttProperty.CORRELATION_DATA, data);
  }

  /** 获取认证方法 */
  public String getAuthenticationMethod() {
    return getProperty(QttProperty.AUTHENTICATION_METHOD);
  }

  /** 设置认证方法 */
  public void setAuthenticationMethod(String method) {
    setProperty(QttProperty.AUTHENTICATION_METHOD, method);
  }

  /** 获取认证数据 */
  public byte[] getAuthenticationData() {
    return getProperty(QttProperty.AUTHENTICATION_DATA);
  }

  /** 设置认证数据 */
  public void setAuthenticationData(byte[] data) {
    setProperty(QttProperty.AUTHENTICATION_DATA, data);
  }

  /** 获取原因字符串 */
  public String getReasonString() {
    return getProperty(QttProperty.REASON_STRING);
  }

  /** 设置原因字符串 */
  public void setReasonString(String reason) {
    setProperty(QttProperty.REASON_STRING, reason);
  }

  /** 获取服务器引用（重定向用） */
  public String getServerReference() {
    return getProperty(QttProperty.SERVER_REFERENCE);
  }

  /** 设置服务器引用 */
  public void setServerReference(String reference) {
    setProperty(QttProperty.SERVER_REFERENCE, reference);
  }

  /** 检查是否支持保留消息 */
  public Boolean isRetainAvailable() {
    Integer value = getProperty(QttProperty.RETAIN_AVAILABLE);
    return value == null ? null : value == 1;
  }

  /** 设置保留消息可用性 */
  public void setRetainAvailable(boolean available) {
    setProperty(QttProperty.RETAIN_AVAILABLE, available);
  }

  /** 检查是否支持通配符订阅 */
  public Boolean isWildcardSubscriptionAvailable() {
    Integer value = getProperty(QttProperty.WILDCARD_SUBSCRIPTION_AVAILABLE);
    return value == null ? null : value == 1;
  }

  /** 设置通配符订阅可用性 */
  public void setWildcardSubscriptionAvailable(boolean available) {
    setProperty(QttProperty.WILDCARD_SUBSCRIPTION_AVAILABLE, available ? 1 : 0);
  }

  /** 检查是否支持共享订阅 */
  public Boolean isSharedSubscriptionAvailable() {
    Integer value = getProperty(QttProperty.SHARED_SUBSCRIPTION_AVAILABLE);
    return value == null ? null : value == 1;
  }

  /** 设置共享订阅可用性 */
  public void setSharedSubscriptionAvailable(boolean available) {
    setProperty(QttProperty.SHARED_SUBSCRIPTION_AVAILABLE, available ? 1 : 0);
  }

  /** 获取最大 QoS */
  public Integer getMaximumQoS() {
    return getProperty(QttProperty.MAXIMUM_QOS);
  }

  /** 设置最大 QoS */
  public void setMaximumQoS(int qos) {
    setProperty(QttProperty.MAXIMUM_QOS, qos);
  }

  /** 获取分配的客户端标识符 */
  public String getAssignedClientIdentifier() {
    return getProperty(QttProperty.ASSIGNED_CLIENT_IDENTIFIER);
  }

  /** 设置分配的客户端标识符 */
  public void setAssignedClientIdentifier(String clientId) {
    setProperty(QttProperty.ASSIGNED_CLIENT_IDENTIFIER, clientId);
  }

  /** 获取响应信息 */
  public String getResponseInformation() {
    return getProperty(QttProperty.RESPONSE_INFORMATION);
  }

  /** 设置响应信息 */
  public void setResponseInformation(String info) {
    setProperty(QttProperty.RESPONSE_INFORMATION, info);
  }

  /** 检查是否支持订阅标识符 */
  public Boolean isSubscriptionIdentifierAvailable() {
    Integer value = getProperty(QttProperty.SUBSCRIPTION_IDENTIFIER_AVAILABLE);
    return value == null ? null : value == 1;
  }

  /** 设置订阅标识符可用性 */
  public void setSubscriptionIdentifierAvailable(boolean available) {
    setProperty(QttProperty.SUBSCRIPTION_IDENTIFIER_AVAILABLE, available ? 1 : 0);
  }

  /** 清空所有属性 */
  public void clear() {
    _Properties.clear();
    _UserProperties.clear();
    _SubscriptionIds.clear();
  }

  /** 检查是否为空 */
  public boolean isEmpty() {
    return _Properties.isEmpty() && _UserProperties.isEmpty() && _SubscriptionIds.isEmpty();
  }

  @Override
  public String toString() {
    return String.format(
        "QttPropertySet{single=%d, userProps=%d, subIds=%d}",
        _Properties.size(), _UserProperties.size(), _SubscriptionIds.size());
  }
}
