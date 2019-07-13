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

import com.tgx.chess.bishop.io.mqtt.bean.QttControl;
import com.tgx.chess.king.base.util.IoUtil;

import static com.tgx.chess.queen.io.core.inf.IQoS.Level.ALMOST_ONCE;

/**
 * @author william.d.zk
 * @date 2019-05-11
 */
public class X112_QttConnack
        extends
        QttControl
{
    public final static int COMMAND = 0x112;

    private boolean mPresent;
    private byte    mResponseCode;

    public X112_QttConnack()
    {
        super(COMMAND);
        setCtrl(generateCtrl(false, false, ALMOST_ONCE, QTT_TYPE.CONNACK));
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

    public Code getCode()
    {
        return Code.valueOf(mResponseCode);
    }

    private void setCode(Code code)
    {
        mResponseCode = code.getValue();
    }

    public void responseOk()
    {
        mPresent = true;
        setCode(Code.ACCEPT);
    }

    public void responseClean()
    {
        mPresent = false;
        setCode(Code.ACCEPT);
    }

    public void rejectUnacceptableProtocol()
    {
        mPresent = false;
        setCode(Code.REJECT_UNACCEPTABLE_PROTOCOL);
    }

    public void rejectIdentifier()
    {
        mPresent = false;
        setCode(Code.REJECT_IDENTIFIER);
    }

    public void rejectServerUnavailable()
    {
        mPresent = false;
        setCode(Code.REJECT_SERVER_UNAVAILABLE);
    }

    public void rejectBadUserOrPassword()
    {
        mPresent = false;
        setCode(Code.REJECT_BAD_USER_OR_PASSWORD);
    }

    public void rejectNotAuthorized()
    {
        mPresent = false;
        setCode(Code.REJECT_NOT_AUTHORIZED);
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeByte(mPresent ? 1
                                         : 0,
                                data,
                                pos);
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
        return mPresent && mResponseCode > 0;
    }

    @Override
    public String toString()
    {
        return String.format("connack:[present:%s,result:%s]", mPresent, getCode());
    }

    public enum Code
    {

        ACCEPT(0),
        REJECT_UNACCEPTABLE_PROTOCOL(1),
        REJECT_IDENTIFIER(2),
        REJECT_SERVER_UNAVAILABLE(3),
        REJECT_BAD_USER_OR_PASSWORD(4),
        REJECT_NOT_AUTHORIZED(5);
        private final byte _Code;

        Code(int code)
        {
            _Code = (byte) code;
        }

        public byte getValue()
        {
            return _Code;
        }

        static Code valueOf(byte code)
        {
            switch (code)
            {
                case 0:
                    return ACCEPT;
                case 1:
                    return REJECT_UNACCEPTABLE_PROTOCOL;
                case 2:
                    return REJECT_IDENTIFIER;
                case 3:
                    return REJECT_SERVER_UNAVAILABLE;
                case 4:
                    return REJECT_BAD_USER_OR_PASSWORD;
                case 5:
                    return REJECT_NOT_AUTHORIZED;
                default:
                    throw new IllegalArgumentException("6-255 Reserved");
            }
        }
    }

}
