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
import static com.isahl.chess.king.base.util.IoUtil.isBlank;
import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE;

import com.isahl.chess.bishop.protocol.mqtt.model.QttPropertySet;
import com.isahl.chess.bishop.protocol.mqtt.model.QttType;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.util.IoUtil;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * MQTT CONNECT 报文
 *
 * <p>支持 MQTT v3.1.1 和 v5.0 协议。 MQTT v5.0 在可变头部之后增加了属性字段。
 *
 * @author william.d.zk
 * @date 2019-05-02
 * @updated 2024 - 增加 MQTT v5.0 属性支持
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_CONTROL_SERIAL, serial = 0x111)
public class X111_QttConnect extends QttControl {
  private static final int MAX_USER_NAME_LENGTH = 127;
  private static final int MAX_PASSWORD_LENGTH = 127;

  public X111_QttConnect() {
    generateCtrl(false, false, ALMOST_ONCE, QttType.CONNECT);
  }

  /** MQTT v5.0 属性集合 */
  private QttPropertySet _Properties;

  // 常用 v5 属性缓存（用于快速访问）
  private Integer _ReceiveMaximum;
  private Integer _MaximumPacketSize;
  private Integer _TopicAliasMaximum;
  private Long _SessionExpiryInterval;
  private Boolean _RequestProblemInformation;
  private Boolean _RequestResponseInformation;
  private String _AuthenticationMethod;
  private byte[] _AuthenticationData;

  private byte mAttr;
  private long mKeepAlive;
  private byte mVersion;
  private String mUserName;
  private String mPassword;
  private String mClientId;
  private String mWillTopic;
  private byte[] mWillMessage;

  // MQTT 协议名常量
  private final int _MQTT = IoUtil.readInt(new byte[] {'M', 'Q', 'T', 'T'}, 0);

  @Override
  public String toString() {
    return String.format(
        "%s:[version:%d ctrl-code %#x client-id:%s clean:%s will-qos:%s will-retain:%s will-topic:%s will-message:%s user:%s keepalive-mills:%d properties:%s]",
        getClass().getSimpleName(),
        mVersion,
        mAttr,
        getClientId(),
        isClean(),
        getWillLevel(),
        isWillRetain(),
        getWillTopic(),
        Objects.nonNull(getWillMessage())
            ? new String(getWillMessage(), StandardCharsets.UTF_8)
            : null,
        getUserName(),
        getKeepAlive(),
        _Properties);
  }

  @Override
  public boolean isMapping() {
    return true;
  }

  enum Flag {
    UserName(0x80),
    Password(0x40),
    WillRetain(0x20),
    WillQoS(0x18),
    Will(0x04),
    Clean(0x02);

    private final int _Mask;

    Flag(int mask) {
      _Mask = mask;
    }

    int getMask() {
      return _Mask;
    }
  }

  private void checkReserved() {
    if ((0x01 & mAttr) != 0) {
      throw new IllegalArgumentException("Flag error 0 bit->reserved 1");
    }
  }

  private void checkWillOpCode() {
    if (!hasWill() && getWillLevel() != ALMOST_ONCE) {
      throw new IllegalStateException("will flag 0 must with will-Qos ALMOST_ONCE(0)");
    }
  }

  public boolean isClean() {
    return (mAttr & Flag.Clean.getMask()) > 0;
  }

  public void setClean() {
    mAttr |= (byte) Flag.Clean.getMask();
  }

  public void setVersion(byte version) {
    mVersion = version;
    if (mContext != null) {
      mContext.setVersion(mVersion);
    }
  }

  public int getVersion() {
    return mVersion;
  }

  public void setKeepAlive(int seconds) {

    if (seconds > 0xFFFF) {
      throw new ZException("keep alive illegal argument [ %d ] ", seconds);
    }
    mKeepAlive = TimeUnit.SECONDS.toMillis(seconds);
  }

  public long getKeepAlive() {
    return mKeepAlive;
  }

  public X111_QttConnect setWill(Level level, boolean retain) {

    if (retain) {
      setWillRetain();
    } else {
      mAttr |= (byte) Flag.Will.getMask();
    }
    setLevel(level);
    return this;
  }

  public boolean hasWill() {
    return (mAttr & Flag.Will.getMask()) > 0;
  }

  public void setWillRetain() {
    mAttr |= (byte) (Flag.Will.getMask() | Flag.WillRetain.getMask());
  }

  public boolean isWillRetain() {
    return (mAttr & Flag.WillRetain.getMask()) > 0;
  }

