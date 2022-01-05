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

import com.isahl.chess.bishop.protocol.mqtt.model.CodeMqtt;
import com.isahl.chess.bishop.protocol.mqtt.model.QttType;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.features.ICode;

import static com.isahl.chess.king.config.KingCode.SUCCESS;
import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE;

/**
 * @author william.d.zk
 * @date 2019-05-11
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_CONTROL_SERIAL,
                  serial = 0x112)
public class X112_QttConnack
        extends QttControl
{

    private int mPresent;
    private int mResponseCode;

    public X112_QttConnack()
    {
        generateCtrl(false, false, ALMOST_ONCE, QttType.CONNACK);
    }

    @Override
    public int length()
    {
        return 2;
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
        return CodeMqtt.valueOf(mResponseCode, mContext.getVersion());
    }

    private void setCode(ICode code)
    {
        mResponseCode = code.getCode(mContext.getVersion());
    }

    public void responseOk()
    {
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

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return output.put(mPresent)
                     .put(mResponseCode);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        mPresent = input.get() & 0x01;
        mResponseCode = input.getUnsigned();
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
        return String.format("%#x connack:[present:%s,result:%s]", serial(), mPresent, getCode());
    }
}
