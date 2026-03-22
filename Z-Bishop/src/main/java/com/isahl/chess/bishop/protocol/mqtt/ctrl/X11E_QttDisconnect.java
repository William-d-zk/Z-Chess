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

import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V5_0;
import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE;

import com.isahl.chess.bishop.protocol.mqtt.model.QttPropertySet;
import com.isahl.chess.bishop.protocol.mqtt.model.QttType;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;

/**
 * MQTT DISCONNECT 报文
 *
 * <p>支持 MQTT v3.1.1 和 v5.0 协议。 MQTT v5.0 增加了原因码和属性字段，支持服务器重定向等场景。
 *
 * @author william.d.zk
 * @date 2019-05-30
 * @updated 2024 - 增加 MQTT v5.0 支持
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_CONTROL_SERIAL, serial = 0x11E)
public class X11E_QttDisconnect extends QttControl {

  /** MQTT v5.0 原因码 */
  private int _ReasonCode = 0;

  /** MQTT v5.0 属性集合 */
  private QttPropertySet _Properties;

  // 常用属性缓存
  private String _ServerReference;
  private String _ReasonString;

  public X11E_QttDisconnect() {
    generateCtrl(false, false, ALMOST_ONCE, QttType.DISCONNECT);
  }

  @Override
  public int length() {
    // v3.1.1: 无剩余长度
    // v5.0: 原因码(1) + 属性(变长)
    if (!isV5()) {
      return 0;
    }

    int length = 1; // 原因码

    if (_Properties != null && !_Properties.isEmpty()) {
      int propsLength = _Properties.length(VERSION_V5_0);
      length += ByteBuf.vSizeOf(propsLength) + propsLength;
    } else {
      length += 1; // 零长度属性
    }

    return length;
  }

  /** 检查是否为 MQTT v5.0 */
  private boolean isV5() {
    return mContext != null && mContext.getVersion() == VERSION_V5_0;
  }

  // ==================== 原因码 ====================

  /** 获取原因码 */
  public int getCode() {
    return _ReasonCode;
  }

  /** 设置原因码 */
  public void setCode(int reasonCode) {
    this._ReasonCode = reasonCode;
  }

  /** 设置原因码（byte 类型） */
  public void setCode(byte reasonCode) {
    this._ReasonCode = reasonCode & 0xFF;
  }

  // ==================== 属性访问 ====================

  /** 获取属性集合 */
  public QttPropertySet getProperties() {
    if (_Properties == null) {
      _Properties = new QttPropertySet();
    }
    return _Properties;
  }

  /** 设置属性集合 */
  public void setProperties(QttPropertySet properties) {
    this._Properties = properties;
    if (properties != null) {
      _ServerReference = properties.getServerReference();
      _ReasonString = properties.getReasonString();
    }
  }

  // ----- 服务器引用（重定向） -----

  /** 获取服务器引用 */
  public String getServerReference() {
    if (_ServerReference == null && _Properties != null) {
      _ServerReference = _Properties.getServerReference();
    }
    return _ServerReference;
  }

  /** 设置服务器引用 */
  public void setServerReference(String serverReference) {
    this._ServerReference = serverReference;
    getProperties().setServerReference(serverReference);
  }

  // ----- 原因字符串 -----

  /** 获取原因字符串 */
  public String getReasonString() {
    if (_ReasonString == null && _Properties != null) {
      _ReasonString = _Properties.getReasonString();
    }
    return _ReasonString;
  }

  /** 设置原因字符串 */
  public void setReasonString(String reasonString) {
    this._ReasonString = reasonString;
    getProperties().setReasonString(reasonString);
  }

  // ==================== 快捷方法 ====================

  /** 是否为服务器重定向 DISCONNECT */
  public boolean isRedirect() {
    return getServerReference() != null && !getServerReference().isEmpty();
  }

  /** 设置正常关闭 */
  public void setNormalDisconnection() {
    _ReasonCode = 0x00; // Normal disconnection
  }

  /** 设置会话被接管 */
  public void setSessionTakenOver() {
    _ReasonCode = 0x8E; // Session taken over
  }

  /** 设置保活超时 */
  public void setKeepAliveTimeout() {
    _ReasonCode = 0x8D; // Keep Alive timeout
  }

  /** 设置服务器关闭 */
  public void setServerShuttingDown() {
    _ReasonCode = 0x8B; // Server shutting down
  }

  // ==================== 编解码 ====================

  @Override
  public ByteBuf suffix(ByteBuf output) {
    if (!isV5()) {
      return output;
    }

    // 原因码
    output.put(_ReasonCode);

    // 属性
    if (_Properties != null && !_Properties.isEmpty()) {
      _Properties.encode(output, VERSION_V5_0);
    } else {
      output.put(0); // 零长度属性
    }

    return output;
  }

  @Override
  public int prefix(ByteBuf input) {
    if (!isV5()) {
      return input.readableBytes();
    }

    // 原因码
    _ReasonCode = input.getUnsigned();

    // 属性解析
    _Properties = new QttPropertySet();
    _Properties.decode(input, VERSION_V5_0);

    // 更新缓存
    _ServerReference = _Properties.getServerReference();
    _ReasonString = _Properties.getReasonString();

    return input.readableBytes();
  }

  @Override
  public String toString() {
    if (isV5()) {
      return String.format(
          "X11E disconnect:[code=0x%02X, ref=%s, reason=%s]",
          _ReasonCode, getServerReference(), getReasonString());
    }
    return "X11E disconnect[v3.1.1]";
  }
}