  public void setUserName(String name) {
    if (isBlank(name)) {
      throw new NullPointerException("user name within [null]");
    }
    mUserName = name;
    mAttr |= (byte) Flag.UserName.getMask();
  }

  public String getUserName() {
    return mUserName;
  }

  public boolean hasUserName() {
    return (mAttr & Flag.UserName.getMask()) > 0;
  }

  public void setPassword(String password) {
    if (isBlank(password)) {
      throw new NullPointerException("password within [null]");
    }
    mPassword = password;
    mAttr |= (byte) Flag.Password.getMask();
  }

  public String getPassword() {
    return mPassword;
  }

  public boolean hasPassword() {
    return (mAttr & Flag.Password.getMask()) > 0;
  }

  public void setClientId(String id) {
    if (isBlank(id)) {
      throw new IllegalArgumentException(
          "unsupported anonymous access,server never create temporary client-id");
    }
    mClientId = id;
  }

  public String getClientId() {
    return mClientId;
  }

  public X111_QttConnect setWillTopic(String topic) {
    if (isBlank(topic)) {
      throw new NullPointerException("will topic within [null]");
    }
    mWillTopic = topic;
    mAttr |= (byte) Flag.Will.getMask();
    return this;
  }

  public String getWillTopic() {
    return mWillTopic;
  }

  public X111_QttConnect setWillMessage(byte[] message) {
    mWillMessage = Objects.requireNonNull(message);
    int messageLength = message.length;
    if (messageLength > 65535) {
      throw new IndexOutOfBoundsException(
          String.format("will message length [%d] out of bounds", messageLength));
    }
    mAttr |= (byte) Flag.Will.getMask();
    return this;
  }

  public byte[] getWillMessage() {
    return mWillMessage;
  }

  public X111_QttConnect setWillLevel(Level level) {
    mAttr |= (byte) (level.getValue() << 3);
    return this;
  }

  public Level getWillLevel() {
    return Level.valueOf((mAttr & Flag.WillQoS.getMask()) >> 3);
  }

  // ==================== MQTT v5.0 属性访问方法 ====================

  /** 获取属性集合 */
  public QttPropertySet getProperties() {
    if (_Properties == null) {
      _Properties = new QttPropertySet();
    }
    return _Properties;
  }

  /** 设置属性集合 */
  public void setProperties(QttPropertySet properties) {
    _Properties = properties;
    // 更新缓存
    if (properties != null) {
      _ReceiveMaximum = properties.getReceiveMaximum();
      _MaximumPacketSize = properties.getMaximumPacketSize();
      _TopicAliasMaximum = properties.getTopicAliasMaximum();
      _SessionExpiryInterval = properties.getSessionExpiryInterval();
      _AuthenticationMethod = properties.getAuthenticationMethod();
      _AuthenticationData = properties.getAuthenticationData();
    }
  }

  /** 检查是否为 MQTT v5.0 */
  public boolean isV5() {
    return mVersion == VERSION_V5_0;
  }

  /**
   * 获取接收最大值 (MQTT v5.0)
   *
   * <p>表示客户端愿意同时处理的 QoS 1 和 QoS 2 消息的最大数量
   *
   * @return 接收最大值，未设置返回默认值 65535
   */
  public int getReceiveMaximum() {
    if (_ReceiveMaximum == null && _Properties != null) {
      _ReceiveMaximum = _Properties.getReceiveMaximum();
    }
    return _ReceiveMaximum != null ? _ReceiveMaximum : 65535;
  }

  /** 设置接收最大值 (MQTT v5.0) */
  public void setReceiveMaximum(int value) {
    _ReceiveMaximum = value;
    getProperties().setReceiveMaximum(value);
  }

  /**
   * 获取最大报文大小 (MQTT v5.0)
   *
   * <p>表示客户端愿意接受的最大报文大小
   *
   * @return 最大报文大小，0 表示未限制，未设置返回 0
   */
  public int getMaximumPacketSize() {
    if (_MaximumPacketSize == null && _Properties != null) {
      _MaximumPacketSize = _Properties.getMaximumPacketSize();
    }
    return _MaximumPacketSize != null ? _MaximumPacketSize : 0;
  }

  /** 设置最大报文大小 (MQTT v5.0) */
  public void setMaximumPacketSize(int size) {
    _MaximumPacketSize = size;
    getProperties().setMaximumPacketSize(size);
  }

  /**
   * 获取主题别名最大值 (MQTT v5.0)
   *
   * @return 主题别名最大值，未设置返回 0
   */
  public int getTopicAliasMaximum() {
    if (_TopicAliasMaximum == null && _Properties != null) {
      _TopicAliasMaximum = _Properties.getTopicAliasMaximum();
    }
    return _TopicAliasMaximum != null ? _TopicAliasMaximum : 0;
  }

