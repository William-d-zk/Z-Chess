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

package com.isahl.chess.bishop.protocol.mqtt.ctrl;

import com.isahl.chess.bishop.protocol.mqtt.model.CodeMqtt;
import com.isahl.chess.bishop.protocol.mqtt.model.QttPropertySet;
import com.isahl.chess.bishop.protocol.mqtt.model.QttType;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.features.ICode;

import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V5_0;
import static com.isahl.chess.king.config.KingCode.SUCCESS;
import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE;

/**
 * MQTT CONNACK 报文
 * <p>
 * 支持 MQTT v3.1.1 和 v5.0 协议。
 * MQTT v5.0 在原因码之后增加了属性字段。
 * </p>
 *
 * @author william.d.zk
 * @date 2019-05-11
 * @updated 2024 - 增加 MQTT v5.0 属性支持
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_CONTROL_SERIAL,
                  serial = 0x112)
public class X112_QttConnack
        extends QttControl
{

    private int mPresent;
    private int mResponseCode;

    /**
     * MQTT v5.0 属性集合
     */
    private QttPropertySet _Properties;

    // 常用 v5 属性缓存
    private Integer _ReceiveMaximum;
    private Integer _MaximumPacketSize;
    private Integer _TopicAliasMaximum;
    private Long    _SessionExpiryInterval;
    private Integer _ServerKeepAlive;
    private String  _AssignedClientIdentifier;
    private String  _ResponseInformation;
    private String  _ServerReference;
    private String  _ReasonString;
    private String  _AuthenticationMethod;
    private byte[]  _AuthenticationData;
    private Integer _MaximumQoS;
    private Boolean _RetainAvailable;
    private Boolean _WildcardSubscriptionAvailable;
    private Boolean _SubscriptionIdentifierAvailable;
    private Boolean _SharedSubscriptionAvailable;

    public X112_QttConnack()
    {
        generateCtrl(false, false, ALMOST_ONCE, QttType.CONNACK);
    }

    @Override
    public int length()
    {
        // v3.1.1: 会话存在标志(1) + 返回码(1) = 2
        // v5.0: 会话存在标志(1) + 原因码(1) + 属性(变长)
        if (!isV5()) {
            return 2;
        }

        int length = 2; // 会话存在标志 + 原因码
        if (_Properties != null && !_Properties.isEmpty()) {
            int propsLength = _Properties.length(VERSION_V5_0);
            length += ByteBuf.vSizeOf(propsLength) + propsLength;
        }
        else {
            length += 1; // 零长度属性
        }
        return length;
    }

    public boolean isPresent()
    {
        return mPresent > 0;
    }

    public void setPresent()
    {
        mPresent = 1;
    }

    public CodeMqtt getCode()
    {
        int version = (mContext != null) ? mContext.getVersion() : 4; // 默认 v3.1.1
        return CodeMqtt.valueOf(mResponseCode, version);
    }

    private void setCode(ICode code)
    {
        int version = (mContext != null) ? mContext.getVersion() : 4;
        mResponseCode = code.getCode(version);
    }

    public void responseOk()
    {
        mPresent = 0;
        setCode(CodeMqtt.OK);
    }

    public void responseClean()
    {
        mPresent = 0;
        setCode(CodeMqtt.OK);
    }

    public void rejectUnsupportedVersion()
    {
        mPresent = 0;
        setCode(CodeMqtt.REJECT_UNSUPPORTED_VERSION_PROTOCOL);
    }

    public void rejectIdentifier()
    {
        mPresent = 0;
        setCode(CodeMqtt.REJECT_IDENTIFIER);
    }

    public void rejectServerUnavailable()
    {
        mPresent = 0;
        setCode(CodeMqtt.REJECT_SERVER_UNAVAILABLE);
    }

    public void rejectBadUserOrPassword()
    {
        mPresent = 0;
        setCode(CodeMqtt.REJECT_BAD_USER_OR_PASSWORD);
    }

    public void rejectNotAuthorized()
    {
        mPresent = 0;
        setCode(CodeMqtt.REJECT_NOT_AUTHORIZED);
    }

    // ==================== MQTT v5.0 增强拒绝方法 ====================

    /**
     * 拒绝：错误的认证方法 (v5)
     */
    public void rejectBadAuthenticationMethod()
    {
        mPresent = 0;
        setCode(CodeMqtt.REJECT_BAD_AUTHENTICATION_METHOD);
    }

    /**
     * 拒绝：服务器繁忙 (v5)
     */
    public void rejectServerBusy()
    {
        mPresent = 0;
        setCode(CodeMqtt.REJECT_SERVER_BUSY);
    }

    /**
     * 拒绝：已禁止 (v5)
     */
    public void rejectBanned()
    {
        mPresent = 0;
        setCode(CodeMqtt.REJECT_BANNED);
    }

    /**
     * 使用原因字符串拒绝 (v5)
     */
    public void rejectWithReason(CodeMqtt code, String reason)
    {
        mPresent = 0;
        setCode(code);
        if (isV5() && reason != null) {
            getProperties().setReasonString(reason);
        }
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
    }

    // ----- 接收最大值 -----

    public int getReceiveMaximum()
    {
        if (_ReceiveMaximum == null && _Properties != null) {
            _ReceiveMaximum = _Properties.getReceiveMaximum();
        }
        return _ReceiveMaximum != null ? _ReceiveMaximum : 65535;
    }

    public void setReceiveMaximum(int value)
    {
        _ReceiveMaximum = value;
        getProperties().setReceiveMaximum(value);
    }

    // ----- 最大报文大小 -----

    public int getMaximumPacketSize()
    {
        if (_MaximumPacketSize == null && _Properties != null) {
            _MaximumPacketSize = _Properties.getMaximumPacketSize();
        }
        return _MaximumPacketSize != null ? _MaximumPacketSize : 0;
    }

    public void setMaximumPacketSize(int size)
    {
        _MaximumPacketSize = size;
        getProperties().setMaximumPacketSize(size);
    }

    // ----- 主题别名最大值 -----

    public int getTopicAliasMaximum()
    {
        if (_TopicAliasMaximum == null && _Properties != null) {
            _TopicAliasMaximum = _Properties.getTopicAliasMaximum();
        }
        return _TopicAliasMaximum != null ? _TopicAliasMaximum : 0;
    }

    public void setTopicAliasMaximum(int value)
    {
        _TopicAliasMaximum = value;
        getProperties().setTopicAliasMaximum(value);
    }

    // ----- 会话过期时间 -----

    public long getSessionExpiryInterval()
    {
        if (_SessionExpiryInterval == null && _Properties != null) {
            _SessionExpiryInterval = _Properties.getSessionExpiryInterval();
        }
        return _SessionExpiryInterval != null ? _SessionExpiryInterval : 0;
    }

    public void setSessionExpiryInterval(long seconds)
    {
        _SessionExpiryInterval = seconds;
        getProperties().setSessionExpiryInterval(seconds);
    }

    // ----- 服务器保活时间 -----

    public int getServerKeepAlive()
    {
        if (_ServerKeepAlive == null && _Properties != null) {
            _ServerKeepAlive = _Properties.getProperty(com.isahl.chess.bishop.protocol.mqtt.model.QttProperty.SERVER_KEEP_ALIVE);
        }
        return _ServerKeepAlive != null ? _ServerKeepAlive : 0;
    }

    public void setServerKeepAlive(int seconds)
    {
        _ServerKeepAlive = seconds;
        getProperties().setProperty(com.isahl.chess.bishop.protocol.mqtt.model.QttProperty.SERVER_KEEP_ALIVE, seconds);
    }

    // ----- 分配的客户端标识符 -----

    public String getAssignedClientIdentifier()
    {
        if (_AssignedClientIdentifier == null && _Properties != null) {
            _AssignedClientIdentifier = _Properties.getAssignedClientIdentifier();
        }
        return _AssignedClientIdentifier;
    }

    public void setAssignedClientIdentifier(String clientId)
    {
        _AssignedClientIdentifier = clientId;
        getProperties().setAssignedClientIdentifier(clientId);
    }

    // ----- 响应信息 -----

    public String getResponseInformation()
    {
        if (_ResponseInformation == null && _Properties != null) {
            _ResponseInformation = _Properties.getResponseInformation();
        }
        return _ResponseInformation;
    }

    public void setResponseInformation(String info)
    {
        _ResponseInformation = info;
        getProperties().setResponseInformation(info);
    }

    // ----- 服务器引用（重定向） -----

    public String getServerReference()
    {
        if (_ServerReference == null && _Properties != null) {
            _ServerReference = _Properties.getServerReference();
        }
        return _ServerReference;
    }

    public void setServerReference(String reference)
    {
        _ServerReference = reference;
        getProperties().setServerReference(reference);
    }

    // ----- 最大 QoS -----

    public int getMaximumQoS()
    {
        if (_MaximumQoS == null && _Properties != null) {
            _MaximumQoS = _Properties.getMaximumQoS();
        }
        return _MaximumQoS != null ? _MaximumQoS : 2; // 默认支持 QoS 2
    }

    public void setMaximumQoS(int qos)
    {
        _MaximumQoS = qos;
        getProperties().setMaximumQoS(qos);
    }

    // ----- 保留消息可用性 -----

    public boolean isRetainAvailable()
    {
        if (_RetainAvailable == null && _Properties != null) {
            _RetainAvailable = _Properties.isRetainAvailable();
        }
        return _RetainAvailable != null ? _RetainAvailable : true;
    }

    public void setRetainAvailable(boolean available)
    {
        _RetainAvailable = available;
        getProperties().setRetainAvailable(available);
    }

    // ----- 通配符订阅可用性 -----

    public boolean isWildcardSubscriptionAvailable()
    {
        if (_WildcardSubscriptionAvailable == null && _Properties != null) {
            _WildcardSubscriptionAvailable = _Properties.isWildcardSubscriptionAvailable();
        }
        return _WildcardSubscriptionAvailable != null ? _WildcardSubscriptionAvailable : true;
    }

    public void setWildcardSubscriptionAvailable(boolean available)
    {
        _WildcardSubscriptionAvailable = available;
        getProperties().setWildcardSubscriptionAvailable(available);
    }

    // ----- 订阅标识符可用性 -----

    public boolean isSubscriptionIdentifierAvailable()
    {
        if (_SubscriptionIdentifierAvailable == null && _Properties != null) {
            _SubscriptionIdentifierAvailable = _Properties.isSubscriptionIdentifierAvailable();
        }
        return _SubscriptionIdentifierAvailable != null ? _SubscriptionIdentifierAvailable : true;
    }

    public void setSubscriptionIdentifierAvailable(boolean available)
    {
        _SubscriptionIdentifierAvailable = available;
        getProperties().setSubscriptionIdentifierAvailable(available);
    }

    // ----- 共享订阅可用性 -----

    public boolean isSharedSubscriptionAvailable()
    {
        if (_SharedSubscriptionAvailable == null && _Properties != null) {
            _SharedSubscriptionAvailable = _Properties.isSharedSubscriptionAvailable();
        }
        return _SharedSubscriptionAvailable != null ? _SharedSubscriptionAvailable : true;
    }

    public void setSharedSubscriptionAvailable(boolean available)
    {
        _SharedSubscriptionAvailable = available;
        getProperties().setSharedSubscriptionAvailable(available);
    }

    // ----- 原因字符串 -----

    public String getReasonString()
    {
        if (_ReasonString == null && _Properties != null) {
            _ReasonString = _Properties.getReasonString();
        }
        return _ReasonString;
    }

    public void setReasonString(String reason)
    {
        _ReasonString = reason;
        getProperties().setReasonString(reason);
    }

    // ----- 认证方法 -----

    public String getAuthenticationMethod()
    {
        if (_AuthenticationMethod == null && _Properties != null) {
            _AuthenticationMethod = _Properties.getAuthenticationMethod();
        }
        return _AuthenticationMethod;
    }

    public void setAuthenticationMethod(String method)
    {
        _AuthenticationMethod = method;
        getProperties().setAuthenticationMethod(method);
    }

    // ----- 认证数据 -----

    public byte[] getAuthenticationData()
    {
        if (_AuthenticationData == null && _Properties != null) {
            _AuthenticationData = _Properties.getAuthenticationData();
        }
        return _AuthenticationData;
    }

    public void setAuthenticationData(byte[] data)
    {
        _AuthenticationData = data;
        getProperties().setAuthenticationData(data);
    }

    // ==================== 编解码方法 ====================

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output.put(mPresent)
              .put(mResponseCode);

        // v5 属性编码
        if (isV5()) {
            if (_Properties != null && !_Properties.isEmpty()) {
                _Properties.encode(output, VERSION_V5_0);
            }
            else {
                output.put(0); // 零长度属性
            }
        }

        return output;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        mPresent = input.get() & 0x01;
        mResponseCode = input.getUnsigned();

        // v5 属性解析
        if (isV5()) {
            _Properties = new QttPropertySet();
            _Properties.decode(input, VERSION_V5_0);

            // 更新缓存
            _ReceiveMaximum = _Properties.getReceiveMaximum();
            _MaximumPacketSize = _Properties.getMaximumPacketSize();
            _TopicAliasMaximum = _Properties.getTopicAliasMaximum();
            _SessionExpiryInterval = _Properties.getSessionExpiryInterval();
            _ServerKeepAlive = _Properties.getProperty(com.isahl.chess.bishop.protocol.mqtt.model.QttProperty.SERVER_KEEP_ALIVE);
            _AssignedClientIdentifier = _Properties.getAssignedClientIdentifier();
            _ResponseInformation = _Properties.getResponseInformation();
            _ServerReference = _Properties.getServerReference();
            _ReasonString = _Properties.getReasonString();
            _AuthenticationMethod = _Properties.getAuthenticationMethod();
            _AuthenticationData = _Properties.getAuthenticationData();
            _MaximumQoS = _Properties.getMaximumQoS();
            _RetainAvailable = _Properties.isRetainAvailable();
            _WildcardSubscriptionAvailable = _Properties.isWildcardSubscriptionAvailable();
            _SubscriptionIdentifierAvailable = _Properties.isSubscriptionIdentifierAvailable();
            _SharedSubscriptionAvailable = _Properties.isSharedSubscriptionAvailable();
        }

        return input.readableBytes();
    }

    public boolean isReject()
    {
        return mResponseCode > SUCCESS;
    }

    public boolean isOk()
    {
        return mResponseCode == SUCCESS;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("X112 connack:[present:%s, code:%s", mPresent, getCode()));
        if (isV5()) {
            sb.append(String.format(", v5:[recvMax=%d, topicAliasMax=%d, maxQoS=%d, retain=%s, wildcard=%s, shared=%s]",
                getReceiveMaximum(), getTopicAliasMaximum(), getMaximumQoS(),
                isRetainAvailable(), isWildcardSubscriptionAvailable(), isSharedSubscriptionAvailable()));
        }
        sb.append("]");
        return sb.toString();
    }
}
