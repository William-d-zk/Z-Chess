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

import com.tgx.chess.bishop.io.mqtt.bean.QttCommand;
import com.tgx.chess.bishop.io.mqtt.bean.QttContext;
import com.tgx.chess.bishop.io.mqtt.bean.QttFrame.QOS_LEVEL;
import com.tgx.chess.king.base.util.IoUtil;

/**
 * @author william.d.zk
 * @date 2019-05-02
 */
public class X201_QttConnect<C extends QttContext>
        extends
        QttCommand<C>
{
    public final static int COMMAND = 0x201;

    public X201_QttConnect()
    {
        super(COMMAND);
    }

    @Override
    public int dataLength()
    {
        return 0;
    }

    @Override
    public int getPriority()
    {
        return QOS_00_NETWORK_CONTROL;
    }

    private boolean   mFlagUserName;
    private boolean   mFlagPassword;
    private boolean   mFlagWillRetain;
    private QOS_LEVEL mFlagWillQoS;
    private boolean   mFlagWill;
    private boolean   mFlagCleanSession;
    private int       mKeepAlive;
    private String    mUserName;
    private String    mPassword;
    private String    mClientId;

    private final int _FixHeadLength = 10;
    private final int _MQTT          = IoUtil.readInt(new byte[] { 'M',
                                                                   'Q',
                                                                   'T',
                                                                   'T' },
                                                      0);

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
            throw new IllegalArgumentException("no will flag,will retain or will qos not 0");
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
        code |= mFlagWillQoS.getValue() << 3;
        code |= mFlagWillRetain ? Flag.WillRetain.getMask()
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

    public void setWillFlag()
    {
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
        mUserName = name;
        mFlagUserName = true;
    }

    public void setPassword(String password)
    {
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
        Integer globalVersion = QttContext.getVersion()
                                          .second();
        if (level != globalVersion) { throw new IllegalArgumentException("Protocol version unsupported"); }
        setControlCode(data[pos++]);
        mKeepAlive = IoUtil.readUnsignedShort(data, pos);
        pos += 2;

        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeShort(_FixHeadLength, data, pos);
        pos += IoUtil.writeInt(_MQTT, data, pos);
        pos += IoUtil.writeByte(QttContext.getVersion()
                                          .second(),
                                data,
                                pos);
        pos += IoUtil.writeByte(getControlCode(), data, pos);
        pos += IoUtil.writeShort(getKeepAlive(), data, pos);

        return pos;
    }
}