  /** 设置主题别名最大值 (MQTT v5.0) */
  public void setTopicAliasMaximum(int value) {
    _TopicAliasMaximum = value;
    getProperties().setTopicAliasMaximum(value);
  }

  /**
   * 获取会话过期时间间隔 (MQTT v5.0)
   *
   * <p>会话在网络连接断开后保持的秒数，0 表示会话随连接断开而结束
   *
   * @return 会话过期时间间隔（秒），未设置返回 0
   */
  public long getSessionExpiryInterval() {
    if (_SessionExpiryInterval == null && _Properties != null) {
      _SessionExpiryInterval = _Properties.getSessionExpiryInterval();
    }
    return _SessionExpiryInterval != null ? _SessionExpiryInterval : 0;
  }

  /** 设置会话过期时间间隔 (MQTT v5.0) */
  public void setSessionExpiryInterval(long seconds) {
    _SessionExpiryInterval = seconds;
    getProperties().setSessionExpiryInterval(seconds);
  }

  /** 是否请求问题信息 (MQTT v5.0) */
  public boolean isRequestProblemInformation() {
    if (_RequestProblemInformation == null && _Properties != null) {
      Integer value =
          _Properties.getProperty(
              com.isahl.chess.bishop.protocol.mqtt.model.QttProperty.REQUEST_PROBLEM_INFORMATION);
      _RequestProblemInformation = value == null || value == 1;
    }
    return _RequestProblemInformation != null ? _RequestProblemInformation : true;
  }

  /** 设置是否请求问题信息 (MQTT v5.0) */
  public void setRequestProblemInformation(boolean request) {
    _RequestProblemInformation = request;
    getProperties()
        .setProperty(
            com.isahl.chess.bishop.protocol.mqtt.model.QttProperty.REQUEST_PROBLEM_INFORMATION,
            request ? 1 : 0);
  }

  /** 是否请求响应信息 (MQTT v5.0) */
  public boolean isRequestResponseInformation() {
    if (_RequestResponseInformation == null && _Properties != null) {
      Integer value =
          _Properties.getProperty(
              com.isahl.chess.bishop.protocol.mqtt.model.QttProperty.REQUEST_RESPONSE_INFORMATION);
      _RequestResponseInformation = value != null && value == 1;
    }
    return _RequestResponseInformation != null ? _RequestResponseInformation : false;
  }

  /** 设置是否请求响应信息 (MQTT v5.0) */
  public void setRequestResponseInformation(boolean request) {
    _RequestResponseInformation = request;
    getProperties()
        .setProperty(
            com.isahl.chess.bishop.protocol.mqtt.model.QttProperty.REQUEST_RESPONSE_INFORMATION,
            request ? 1 : 0);
  }

  /** 获取认证方法 (MQTT v5.0) */
  public String getAuthenticationMethod() {
    if (_AuthenticationMethod == null && _Properties != null) {
      _AuthenticationMethod = _Properties.getAuthenticationMethod();
    }
    return _AuthenticationMethod;
  }

  /** 设置认证方法 (MQTT v5.0) */
  public void setAuthenticationMethod(String method) {
    _AuthenticationMethod = method;
    getProperties().setAuthenticationMethod(method);
  }

  /** 获取认证数据 (MQTT v5.0) */
  public byte[] getAuthenticationData() {
    if (_AuthenticationData == null && _Properties != null) {
      _AuthenticationData = _Properties.getAuthenticationData();
    }
    return _AuthenticationData;
  }

  /** 设置认证数据 (MQTT v5.0) */
  public void setAuthenticationData(byte[] data) {
    _AuthenticationData = data;
    getProperties().setAuthenticationData(data);
  }

  // ==================== 编解码方法 ====================

  @Override
  public int prefix(ByteBuf input) {
    int protocolNameLength = input.getShort();
    if (protocolNameLength != 4) {
      throw new IndexOutOfBoundsException(
          String.format("fix head length error ![%d]", protocolNameLength));
    }
    int mqtt = input.getInt();
    if (mqtt != _MQTT) {
      throw new IllegalArgumentException("FixHead Protocol name wrong");
    }
    setVersion(input.get());
    mAttr = input.get();
    setKeepAlive(input.getUnsignedShort());
    checkWillOpCode();
    checkReserved();
    return input.readableBytes();
  }

