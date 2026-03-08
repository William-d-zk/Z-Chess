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

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MQTT v5.0 协议属性定义
 * 
 * 属性是 MQTT v5 的核心扩展机制，几乎所有报文都可以携带属性。
 * 属性采用 TLV (Type-Length-Value) 格式编码。
 * 
 * @author william.d.zk
 * @see <a href="https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html">MQTT v5.0 Specification</a>
 */
public enum QttProperty
{
    // ==================== 单字节属性 (0x00-0x7F) ====================
    
    /**
     * 载荷格式指示器 (0x01)
     * 值: 0 = 未指定字节流, 1 = UTF-8 编码字符串
     * 适用: PUBLISH, Will Properties
     */
    PAYLOAD_FORMAT_INDICATOR(0x01, PropertyType.BYTE),
    
    /**
     * 请求问题信息 (0x17)
     * 值: 0 = 不发送原因字符串和用户属性, 1 = 可以发送
     * 适用: CONNECT
     */
    REQUEST_PROBLEM_INFORMATION(0x17, PropertyType.BYTE),
    
    /**
     * 请求响应信息 (0x19)
     * 值: 0 = 不发送响应信息, 1 = 可以发送
     * 适用: CONNECT
     */
    REQUEST_RESPONSE_INFORMATION(0x19, PropertyType.BYTE),
    
    /**
     * 最大 QoS (0x24)
     * 值: 0 或 1, 表示服务端支持的最大 QoS 等级
     * 适用: CONNACK
     */
    MAXIMUM_QOS(0x24, PropertyType.BYTE),
    
    /**
     * 保留消息可用性 (0x25)
     * 值: 0 = 不支持保留消息, 1 = 支持
     * 适用: CONNACK
     */
    RETAIN_AVAILABLE(0x25, PropertyType.BYTE),
    
    /**
     * 通配符订阅可用性 (0x28)
     * 值: 0 = 不支持通配符订阅, 1 = 支持
     * 适用: CONNACK
     */
    WILDCARD_SUBSCRIPTION_AVAILABLE(0x28, PropertyType.BYTE),
    
    /**
     * 订阅标识符可用性 (0x29)
     * 值: 0 = 不支持订阅标识符, 1 = 支持
     * 适用: CONNACK
     */
    SUBSCRIPTION_IDENTIFIER_AVAILABLE(0x29, PropertyType.BYTE),
    
    /**
     * 共享订阅可用性 (0x2A)
     * 值: 0 = 不支持共享订阅, 1 = 支持
     * 适用: CONNACK
     */
    SHARED_SUBSCRIPTION_AVAILABLE(0x2A, PropertyType.BYTE),
    
    // ==================== 双字节整数属性 ====================
    
    /**
     * 服务器保活时间 (0x13)
     * 值: 双字节整数，服务器指定的保活时间（秒）
     * 适用: CONNACK
     */
    SERVER_KEEP_ALIVE(0x13, PropertyType.TWO_BYTE_INTEGER),
    
    /**
     * 接收最大值 (0x21)
     * 值: 双字节整数，表示同时处理的 QoS > 0 消息的最大数量
     * 适用: CONNECT, CONNACK
     */
    RECEIVE_MAXIMUM(0x21, PropertyType.TWO_BYTE_INTEGER),
    
    /**
     * 主题别名最大值 (0x22)
     * 值: 双字节整数，表示允许的主题别名的最大数量
     * 适用: CONNECT, CONNACK
     */
    TOPIC_ALIAS_MAXIMUM(0x22, PropertyType.TWO_BYTE_INTEGER),
    
    /**
     * 主题别名 (0x23)
     * 值: 双字节整数，主题别名标识符
     * 适用: PUBLISH
     */
    TOPIC_ALIAS(0x23, PropertyType.TWO_BYTE_INTEGER),
    
    // ==================== 四字节整数属性 ====================
    
    /**
     * 消息过期时间间隔 (0x02)
     * 值: 四字节整数，消息过期前的秒数
     * 适用: PUBLISH, Will Properties
     */
    MESSAGE_EXPIRY_INTERVAL(0x02, PropertyType.FOUR_BYTE_INTEGER),
    
    /**
     * 会话过期时间间隔 (0x11)
     * 值: 四字节整数，会话在网络连接断开后保持的秒数
     * 适用: CONNECT, CONNACK, DISCONNECT
     */
    SESSION_EXPIRY_INTERVAL(0x11, PropertyType.FOUR_BYTE_INTEGER),
    
    /**
     * 遗嘱延迟时间间隔 (0x18)
     * 值: 四字节整数，发送遗嘱消息前的延迟秒数
     * 适用: Will Properties
     */
    WILL_DELAY_INTERVAL(0x18, PropertyType.FOUR_BYTE_INTEGER),
    
    /**
     * 最大报文大小 (0x27)
     * 值: 四字节整数，表示允许的最大报文大小
     * 适用: CONNECT, CONNACK
     */
    MAXIMUM_PACKET_SIZE(0x27, PropertyType.FOUR_BYTE_INTEGER),
    
    // ==================== UTF-8 字符串属性 ====================
    
    /**
     * 内容类型 (0x03)
     * 值: UTF-8 编码字符串，描述应用层消息内容
     * 适用: PUBLISH, Will Properties
     */
    CONTENT_TYPE(0x03, PropertyType.UTF_8_ENCODED_STRING),
    
    /**
     * 响应主题 (0x08)
     * 值: UTF-8 编码字符串，用于响应消息的主题名
     * 适用: PUBLISH, Will Properties
     */
    RESPONSE_TOPIC(0x08, PropertyType.UTF_8_ENCODED_STRING),
    
