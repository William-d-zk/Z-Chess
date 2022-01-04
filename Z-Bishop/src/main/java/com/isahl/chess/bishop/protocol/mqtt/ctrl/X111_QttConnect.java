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
package com.isahl.chess.bishop.protocol.mqtt.ctrl;

import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.mqtt.model.QttType;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.util.IoUtil;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.isahl.chess.king.base.util.IoUtil.isBlank;
import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE;

/**
 * @author william.d.zk
 * @date 2019-05-02
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_CONTROL_SERIAL,
                  serial = 0x111)
public class X111_QttConnect
        extends QttControl
{
    private final static int MAX_USER_NAME_LENGTH = 127;
    private final static int MAX_PASSWORD_LENGTH  = 127;

    public X111_QttConnect()
    {
        generateCtrl(false, false, ALMOST_ONCE, QttType.CONNECT);
    }

    @Override
    public int length()
    {
        return 10 + super.length();
    }

    private byte   mAttr;
    private int    mKeepAlive;
    private byte   mVersion;
    private String mUserName;
    private String mPassword;
    private String mClientId;
    private String mWillTopic;
    private byte[] mWillMessage;

    private final int _MQTT = IoUtil.readInt(new byte[]{ 'M', 'Q', 'T', 'T'
    }, 0);

    @Override
    public String toString()
    {
        return String.format(
                "%s:[ctrl-code %#x clientId:%s clean:%s willQoS:%s willRetain:%s willTopic:%s willMessage:%s user:%s password:%s keepalive:%d ]",
                getClass().getSimpleName(),
                mAttr,
                getClientId(),
                isClean(),
                getWillLevel(),
                isWillRetain(),
                getWillTopic(),
                Objects.nonNull(getWillMessage()) ? new String(getWillMessage(), StandardCharsets.UTF_8) : null,
                getUserName(),
                getPassword(),
                getKeepAlive());
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    enum Flag
    {
        UserName(0x80),
        Password(0x40),
        WillRetain(0x20),
        WillQoS(0x18),
        Will(0x04),
        Clean(0x02);

        private final int _Mask;

        Flag(int mask)
        {
            _Mask = mask;
        }

        int getMask()
        {
            return _Mask;
        }
    }

    private void checkReserved()
    {
        if((0x01 & mAttr) != 0) {throw new IllegalArgumentException("Flag error 0 bit->reserved 1");}
    }

    private void checkWillOpCode()
    {
        if(!hasWill() && getWillLevel() != ALMOST_ONCE) {
            throw new IllegalStateException("will flag 0 must with will-Qos ALMOST_ONCE(0)");
        }
    }

    public boolean isClean()
    {
        return (mAttr & Flag.Clean.getMask()) > 0;
    }

    public void setClean()
    {
        mAttr |= Flag.Clean.getMask();
    }

    public void setVersion(byte version)
    {
        mVersion = version;
        if(mContext != null) {
            mContext.setVersion(mVersion);
        }
    }

    public int getVersion()
    {
        return mVersion;
    }

    public void setKeepAlive(int seconds)
    {
        mKeepAlive = seconds;
    }

    public int getKeepAlive()
    {
        return mKeepAlive;
    }

    public X111_QttConnect setWill(Level level, boolean retain)
    {

        if(retain) {
            setWillRetain();
        }
        else {mAttr |= Flag.Will.getMask();}
        setLevel(level);
        return this;
    }

    public boolean hasWill()
    {
        return (mAttr & Flag.Will.getMask()) > 0;
    }

    public void setWillRetain()
    {
        mAttr |= Flag.Will.getMask() | Flag.WillRetain.getMask();
    }

    public boolean isWillRetain()
    {
        return (mAttr & Flag.WillRetain.getMask()) > 0;
    }

    public X111_QttConnect setUserName(String name)
    {
        if(isBlank(name)) {throw new NullPointerException("user name within [null]");}
        mUserName = name;
        mAttr |= Flag.UserName.getMask();
        return this;
    }

    public String getUserName()
    {
        return mUserName;
    }

    public boolean hasUserName()
    {
        return (mAttr & Flag.UserName.getMask()) > 0;
    }

    public X111_QttConnect setPassword(String password)
    {
        if(isBlank(password)) {throw new NullPointerException("password within [null]");}
        mPassword = password;
        mAttr |= Flag.Password.getMask();
        return this;
    }

    public String getPassword()
    {
        return mPassword;
    }

    public boolean hasPassword()
    {
        return (mAttr & Flag.Password.getMask()) > 0;
    }

    public X111_QttConnect setClientId(String id)
    {
        if(isBlank(id)) {
            throw new IllegalArgumentException("unsupported anonymous access,server never create temporary client-id");
        }
        mClientId = id;
        return this;
    }

    public String getClientId()
    {
        return mClientId;
    }

    public X111_QttConnect setWillTopic(String topic)
    {
        if(isBlank(topic)) {throw new NullPointerException("will topic within [null]");}
        mWillTopic = topic;
        mAttr |= Flag.Will.getMask();
        return this;
    }

    public String getWillTopic()
    {
        return mWillTopic;
    }

    public X111_QttConnect setWillMessage(byte[] message)
    {
        mWillMessage = Objects.requireNonNull(message);
        int messageLength = message.length;
        if(messageLength > 65535) {
            throw new IndexOutOfBoundsException(String.format("will message length [%d] out of bounds", messageLength));
        }
        mAttr |= Flag.Will.getMask();
        return this;
    }

    public byte[] getWillMessage()
    {
        return mWillMessage;
    }

    public X111_QttConnect setWillLevel(Level level)
    {
        mAttr |= level.getValue() << 3;
        return this;
    }

    public Level getWillLevel()
    {
        return Level.valueOf((mAttr & Flag.WillQoS.getMask()) >> 3);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int protocolNameLength = input.getShort();
        if(protocolNameLength != 4) {
            throw new IndexOutOfBoundsException(String.format("fix head length error ![%d]", protocolNameLength));
        }
        int mqtt = input.getInt();
        if(mqtt != _MQTT) {throw new IllegalArgumentException("FixHead Protocol name wrong");}
        setVersion(input.get());
        mAttr = input.get();
        setKeepAlive(input.getUnsignedShort());
        checkWillOpCode();
        checkReserved();
        return input.readableBytes();
    }

    @Override
    public void fold(ByteBuf input, int remain)
    {
        input.markReader();
        super.fold(input, remain);
        input.resetReader();
        int clientIdLength = input.getUnsignedShort();
        if(clientIdLength > 0) {
            mClientId = input.readUTF(clientIdLength);
        }
        else {
            throw new IllegalArgumentException("unsupported anonymous access,server never create temporary client-id");
        }
        if(hasWill()) {
            int willTopicLength = input.getUnsignedShort();
            if(willTopicLength < 1) {throw new IllegalArgumentException("will-topic must not be blank");}
            mWillTopic = input.readUTF(willTopicLength);
            int willMessageLength = input.getUnsignedShort();
            if(willMessageLength < 1) {throw new IllegalArgumentException("will-payload must not be blank");}
            mWillMessage = new byte[willMessageLength];
            input.get(mWillMessage);
        }
        if(hasUserName()) {
            int userNameLength = input.getShort() & 0xFFFF;
            if(userNameLength < 1 || userNameLength > MAX_USER_NAME_LENGTH) {
                throw new IndexOutOfBoundsException(String.format(
                        "client:[%s] { user name length within [0 < length ≤ %d], error:[%d] }",
                        mClientId,
                        MAX_USER_NAME_LENGTH,
                        userNameLength));
            }
            mUserName = input.readUTF(userNameLength);
        }
        if(hasPassword()) {
            int passwordLength = input.getShort() & 0xFFFF;
            if(passwordLength < 1 || passwordLength > MAX_PASSWORD_LENGTH) {
                throw new IndexOutOfBoundsException(String.format(
                        "client:[%s] { password length within [0 < length ≤ %d], error:[%d] }",
                        mClientId,
                        MAX_PASSWORD_LENGTH,
                        passwordLength));
            }
            mPassword = input.readUTF(passwordLength);
        }
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output.putShort((short) 4)
              .putInt(_MQTT)
              .put(mVersion)
              .put(mAttr)
              .putShort((short) getKeepAlive());
        if(mPayload != null) {output.put(mPayload);}
        return output;
    }

}