  @Override
  public void fold(ByteBuf input, int remain) {
    input.markReader();
    super.fold(input, remain);
    input.resetReader();

    // 解析 v5 属性（在客户端标识符之前）
    if (isV5()) {
      _Properties = new QttPropertySet();
      _Properties.decode(input, mVersion);
      // 更新缓存
      _ReceiveMaximum = _Properties.getReceiveMaximum();
      _MaximumPacketSize = _Properties.getMaximumPacketSize();
      _TopicAliasMaximum = _Properties.getTopicAliasMaximum();
      _SessionExpiryInterval = _Properties.getSessionExpiryInterval();
      _AuthenticationMethod = _Properties.getAuthenticationMethod();
      _AuthenticationData = _Properties.getAuthenticationData();
    }

    int clientIdLength = input.getUnsignedShort();
    if (clientIdLength > 0) {
      mClientId = input.readUTF(clientIdLength);
    } else {
      throw new IllegalArgumentException(
          "unsupported anonymous access,server never create temporary client-id");
    }

    // 解析遗嘱属性（v5）
    QttPropertySet willProperties = null;
    if (isV5() && hasWill()) {
      willProperties = new QttPropertySet();
      willProperties.decode(input, mVersion);
    }

    if (hasWill()) {
      int willTopicLength = input.getUnsignedShort();
      if (willTopicLength < 1) {
        throw new IllegalArgumentException("will-topic must not be blank");
      }
      mWillTopic = input.readUTF(willTopicLength);
      int willMessageLength = input.getUnsignedShort();
      if (willMessageLength < 1) {
        throw new IllegalArgumentException("will-payload must not be blank");
      }
      mWillMessage = new byte[willMessageLength];
      input.get(mWillMessage);
    }
    if (hasUserName()) {
      int userNameLength = input.getUnsignedShort();
      if (userNameLength < 1 || userNameLength > MAX_USER_NAME_LENGTH) {
        throw new IndexOutOfBoundsException(
            String.format(
                "client:[%s] { user name length within [0 < length ≤ %d], error:[%d] }",
                mClientId, MAX_USER_NAME_LENGTH, userNameLength));
      }
      mUserName = input.readUTF(userNameLength);
    }
    if (hasPassword()) {
      int passwordLength = input.getUnsignedShort();
      if (passwordLength < 1 || passwordLength > MAX_PASSWORD_LENGTH) {
        throw new IndexOutOfBoundsException(
            String.format(
                "client:[%s] { password length within [0 < length ≤ %d], error:[%d] }",
                mClientId, MAX_PASSWORD_LENGTH, passwordLength));
      }
      mPassword = input.readUTF(passwordLength);
    }
  }

  @Override
  public ByteBuf suffix(ByteBuf output) {
    output
        .putShort(4)
        .putInt(_MQTT)
        .put(mVersion)
        .put(mAttr)
        .putShort((int) TimeUnit.MILLISECONDS.toSeconds(getKeepAlive()));

    // v5 属性编码
    if (isV5()) {
      QttPropertySet props = _Properties != null ? _Properties : new QttPropertySet();
      props.encode(output, mVersion);
    }

    if (mPayload != null) {
      output.put(mPayload);
    }
    return output;
  }

  /** 计算 CONNECT 报文负载长度 */
  @Override
  public int length() {
    // 固定头部: 2(协议名长度) + 4(协议名) + 1(版本) + 1(连接标志) + 2(保活时间) = 10
    int length = 10;

    // v5 属性长度
    if (isV5() && _Properties != null) {
      int propsLength = _Properties.length(VERSION_V5_0);
      length += ByteBuf.vSizeOf(propsLength) + propsLength;
    }

    // 客户端标识符
    length += 2 + (mClientId != null ? mClientId.getBytes(StandardCharsets.UTF_8).length : 0);

    // 遗嘱（包含 v5 遗嘱属性）
    if (hasWill()) {
      if (isV5()) {
        // 遗嘱属性（简化处理，实际应有独立的遗嘱属性集合）
        length += 1; // 零长度属性占位
      }
      length += 2 + (mWillTopic != null ? mWillTopic.getBytes(StandardCharsets.UTF_8).length : 0);
      length += 2 + (mWillMessage != null ? mWillMessage.length : 0);
    }

    // 用户名
    if (hasUserName() && mUserName != null) {
      length += 2 + mUserName.getBytes(StandardCharsets.UTF_8).length;
    }

    // 密码
    if (hasPassword() && mPassword != null) {
      length += 2 + mPassword.getBytes(StandardCharsets.UTF_8).length;
    }

    return length;
  }
}
