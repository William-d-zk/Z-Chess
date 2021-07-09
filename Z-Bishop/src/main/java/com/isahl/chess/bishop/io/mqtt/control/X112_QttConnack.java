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

import com.isahl.chess.bishop.io.mqtt.QttCode;
import com.isahl.chess.bishop.io.mqtt.QttControl;
import com.isahl.chess.bishop.io.mqtt.QttType;
import com.isahl.chess.king.base.inf.ICode;
import com.isahl.chess.king.base.util.IoUtil;

import static com.isahl.chess.queen.io.core.inf.IQoS.Level.ALMOST_ONCE;

/**
 * @author william.d.zk
 * @date 2019-05-11
 */
public class X112_QttConnack
        extends QttControl
{
    public final static int COMMAND = 0x112;

    private boolean mPresent;
    private byte    mResponseCode;

    public X112_QttConnack()
    {
        super(COMMAND);
        setCtrl(generateCtrl(false, false, ALMOST_ONCE, QttType.CONNACK));
    }

    @Override
    public int dataLength()
    {
        return 2;
    }

    @Override
    public int getPriority()
    {
        return QOS_PRIORITY_00_NETWORK_CONTROL;
    }

    public boolean isPresent()
    {
        return mPresent;
    }

    public void setPresent()
    {
        mPresent = true;
    }

    public QttCode getCode()
    {
        return QttCode.valueOf(mResponseCode & 0xFF, mVersion);
    }

    private void setCode(ICode code)
    {
        mResponseCode = (byte) code.getCode(mVersion);
    }

    public void responseOk()
    {
        mPresent = true;
        setCode(QttCode.OK);
    }

    public void responseClean()
    {
        mPresent = false;
        setCode(QttCode.OK);
    }

    public void rejectUnsupportedVersion()
    {
        mPresent = false;
        setCode(QttCode.REJECT_UNSUPPORTED_VERSION_PROTOCOL);
    }

    public void rejectIdentifier()
    {
        mPresent = false;
        setCode(QttCode.REJECT_IDENTIFIER);
    }

    public void rejectServerUnavailable()
    {
        mPresent = false;
        setCode(QttCode.REJECT_SERVER_UNAVAILABLE);
    }

    public void rejectBadUserOrPassword()
    {
        mPresent = false;
        setCode(QttCode.REJECT_BAD_USER_OR_PASSWORD);
    }

    public void rejectNotAuthorized()
    {
        mPresent = false;
        setCode(QttCode.REJECT_NOT_AUTHORIZED);
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeByte(mPresent ? 1 : 0, data, pos);
        pos += IoUtil.writeByte(mResponseCode, data, pos);
        return pos;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mPresent = data[pos++] > 0;
        mResponseCode = data[pos++];
        return pos;
    }

    public boolean isIllegalState()
    {
        return !mPresent || mResponseCode > 0;
    }

    public boolean isOk()
    {
        return mPresent && mResponseCode == 0;
    }

    @Override
    public String toString()
    {
        return String.format("%#x connack:[present:%s,result:%s]", COMMAND, mPresent, getCode());
    }

}
