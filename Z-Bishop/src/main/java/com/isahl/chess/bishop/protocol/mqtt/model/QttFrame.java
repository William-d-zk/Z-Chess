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

package com.isahl.chess.bishop.protocol.mqtt.model;

import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.features.IReset;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.content.IFrame;

import java.nio.ByteBuffer;

/**
 * @author william.d.zk
 * @date 2019-05-02
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_FRAME_SERIAL)
public class QttFrame
        extends MqttProtocol
        implements IReset,
                   IFrame
{
    @Override
    public boolean isCtrl()
    {
        return switch(QttType.valueOf(getOpCode())) {
            case CONNECT, CONNACK, PINGREQ, PINGRESP, DISCONNECT, AUTH -> true;
            default -> false;
        };
    }

    @Override
    public void put(byte ctrl)
    {
        setOpCode(ctrl);
    }

    @Override
    public byte ctrl()
    {
        return getOpCode();
    }

    @Override
    public void reset()
    {
        mPayloadLength = -1;
        mFrameOpCode = 0;
        mLengthCode = 0;
    }

    private byte   mLengthCode;
    private int    mPayloadLength;
    private byte[] mPayload;

    public void setLengthCode(byte lengthCode)
    {
        mLengthCode = lengthCode;
    }

    public boolean isLengthCodeLack()
    {
        return (mLengthCode & 0x80) != 0;
    }

    @Override
    public int lack(int position)
    {
        mPayloadLength += (mLengthCode & 0x7F) << (position * 7);
        if(isLengthCodeLack()) {return 1;}
        return mPayloadLength;
    }

    public int getPayloadLength()
    {
        return mPayloadLength;
    }

    @Override
    public void put(byte[] payload)
    {
        if(payload != null && (payload.length > 268435455 || payload.length < 2)) {
            throw new IndexOutOfBoundsException();
        }
        mPayload = payload;
        mPayloadLength = mPayload == null ? 0 : mPayload.length;
    }

    @Override
    public int length()
    {
        return 1 + mPayloadLength +
               (mPayloadLength < 128 ? 1 : mPayloadLength < 16384 ? 2 : mPayloadLength < 2097152 ? 3 : 4);
    }

    @Override
    public void decodec(ByteBuffer input)
    {
        setOpCode(input.get());
        if(input.hasRemaining()) {
            mPayloadLength = IoUtil.readVariableIntLength(input);
            mPayload = new byte[mPayloadLength];
            input.get(mPayload);
        }
    }

    @Override
    public void encodec(ByteBuffer output)
    {
        output.put(mFrameOpCode);
        output.put(IoUtil.variableLength(mPayloadLength));
        if(mPayloadLength > 0) {
            output.put(mPayload);
        }
    }

    @Override
    public int _sub()
    {
        return QttType.serialOf(getType());
    }
}
