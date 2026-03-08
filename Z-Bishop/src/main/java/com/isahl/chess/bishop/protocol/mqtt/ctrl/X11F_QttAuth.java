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

import com.isahl.chess.bishop.protocol.mqtt.model.QttPropertySet;
import com.isahl.chess.bishop.protocol.mqtt.model.QttType;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;

import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE;

/**
 * MQTT AUTH 报文（MQTT v5.0）
 * <p>
 * AUTH 报文仅用于 MQTT v5.0，用于增强认证流程（Enhanced Authentication）和重新认证（Re-authentication）。
 * <p>
 * 原因码（Reason Code）：
 * <ul>
 *   <li>0x00 - 成功（Success）</li>
 *   <li>0x18 - 继续认证（Continue Authentication）</li>
 *   <li>0x19 - 重新认证（Re-authenticate）</li>
 * </ul>
 *
 * @author william.d.zk
 * @date 2019-05-30
 * @updated 2024 - 完整实现 MQTT v5.0 AUTH
 * @see <a href="https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html#_Toc3901217">MQTT v5.0 AUTH</a>
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x11F)
public class X11F_QttAuth
        extends QttControl
{
    /**
     * 原因码：成功
     */
    public static final byte REASON_SUCCESS = 0x00;

    /**
     * 原因码：继续认证
     */
    public static final byte REASON_CONTINUE_AUTHENTICATION = 0x18;

    /**
     * 原因码：重新认证
     */
    public static final byte REASON_REAUTHENTICATE = 0x19;

    /**
     * MQTT v5.0 属性集合
     */
    private QttPropertySet _Properties;

    /**
     * 原因码
     */
    private int _ReasonCode = REASON_SUCCESS;

    // 常用属性缓存
    private String _AuthMethod;
    private byte[] _AuthData;
    private String _ReasonString;

    public X11F_QttAuth()
    {
        generateCtrl(false, false, ALMOST_ONCE, QttType.AUTH);
    }

    @Override
    public int length()
    {
        // 原因码 1 字节 + 属性
        int length = 1;

        if (_Properties != null && !_Properties.isEmpty()) {
            int propsLength = _Properties.length(VERSION_V5_0);
            length += ByteBuf.vSizeOf(propsLength) + propsLength;
        }
        else {
            length += 1; // 零长度属性
        }

        return length;
    }

    // ==================== 原因码 ====================

    public int getReasonCode()
    {
        return _ReasonCode;
    }

    public void setReasonCode(int reasonCode)
    {
        _ReasonCode = reasonCode;
    }

    public boolean isSuccess()
    {
        return _ReasonCode == REASON_SUCCESS;
    }

    public boolean isContinueAuthentication()
    {
        return _ReasonCode == REASON_CONTINUE_AUTHENTICATION;
    }

    public boolean isReauthenticate()
    {
        return _ReasonCode == REASON_REAUTHENTICATE;
    }

    /**
     * 设置成功状态
     */
    public void setSuccess()
    {
        _ReasonCode = REASON_SUCCESS;
    }

    /**
     * 设置继续认证状态
     */
    public void setContinueAuthentication()
    {
        _ReasonCode = REASON_CONTINUE_AUTHENTICATION;
    }

    /**
     * 设置重新认证状态
     */
    public void setReauthenticate()
    {
        _ReasonCode = REASON_REAUTHENTICATE;
    }

    // ==================== 属性访问 ====================

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
        if (properties != null) {
            _AuthMethod = properties.getAuthenticationMethod();
            _AuthData = properties.getAuthenticationData();
            _ReasonString = properties.getReasonString();
        }
    }

    // ----- 认证方法 -----

    public String getAuthMethod()
    {
        if (_AuthMethod == null && _Properties != null) {
            _AuthMethod = _Properties.getAuthenticationMethod();
        }
        return _AuthMethod;
    }

    public void setAuthMethod(String method)
    {
        _AuthMethod = method;
        getProperties().setAuthenticationMethod(method);
    }

    // ----- 认证数据 -----

    public byte[] getAuthData()
    {
        if (_AuthData == null && _Properties != null) {
            _AuthData = _Properties.getAuthenticationData();
        }
        return _AuthData;
    }

    public void setAuthData(byte[] data)
    {
        _AuthData = data;
        getProperties().setAuthenticationData(data);
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

    // ----- 用户属性 -----

    public void addUserProperty(String key, String value)
    {
        getProperties().addUserProperty(key, value);
    }

    // ==================== 编解码 ====================

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output.put(_ReasonCode);

        // 属性（AUTH 仅在 v5 中使用，直接使用 VERSION_V5_0）
        if (_Properties != null && !_Properties.isEmpty()) {
            _Properties.encode(output, VERSION_V5_0);
        }
        else {
            output.put(0);
        }

        return output;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        _ReasonCode = input.getUnsigned();

        // 属性解析（AUTH 仅在 v5 中使用）
        _Properties = new QttPropertySet();
        _Properties.decode(input, VERSION_V5_0);

        _AuthMethod = _Properties.getAuthenticationMethod();
        _AuthData = _Properties.getAuthenticationData();
        _ReasonString = _Properties.getReasonString();

        return input.readableBytes();
    }

    @Override
    public String toString()
    {
        return String.format("X11F auth:[reason=0x%02X(%s), method=%s, data=%s]",
            _ReasonCode,
            getReasonCodeName(_ReasonCode),
            getAuthMethod(),
            _AuthData != null ? _AuthData.length + "bytes" : "null"
        );
    }

    private String getReasonCodeName(int code)
    {
        return switch (code) {
            case REASON_SUCCESS -> "Success";
            case REASON_CONTINUE_AUTHENTICATION -> "Continue Authentication";
            case REASON_REAUTHENTICATE -> "Re-authenticate";
            default -> "Unknown";
        };
    }
}