    /**
     * 分配的客户端标识符 (0x12)
     * 值: UTF-8 编码字符串，服务器分配的客户端标识符
     * 适用: CONNACK
     */
    ASSIGNED_CLIENT_IDENTIFIER(0x12, PropertyType.UTF_8_ENCODED_STRING),
    
    /**
     * 认证方法 (0x15)
     * 值: UTF-8 编码字符串，认证方法的名称
     * 适用: CONNECT, CONNACK, AUTH
     */
    AUTHENTICATION_METHOD(0x15, PropertyType.UTF_8_ENCODED_STRING),
    
    /**
     * 响应信息 (0x1A)
     * 值: UTF-8 编码字符串，CONNACK 中的响应信息
     * 适用: CONNACK
     */
    RESPONSE_INFORMATION(0x1A, PropertyType.UTF_8_ENCODED_STRING),
    
    /**
     * 服务器引用 (0x1C)
     * 值: UTF-8 编码字符串，用于服务器重定向
     * 适用: CONNACK, DISCONNECT
     */
    SERVER_REFERENCE(0x1C, PropertyType.UTF_8_ENCODED_STRING),
    
    /**
     * 原因字符串 (0x1F)
     * 值: UTF-8 编码字符串，人类可读的原因描述
     * 适用: CONNACK, PUBACK, PUBREC, PUBREL, PUBCOMP, SUBACK, UNSUBACK, DISCONNECT, AUTH
     */
    REASON_STRING(0x1F, PropertyType.UTF_8_ENCODED_STRING),
    
    // ==================== 二进制数据属性 ====================
    
    /**
     * 关联数据 (0x09)
     * 值: 二进制数据，用于请求和响应消息的关联
     * 适用: PUBLISH, Will Properties
     */
    CORRELATION_DATA(0x09, PropertyType.BINARY_DATA),
    
    /**
     * 认证数据 (0x16)
     * 值: 二进制数据，认证方法的特定数据
     * 适用: CONNECT, CONNACK, AUTH
     */
    AUTHENTICATION_DATA(0x16, PropertyType.BINARY_DATA),
    
    // ==================== 可重复属性 ====================
    
    /**
     * 用户属性 (0x26)
     * 值: UTF-8 字符串对 (key-value)，可以多次出现
     * 适用: CONNECT, CONNACK, PUBLISH, Will Properties, PUBACK, PUBREC, PUBREL, PUBCOMP, SUBSCRIBE, SUBACK, UNSUBSCRIBE, UNSUBACK, DISCONNECT, AUTH
     */
    USER_PROPERTY(0x26, PropertyType.UTF_8_STRING_PAIR, true),
    
    /**
     * 订阅标识符 (0x0B)
     * 值: 变长字节整数，订阅的唯一标识
     * 适用: PUBLISH, SUBSCRIBE
     */
    SUBSCRIPTION_IDENTIFIER(0x0B, PropertyType.VARIABLE_BYTE_INTEGER, true);
    
    // ==================== 枚举定义 ====================
    
    public enum PropertyType
    {
        BYTE(1),                    // 单字节
        TWO_BYTE_INTEGER(2),        // 双字节整数
        FOUR_BYTE_INTEGER(4),       // 四字节整数
        VARIABLE_BYTE_INTEGER(-1),  // 变长字节整数 (1-4字节)
        UTF_8_ENCODED_STRING(-2),   // UTF-8 字符串 (2字节长度 + 内容)
        UTF_8_STRING_PAIR(-3),      // UTF-8 字符串对 (2个UTF-8字符串)
        BINARY_DATA(-4);            // 二进制数据 (2字节长度 + 内容)
        
        private final int _FixedLength;
        
        PropertyType(int fixedLength) {
            _FixedLength = fixedLength;
        }
        
        /**
         * 获取固定长度，-1 表示变长
         */
        public int getFixedLength() {
            return _FixedLength;
        }
        
        /**
         * 是否为变长类型
         */
        public boolean isVariableLength() {
            return _FixedLength < 0;
        }
    }
    
    private final int           _Id;
    private final PropertyType  _Type;
    private final boolean       _Repeatable;
    
    private static final Map<Integer, QttProperty> _IdToProperty = Arrays.stream(values())
                                                                          .collect(Collectors.toMap(QttProperty::getId, Function.identity()));
    
    QttProperty(int id, PropertyType type) {
        this(id, type, false);
    }
    
    QttProperty(int id, PropertyType type, boolean repeatable) {
        _Id = id;
        _Type = type;
        _Repeatable = repeatable;
    }
    
    /**
     * 获取属性标识符
     */
    public int getId() {
        return _Id;
    }
    
    /**
     * 获取属性数据类型
     */
    public PropertyType getType() {
        return _Type;
    }
    
    /**
     * 是否可重复出现
     */
    public boolean isRepeatable() {
        return _Repeatable;
    }
    
    /**
     * 根据标识符查找属性
     * 
     * @param id 属性标识符
     * @return 属性枚举，未找到返回 null
     */
    public static QttProperty valueOf(int id) {
        return _IdToProperty.get(id);
    }
    
    /**
     * 检查属性标识符是否有效
     */
    public static boolean isValid(int id) {
        return _IdToProperty.containsKey(id);
    }
    
    @Override
    public String toString() {
        return String.format("%s(0x%02X, %s%s)", 
            name(), _Id, _Type, _Repeatable ? ", repeatable" : "");
    }
}
