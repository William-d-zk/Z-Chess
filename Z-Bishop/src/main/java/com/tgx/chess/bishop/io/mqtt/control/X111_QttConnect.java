/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.tgx.chess.bishop.io.mqtt.control;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.tgx.chess.bishop.io.mqtt.bean.BaseQtt;
import com.tgx.chess.bishop.io.mqtt.bean.QttContext;
import com.tgx.chess.bishop.io.mqtt.bean.QttControl;
import com.tgx.chess.bishop.io.mqtt.bean.QttFrame;
import com.tgx.chess.king.base.util.IoUtil;

/**
 * @author william.d.zk
 * @date 2019-05-02
 */
public class X111_QttConnect
        extends
        QttControl
{
    private final static int MAX_USER_NAME_LENGTH = 127;
    private final static int MAX_PASSWORD_LENGTH  = 127;

    public final static int COMMAND = 0x111;

    public X111_QttConnect()
    {
        super(COMMAND);
        setCtrl(QttFrame.generateCtrl(false, false, BaseQtt.QOS_LEVEL.QOS_ONLY_ONCE, QttFrame.QTT_TYPE.CONNECT));
    }

    @Override
    public int dataLength()
    {
        return 11 + getClientIdLength() + getWillLength() + getUserNameLength() + getPasswordLength();
    }

    @Override
    public int getPriority()
    {
        return QOS_00_NETWORK_CONTROL;
    }

    private boolean           mFlagUserName;
    private boolean           mFlagPassword;
    private boolean           mFlagWillRetain;
    private BaseQtt.QOS_LEVEL mFlagWillQoS;
    private boolean           mFlagWill;
    private boolean           mFlagCleanSession;
    private int               mKeepAlive;
    private String            mUserName;
    private String            mPassword;
    private String            mClientId;
    private int               mClientIdLength;
    private String            mWillTopic;
    private String            mWillMessage;

    private final int _FixHeadLength = 10;
    private final int _MQTT           = IoUtil.readInt(new byte[] { 'M',
                                                                    'Q',
                                                                    'T',
                                                                    'T' },
                                                       0);

    @Override
    public void reset()
    {
        super.reset();
        mFlagUserName = false;
        mFlagPassword = false;
        mFlagWill = false;
        mFlagWillQoS = null;
        mFlagWillRetain = false;
        mFlagCleanSession = false;
        mUserName = null;
        mPassword = null;
        mKeepAlive = 0;
        mClientId = null;
        mClientIdLength = 0;
        mWillTopic = null;
        mWillMessage = null;
    }

    enum Flag
    {
        UserName((byte) 0x80),
        Password((byte) 0x40),
        WillRetain((byte) 0x20),
        WillQoS((byte) 0x18),
        Will((byte) 0x04),
        CleanSession((byte) 0x02);
        private final byte _Mask;

        Flag(byte mask)
        {
            _Mask = mask;
        }

        byte getMask()
        {
            return _Mask;
        }
    }

    private void setControlCode(byte code)
    {
        if ((0x01 & code) != 0) { throw new IllegalArgumentException("Flag error 0 bit->reserved 1"); }
        mFlagCleanSession = (code & Flag.CleanSession.getMask()) != 0;
        mFlagWill = (code & Flag.Will.getMask()) != 0;
        mFlagWillQoS = QOS_LEVEL.valueOf((byte) ((code & Flag.WillQoS.getMask()) >> 3));
        mFlagWillRetain = (code & Flag.WillRetain.getMask()) != 0;
        if (!mFlagWill && (mFlagWillRetain || mFlagWillQoS.getValue() > QOS_LEVEL.QOS_LESS_ONCE.getValue())) {
            throw new IllegalArgumentException("no will flag, will retain or will qos not 0");
        }
        mFlagPassword = (code & Flag.Password.getMask()) != 0;
        mFlagUserName = (code & Flag.UserName.getMask()) != 0;
    }

    private int getControlCode()
    {
        byte code = 0;
        code |= mFlagCleanSession ? Flag.CleanSession.getMask()
                                  : 0;
        code |= mFlagWill ? Flag.Will.getMask()
                          : 0;
        code |= mFlagWill ? mFlagWillQoS.getValue() << 3
                          : 0;
        code |= mFlagWill && mFlagWillRetain ? Flag.WillRetain.getMask()
                                             : 0;
        code |= mFlagPassword ? Flag.Password.getMask()
                              : 0;
        code |= mFlagUserName ? Flag.UserName.getMask()
                              : 0;
        return code;
    }

    public boolean isCleanSession()
    {
        return mFlagCleanSession;
    }

    public void setCleanSession()
    {
        mFlagCleanSession = true;
    }

    public void setKeepAlive(int seconds)
    {
        mKeepAlive = seconds;
    }

    public int getKeepAlive()
    {
        return mKeepAlive;
    }

    public void setWill(QOS_LEVEL level, boolean retain)
    {
        mFlagWillQoS = level;
        mFlagWillRetain = retain;
        mFlagWill = true;
    }

    public void setWillQoS(QOS_LEVEL level)
    {
        mFlagWillQoS = level;
    }

    public QOS_LEVEL getWillQoS()
    {
        return mFlagWillQoS;
    }

    public void setWillRetain()
    {
        mFlagWillRetain = true;
    }

    public boolean isWillRetain()
    {
        return mFlagWillRetain;
    }

    public void setUserName(String name)
    {
        if (Objects.isNull(name) || "".equals(name)) { throw new NullPointerException("user name within [null]"); }
        mUserName = name;
        mFlagUserName = true;
    }

    public void setPassword(String password)
    {
        if (Objects.isNull(password) || "".equals(password)) {
            throw new NullPointerException("password within [null]");
        }
        mPassword = password;
        mFlagPassword = true;
    }

    public String getUserName()
    {
        return mUserName;
    }

    public String getPassword()
    {
        return mPassword;
    }

    public int getClientIdLength()
    {
        return mClientIdLength;
    }

    public void setClientId(String id)
    {
        mClientIdLength = Objects.isNull(id) || "".equals(id) ? 0
                                                              : id.getBytes().length;
        if (mClientIdLength < 1 || mClientIdLength > 23) {
            throw new IndexOutOfBoundsException("client identity length within [0 < length < 24]");
        }
        mClientId = id;
    }

    public void setWillTopic(String topic)
    {
        if (Objects.isNull(topic) || "".equals(topic)) { throw new NullPointerException("will topic within [null]"); }
        mWillTopic = topic;
        mFlagWill = true;
    }

    public String getWillTopic()
    {
        return mWillTopic;
    }

    public void setWillMessage(String message)
    {
        if (Objects.isNull(message) || "".equals(message)) {
            throw new NullPointerException("will message with null ");
        }
        int messageLength = message.getBytes(StandardCharsets.UTF_8).length;
        if (messageLength > 65535) {
            throw new IndexOutOfBoundsException(String.format("will message length [%d] out of bounds", messageLength));
        }
        mWillMessage = message;
        mFlagWill = true;
    }

    public String getWillMessage()
    {
        return mWillMessage;
    }

    private int getWillLength()
    {
        if (mFlagWill) {
            int length = 0;
            byte[] varWillTopic = mWillTopic.getBytes(StandardCharsets.UTF_8);
            byte[] varWillTopicLength = IoUtil.variableLength(varWillTopic.length);
            length += varWillTopicLength.length + varWillTopic.length;
            byte[] varWillMessage = mWillMessage.getBytes(StandardCharsets.UTF_8);
            length += varWillMessage.length + 2;
            return length;
        }
        return 0;
    }

    private int getUserNameLength()
    {
        if (mFlagUserName) {
            byte[] varUserName = mUserName.getBytes(StandardCharsets.UTF_8);
            byte[] varUserNameLength = IoUtil.variableLength(varUserName.length);
            return varUserNameLength.length + varUserName.length;
        }
        return 0;
    }

    private int getPasswordLength()
    {
        if (mFlagPassword) {
            byte[] varPassword = mPassword.getBytes(StandardCharsets.UTF_8);
            byte[] varPasswordLength = IoUtil.variableLength(varPassword.length);
            return varPasswordLength.length + varPassword.length;
        }
        return 0;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        short fixLength = IoUtil.readShort(data, pos);
        pos += 2;
        if (fixLength != _FixHeadLength) {
            throw new IndexOutOfBoundsException(String.format("fix head length error ![%d]", fixLength));
        }
        int mqtt = IoUtil.readInt(data, pos);
        pos += 4;
        if (mqtt != _MQTT) { throw new IllegalArgumentException("FixHead Protocol name wrong"); }
        int level = data[pos++];
        int globalVersion = QttContext.getVersion()
                                          .second();
        if (level != globalVersion) { throw new IllegalArgumentException("Protocol version unsupported"); }
        setControlCode(data[pos++]);
        mKeepAlive = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        ByteBuffer payload = ByteBuffer.wrap(data, pos, data.length - pos);
        mClientIdLength = (int) IoUtil.readVariableLongLength(payload);
        pos = payload.position();
        if (mClientIdLength < 1 || mClientIdLength > 23) {
            //follow protocol 3.1, client identity can't empty
            throw new IllegalArgumentException("client identity length within [0 < length < 24]");
        }
        mClientId = new String(data, pos, mClientIdLength, StandardCharsets.UTF_8);
        payload.position(pos += mClientIdLength);
        if (mFlagWill) {
            int willTopicLength = (int) IoUtil.readVariableLongLength(payload);
            pos = payload.position();
            mWillTopic = new String(data, pos, willTopicLength, StandardCharsets.UTF_8);
            payload.position(pos += willTopicLength);
            int willMessageLength = IoUtil.readUnsignedShort(data, pos);
            pos += 2;
            mWillMessage = new String(data, pos, willMessageLength, StandardCharsets.UTF_8);
            payload.position(pos += willMessageLength);
        }
        if (mFlagUserName) {
            int userNameLength = (int) IoUtil.readVariableLongLength(payload);
            if (userNameLength < 1 || userNameLength > MAX_USER_NAME_LENGTH) {
                throw new IndexOutOfBoundsException(String.format(" user name length within [0 < length < %d], error:[%d]",
                                                                  MAX_USER_NAME_LENGTH + 1,
                                                                  userNameLength));
            }
            pos = payload.position();
            mUserName = new String(data, pos, userNameLength, StandardCharsets.UTF_8);
            payload.position(pos += userNameLength);
        }
        if (mFlagPassword) {
            int passwordLength = (int) IoUtil.readVariableLongLength(payload);
            if (passwordLength < 1 || passwordLength > MAX_PASSWORD_LENGTH) {
                throw new IndexOutOfBoundsException(String.format(" password length within [0 < length < %d], error:[%d]",
                                                                  MAX_PASSWORD_LENGTH + 1,
                                                                  passwordLength));
            }
            pos = payload.position();
            mPassword = new String(data, pos, passwordLength, StandardCharsets.UTF_8);
            payload.position(pos += passwordLength);
        }
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeShort(_FixHeadLength, data, pos);
        pos += IoUtil.writeInt(_MQTT, data, pos);
        //此处必须分开写，否则直接写到writeByte方法中会出现类型推定错误
        int version = QttContext.getVersion()
                                .second();
        pos += IoUtil.writeByte(version, data, pos);
        pos += IoUtil.writeByte(getControlCode(), data, pos);
        pos += IoUtil.writeShort(getKeepAlive(), data, pos);
        byte[] varClientIdLength = IoUtil.variableLength(mClientIdLength);
        pos += IoUtil.write(varClientIdLength, data, pos);
        pos += IoUtil.write(mClientId.getBytes(), data, pos);
        if (mFlagWill) {
            byte[] varWillTopic = mWillTopic.getBytes(StandardCharsets.UTF_8);
            byte[] varWillTopicLength = IoUtil.variableLength(varWillTopic.length);
            pos += IoUtil.write(varWillTopicLength, data, pos);
            pos += IoUtil.write(varWillTopic, data, pos);
            byte[] varWillMessage = mWillMessage.getBytes(StandardCharsets.UTF_8);
            pos += IoUtil.writeShort(varWillMessage.length, data, pos);
            pos += IoUtil.write(varWillMessage, data, pos);
        }
        if (mFlagUserName) {
            byte[] varUserName = mUserName.getBytes(StandardCharsets.UTF_8);
            if (varUserName.length > MAX_USER_NAME_LENGTH) {
                throw new IndexOutOfBoundsException(String.format(" user name length within [0 < length < %d], error:[%d]",
                                                                  MAX_USER_NAME_LENGTH + 1,
                                                                  varUserName.length));
            }
            byte[] varUserNameLength = IoUtil.variableLength(varUserName.length);
            pos += IoUtil.write(varUserNameLength, data, pos);
            pos += IoUtil.write(varUserName, data, pos);

        }
        if (mFlagPassword) {
            byte[] varPassword = mPassword.getBytes(StandardCharsets.UTF_8);
            if (varPassword.length > MAX_PASSWORD_LENGTH) {
                throw new IndexOutOfBoundsException(String.format(" password length within [0 < length < %d], error:[%d]",
                                                                  MAX_PASSWORD_LENGTH + 1,
                                                                  varPassword.length));
            }
            byte[] varPasswordLength = IoUtil.variableLength(varPassword.length);
            pos += IoUtil.write(varPasswordLength, data, pos);
            pos += IoUtil.write(varPassword, data, pos);
        }
        return pos;
    }
}