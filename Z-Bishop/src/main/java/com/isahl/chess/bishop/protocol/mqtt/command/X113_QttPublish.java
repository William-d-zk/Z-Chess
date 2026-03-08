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

package com.isahl.chess.bishop.protocol.mqtt.command;

import com.isahl.chess.bishop.protocol.mqtt.model.QttPropertySet;
import com.isahl.chess.bishop.protocol.mqtt.model.QttTopicAlias;
import com.isahl.chess.bishop.protocol.mqtt.model.QttType;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.routes.IRoutable;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V5_0;
import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE;

/**
 * MQTT PUBLISH 报文
 * <p>
 * 支持 MQTT v3.1.1 和 v5.0 协议。
 * MQTT v5.0 增加了属性字段，支持主题别名、消息过期时间、内容类型等特性。
 * </p>
 *
 * @author william.d.zk
 * @date 2019-05-30
 * @updated 2024 - 增加 MQTT v5.0 属性支持
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x113)
public class X113_QttPublish
        extends QttCommand
        implements IRoutable
{

    public X113_QttPublish()
    {
        generateCtrl(false, false, ALMOST_ONCE, QttType.PUBLISH);
    }

    /**
     * MQTT v5.0 属性集合
     */
    private QttPropertySet _Properties;

    // 常用 v5 属性缓存
    private Integer _TopicAlias;
    private Long    _MessageExpiryInterval;
    private String  _ContentType;
    private String  _ResponseTopic;
    private byte[]  _CorrelationData;
    private Integer _PayloadFormatIndicator;

    private String mTopic;
    private long   mTarget;

    @Override
    public int length()
    {
        int length = 0;

        // 主题名（可能被主题别名替代）
        if (mTopic != null) {
            length += 2 + mTopic.getBytes(StandardCharsets.UTF_8).length;
        }
        else if (!isV5()) {
            // v3.1.1 必须包含主题名
            length += 2;
        }

        // 消息标识符（QoS > 0 时需要）
        if (level().getValue() > ALMOST_ONCE.getValue()) {
            length += 2;
        }

        // v5 属性
        if (isV5()) {
            if (_Properties != null && !_Properties.isEmpty()) {
                int propsLength = _Properties.length(VERSION_V5_0);
                length += ByteBuf.vSizeOf(propsLength) + propsLength;
            }
            else {
                length += 1; // 零长度属性
            }
        }

        // 载荷
        if (mPayload != null) {
            length += mPayload.length;
        }

        return length;
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_07_ROUTE_MESSAGE;
    }

    public X113_QttPublish withTopic(String topic)
    {
        mTopic = Objects.requireNonNull(topic);
        return this;
    }

    @Override
    public String topic()
    {
        return mTopic;
    }

    @Override
    public long target()
    {
        return mTarget;
    }

    public void target(long target)
    {
        mTarget = target;
    }

    // ==================== MQTT v5.0 属性访问方法 ====================

    /**
     * 检查是否为 MQTT v5.0
     */
    public boolean isV5()
    {
        return mContext != null && mContext.getVersion() == VERSION_V5_0;
    }

    /**
     * 获取属性集合
     */
    public QttPropertySet getProperties()
    {
        if (_Properties == null) {
            _Properties = new QttPropertySet();
        }
        return _Properties;
    }

    /**
     * 设置属性集合
     */
    public void setProperties(QttPropertySet properties)
    {
        _Properties = properties;
        // 更新缓存
        if (properties != null) {
            _TopicAlias = properties.getTopicAlias();
            _MessageExpiryInterval = properties.getMessageExpiryInterval();
            _ContentType = properties.getContentType();
            _ResponseTopic = properties.getResponseTopic();
            _CorrelationData = properties.getCorrelationData();
            _PayloadFormatIndicator = properties.getProperty(com.isahl.chess.bishop.protocol.mqtt.model.QttProperty.PAYLOAD_FORMAT_INDICATOR);
        }
    }

    // ----- 主题别名 -----

    public Integer getTopicAlias()
    {
        if (_TopicAlias == null && _Properties != null) {
            _TopicAlias = _Properties.getTopicAlias();
        }
        return _TopicAlias;
    }

    public void setTopicAlias(Integer alias)
    {
        _TopicAlias = alias;
        getProperties().setTopicAlias(alias);
    }

    /**
     * 使用主题别名（主题名将被清空）
     */
    public void useTopicAlias(int alias)
    {
        setTopicAlias(alias);
        mTopic = null; // 使用别名时不需要主题名
    }

    // ----- 消息过期时间 -----

    public Long getMessageExpiryInterval()
    {
        if (_MessageExpiryInterval == null && _Properties != null) {
            _MessageExpiryInterval = _Properties.getMessageExpiryInterval();
        }
        return _MessageExpiryInterval;
    }

    public void setMessageExpiryInterval(long seconds)
    {
        _MessageExpiryInterval = seconds;
        getProperties().setMessageExpiryInterval(seconds);
    }

    /**
     * 检查消息是否已过期
     */
    public boolean isExpired()
    {
        Long expiry = getMessageExpiryInterval();
        if (expiry == null || expiry <= 0) {
            return false;
        }
        // 注意：实际过期检查需要知道消息发布时间
        // 这里简化处理，假设 0 表示已过期
        return expiry <= System.currentTimeMillis();
    }

    // ----- 内容类型 -----

    public String getContentType()
    {
        if (_ContentType == null && _Properties != null) {
            _ContentType = _Properties.getContentType();
        }
        return _ContentType;
    }

    public void setContentType(String contentType)
    {
        _ContentType = contentType;
        if(isV5()) getProperties().setContentType(contentType);
    }

    // ----- 响应主题 -----

    public String getResponseTopic()
    {
        if (_ResponseTopic == null && _Properties != null) {
            _ResponseTopic = _Properties.getResponseTopic();
        }
        return _ResponseTopic;
    }

    public void setResponseTopic(String topic)
    {
        _ResponseTopic = topic;
        getProperties().setResponseTopic(topic);
    }

    // ----- 关联数据 -----

    public byte[] getCorrelationData()
    {
        if (_CorrelationData == null && _Properties != null) {
            _CorrelationData = _Properties.getCorrelationData();
        }
        return _CorrelationData;
    }

    public void setCorrelationData(byte[] data)
    {
        _CorrelationData = data;
        getProperties().setCorrelationData(data);
    }

    // ----- 载荷格式指示器 -----

    public Integer getPayloadFormatIndicator()
    {
        if (_PayloadFormatIndicator == null && _Properties != null) {
            _PayloadFormatIndicator = _Properties.getProperty(com.isahl.chess.bishop.protocol.mqtt.model.QttProperty.PAYLOAD_FORMAT_INDICATOR);
        }
        return _PayloadFormatIndicator;
    }

    public void setPayloadFormatIndicator(int indicator)
    {
        _PayloadFormatIndicator = indicator;
        getProperties().setProperty(com.isahl.chess.bishop.protocol.mqtt.model.QttProperty.PAYLOAD_FORMAT_INDICATOR, indicator);
    }

    /**
     * 检查载荷是否为 UTF-8 字符串
     */
    public boolean isPayloadUtf8()
    {
        Integer indicator = getPayloadFormatIndicator();
        return indicator != null && indicator == 1;
    }

    // ----- 用户属性 -----

    public void addUserProperty(String key, String value)
    {
        getProperties().addUserProperty(key, value);
    }

    // ==================== 编解码方法 ====================

    @Override
    public int prefix(ByteBuf input)
    {
        // v5 首先解析属性（如果有的话）
        if (isV5()) {
            _Properties = new QttPropertySet();
            _Properties.decode(input, VERSION_V5_0);

            // 更新缓存
            _TopicAlias = _Properties.getTopicAlias();
            _MessageExpiryInterval = _Properties.getMessageExpiryInterval();
            _ContentType = _Properties.getContentType();
            _ResponseTopic = _Properties.getResponseTopic();
            _CorrelationData = _Properties.getCorrelationData();
            _PayloadFormatIndicator = _Properties.getProperty(com.isahl.chess.bishop.protocol.mqtt.model.QttProperty.PAYLOAD_FORMAT_INDICATOR);
        }

        // 解析主题名（v5 可能被主题别名替代）
        int topicLength = input.getUnsignedShort();
        if (topicLength > 0) {
            mTopic = input.readUTF(topicLength);
        }
        else {
            mTopic = null; // v5 可能使用主题别名
        }

        // 消息标识符（QoS > 0）
        if (level().getValue() > ALMOST_ONCE.getValue()) {
            msgId(input.getUnsignedShort());
        }

        return input.readableBytes();
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        // v5 属性编码（在主题之前）
        if (isV5()) {
            if (_Properties != null && !_Properties.isEmpty()) {
                _Properties.encode(output, VERSION_V5_0);
            }
            else {
                output.put(0); // 零长度属性
            }
        }

        // 主题名（如果存在）
        if (mTopic != null) {
            byte[] topicBytes = mTopic.getBytes(StandardCharsets.UTF_8);
            output.putShort(topicBytes.length);
            output.put(topicBytes);
        }
        else {
            output.putShort(0); // v5 使用主题别名时主题名为空
        }

        // 消息标识符（QoS > 0）
        if (level().getValue() > ALMOST_ONCE.getValue()) {
            output.putShort(msgId());
        }

        // 载荷
        if (mPayload != null) {
            output.put(mPayload);
        }

        return output;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("X113 publish | dup:%s,retain:%s,qos:%s | msg-id:%d",
                isDuplicate(), isRetain(), level(), msgId()));

        if (isV5()) {
            sb.append(String.format(" | alias:%s,expiry:%s",
                getTopicAlias(), getMessageExpiryInterval()));
        }

        sb.append(String.format(" | topic:\"%s\" \npayload: \"%s\" \n",
            topic(),
            mPayload == null ? "NULL" : new String(mPayload, StandardCharsets.UTF_8)));

        return sb.toString();
    }

    @Override
    public X113_QttPublish duplicate()
    {
        X113_QttPublish n113 = new X113_QttPublish();
        n113.withTopic(topic());
        n113.setLevel(level());
        n113.mPayload = new byte[mPayload.length];
        IoUtil.addArray(mPayload, n113.mPayload);

        // 复制 v5 属性
        if (isV5() && _Properties != null) {
            // 创建新的属性集合，复制关键属性
            QttPropertySet newProps = new QttPropertySet();
            if (getMessageExpiryInterval() != null) {
                newProps.setMessageExpiryInterval(getMessageExpiryInterval());
            }
            if (getContentType() != null) {
                newProps.setContentType(getContentType());
            }
            if (getPayloadFormatIndicator() != null) {
                newProps.setProperty(com.isahl.chess.bishop.protocol.mqtt.model.QttProperty.PAYLOAD_FORMAT_INDICATOR, getPayloadFormatIndicator());
            }
            // 不复制主题别名（每个连接独立）
            n113.setProperties(newProps);
        }

        return n113;
    }
}
