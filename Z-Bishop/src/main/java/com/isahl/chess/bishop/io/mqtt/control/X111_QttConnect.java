/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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
package com.isahl.chess.bishop.io.mqtt.control;

import com.isahl.chess.bishop.io.mqtt.QttControl;
import com.isahl.chess.bishop.io.mqtt.QttType;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.inf.IConsistent;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.isahl.chess.king.base.util.IoUtil.isBlank;
import static com.isahl.chess.queen.io.core.inf.IQoS.Level.ALMOST_ONCE;

/**
 * @author william.d.zk
 * 
 * @date 2019-05-02
 */
public class X111_QttConnect
        extends
        QttControl
        implements
        IConsistent
{
    private final static int MAX_USER_NAME_LENGTH = 127;
    private final static int MAX_PASSWORD_LENGTH  = 127;

    public final static int COMMAND = 0x111;

    public X111_QttConnect()
    {
        super(COMMAND);
        setCtrl(generateCtrl(false, false, ALMOST_ONCE, QttType.CONNECT));
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public int dataLength()
    {
        return 10 + getClientIdLength() + getWillLength() + getUserNameLength() + getPasswordLength();
    }

    @Override
    public int getPriority()
    {
        return QOS_PRIORITY_00_NETWORK_CONTROL;
    }

    private boolean mFlagUserName;
    private boolean mFlagPassword;
    private boolean mFlagWillRetain;
    private Level   mFlagWillQoS;
    private boolean mFlagWill;
    private boolean mFlagClean;
    private int     mKeepAlive;
    private String  mUserName;
    private String  mPassword;
    private String  mClientId;
    private int     mClientIdLength;
    private String  mWillTopic;
    private byte[]  mWillMessage;

    private final int _MQTT = IoUtil.readInt(new byte[]{'M',
                                                        'Q',
                                                        'T',
                                                        'T'},
                                             0);

    @Override
    public String toString()
    {
        return String.format("connect:[ctrl-code %x clientId:%s clean:%s willQoS:%s willRetain:%s willTopic:%s willMessage:%s user:%s password:%s keepalive:%d ]",
                             getControlCode(),
                             getClientId(),
                             isClean(),
                             getWillQoS(),
                             isWillRetain(),
                             getWillTopic(),
                             Objects.nonNull(getWillMessage()) ? new String(getWillMessage(), StandardCharsets.UTF_8)
                                                               : null,
                             getUserName(),
                             getPassword(),
                             getKeepAlive()

        );
    }

    @Override
    public void reset()
    {
        super.reset();
        mFlagUserName = false;
        mFlagPassword = false;
        mFlagWill = false;
        mFlagWillQoS = null;
        mFlagWillRetain = false;
        mFlagClean = false;
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
        Clean((byte) 0x02);

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
        mFlagClean = (code & Flag.Clean.getMask()) != 0;
        mFlagWill = (code & Flag.Will.getMask()) != 0;
        mFlagWillQoS = Level.valueOf((byte) ((code & Flag.WillQoS.getMask()) >> 3));
        mFlagWillRetain = (code & Flag.WillRetain.getMask()) != 0;
        if (!mFlagWill && (mFlagWillRetain || mFlagWillQoS.ordinal() > Level.EXACTLY_ONCE.ordinal())) {
            throw new IllegalArgumentException("no will flag, will retain or will qos not 0");
        }
        mFlagPassword = (code & Flag.Password.getMask()) != 0;
        mFlagUserName = (code & Flag.UserName.getMask()) != 0;
        checkWillOpCode();
    }

    private void checkWillOpCode()
    {
        if (!mFlagWill && !mFlagWillQoS.equals(ALMOST_ONCE)) {
            throw new IllegalStateException("will flag 0 must with will-Qos ALMOST_ONCE(0)");
        }
    }

    private int getControlCode()
    {
        byte code = 0;
        code |= mFlagClean ? Flag.Clean.getMask(): 0;
        code |= mFlagWill ? Flag.Will.getMask(): 0;
        code |= mFlagWill ? mFlagWillQoS.ordinal() << 3: 0;
        code |= mFlagWill && mFlagWillRetain ? Flag.WillRetain.getMask(): 0;
        code |= mFlagPassword ? Flag.Password.getMask(): 0;
        code |= mFlagUserName ? Flag.UserName.getMask(): 0;
        return code;
    }

    public boolean isClean()
    {
        return mFlagClean;
    }

    public void setClean()
    {
        mFlagClean = true;
    }

    public void setKeepAlive(int seconds)
    {
        mKeepAlive = seconds;
    }

    public int getKeepAlive()
    {
        return mKeepAlive;
    }

    public void setWill(Level level, boolean retain)
    {
        mFlagWillQoS = level;
        mFlagWillRetain = retain;
        mFlagWill = true;
    }

    public boolean hasWill()
    {
        return mFlagWill;
    }

    public void setWillQoS(Level level)
    {
        mFlagWillQoS = level;
    }

    public Level getWillQoS()
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
        if (Objects.isNull(password)) { throw new NullPointerException("password within [null]"); }
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
        return mClientIdLength + 2;
    }

    public void setClientId(String id)
    {
        mClientIdLength = isBlank(id) ? 0: id.getBytes().length;
        if (mClientIdLength < 1) {
            setClean();
        }
        mClientId = id;
    }

    public String getClientId()
    {
        return mClientId;
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

    public void setWillMessage(byte[] message)
    {
        if (Objects.isNull(message)) { throw new NullPointerException("will message with null "); }
        int messageLength = message.length;
        if (messageLength > 65535) {
            throw new IndexOutOfBoundsException(String.format("will message length [%d] out of bounds", messageLength));
        }
        mWillMessage = message;
        mFlagWill = true;
    }

    public byte[] getWillMessage()
    {
        return mWillMessage;
    }

    private int getWillLength()
    {
        if (mFlagWill) {
            byte[] varWillTopic = mWillTopic.getBytes(StandardCharsets.UTF_8);
            return 4 + varWillTopic.length + mWillMessage.length;
        }
        return 0;
    }

    private int getUserNameLength()
    {
        if (mFlagUserName) {
            byte[] varUserName = mUserName.getBytes(StandardCharsets.UTF_8);
            return 2 + varUserName.length;
        }
        return 0;
    }

    private int getPasswordLength()
    {
        return mFlagPassword ? 2 + mPassword.getBytes(StandardCharsets.UTF_8).length: 0;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        int protocolNameLength = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        if (protocolNameLength != 4) {
            throw new IndexOutOfBoundsException(String.format("fix head length error ![%d]", protocolNameLength));
        }
        int mqtt = IoUtil.readInt(data, pos);
        pos += protocolNameLength;
        if (mqtt != _MQTT) { throw new IllegalArgumentException("FixHead Protocol name wrong"); }
        mVersion = data[pos++];
        setControlCode(data[pos++]);
        mKeepAlive = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        mClientIdLength = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        if (mClientIdLength > 0) {
            mClientId = new String(data, pos, mClientIdLength, StandardCharsets.UTF_8);
            pos += mClientIdLength;
        }
        else {
            throw new IllegalArgumentException("unsupported anonymous access,server never create temporary client-id");
        }
        if (mFlagWill) {
            int willTopicLength = IoUtil.readUnsignedShort(data, pos);
            pos += 2;
            if (willTopicLength < 1) throw new IllegalArgumentException("will-topic must not be blank");
            mWillTopic = new String(data, pos, willTopicLength, StandardCharsets.UTF_8);
            pos += willTopicLength;
            int willMessageLength = IoUtil.readUnsignedShort(data, pos);
            pos += 2;
            if (willMessageLength < 1) throw new IllegalArgumentException("will-payload must not be blank");
            mWillMessage = new byte[willMessageLength];
            pos = IoUtil.read(data, pos, mWillMessage, 0, willMessageLength);
        }
        if (mFlagUserName) {
            int userNameLength = IoUtil.readUnsignedShort(data, pos);
            pos += 2;
            if (userNameLength < 1 || userNameLength > MAX_USER_NAME_LENGTH) {
                throw new IndexOutOfBoundsException(String.format("%s { user name length within [0 < length < %d], error:[%d] }",
                                                                  mClientId,
                                                                  MAX_USER_NAME_LENGTH + 1,
                                                                  userNameLength));
            }
            mUserName = new String(data, pos, userNameLength, StandardCharsets.UTF_8);
            pos += userNameLength;
        }
        if (mFlagPassword) {
            int passwordLength = IoUtil.readUnsignedShort(data, pos);
            pos += 2;
            if (passwordLength < 1 || passwordLength > MAX_PASSWORD_LENGTH) {
                throw new IndexOutOfBoundsException(String.format("%s { password length within [0 < length < %d], error:[%d] }",
                                                                  mClientId,
                                                                  MAX_PASSWORD_LENGTH + 1,
                                                                  passwordLength));
            }
            byte[] pwd = new byte[passwordLength];

            pos = IoUtil.read(data, pos, pwd, 0, passwordLength);
            mPassword = new String(pwd, StandardCharsets.UTF_8);
        }
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeShort(4, data, pos);
        pos += IoUtil.writeInt(_MQTT, data, pos);
        // 此处必须分开写，否则直接写到writeByte方法中会出现类型推定错误
        pos += IoUtil.writeByte(mVersion, data, pos);
        pos += IoUtil.writeByte(getControlCode(), data, pos);
        pos += IoUtil.writeShort(getKeepAlive(), data, pos);
        pos += IoUtil.writeShort(mClientIdLength, data, pos);
        if (mClientIdLength > 0) {
            pos += IoUtil.write(mClientId.getBytes(), data, pos);
        }
        if (mFlagWill) {
            if (isBlank(mWillTopic)) { throw new NullPointerException("will topic within [null]"); }
            byte[] varWillTopic = mWillTopic.getBytes(StandardCharsets.UTF_8);
            pos += IoUtil.writeShort(varWillTopic.length, data, pos);
            pos += IoUtil.write(varWillTopic, data, pos);
            if (Objects.isNull(mWillMessage)) { throw new NullPointerException("will message within [null]"); }
            pos += IoUtil.writeShort(mWillMessage.length, data, pos);
            pos += IoUtil.write(mWillMessage, data, pos);
        }
        if (mFlagUserName) {
            byte[] varUserName = mUserName.getBytes(StandardCharsets.UTF_8);
            if (varUserName.length > MAX_USER_NAME_LENGTH) {
                throw new IndexOutOfBoundsException(String.format(" user name length within [0 < length < %d], error:[%d]",
                                                                  MAX_USER_NAME_LENGTH + 1,
                                                                  varUserName.length));
            }
            pos += IoUtil.writeShort(varUserName.length, data, pos);
            pos += IoUtil.write(varUserName, data, pos);
        }
        if (mFlagPassword) {
            byte[] pwd = mPassword.getBytes(StandardCharsets.UTF_8);
            if (pwd.length > MAX_PASSWORD_LENGTH) {
                throw new IndexOutOfBoundsException(String.format(" password length within [0 < length < %d], error:[%d]",
                                                                  MAX_PASSWORD_LENGTH + 1,
                                                                  pwd.length));
            }
            pos += IoUtil.writeShort(pwd.length, data, pos);
            pos += IoUtil.write(pwd, data, pos);
        }
        return pos;
    }

    @Override
    public long getOrigin()
    {
        return getSession().getIndex();
    }
}