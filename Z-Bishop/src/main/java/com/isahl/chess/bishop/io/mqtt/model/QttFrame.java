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

package com.isahl.chess.bishop.io.mqtt.model;

import com.isahl.chess.king.base.features.IReset;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.channels.IVariableLength;
import com.isahl.chess.queen.io.core.features.model.content.IFrame;

import java.nio.ByteBuffer;

/**
 * @author william.d.zk
 * @date 2019-05-02
 */
public class QttFrame
        extends MqttProtocol
        implements IReset,
                   IFrame,
                   IVariableLength
{
    @Override
    public boolean isCtrl()
    {
        int head = getOpCode() & 240;
        return head == QttType.CONNECT.getValue() || head == QttType.CONNACK.getValue() ||
               head == QttType.PINGREQ.getValue() || head == QttType.PINGRESP.getValue() ||
               head == QttType.DISCONNECT.getValue() || head == QttType.AUTH.getValue();
    }

    @Override
    public void putCtrl(byte ctrl)
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
    public int lackLength(int position)
    {
        mPayloadLength += (mLengthCode & 0x7F) << (position * 7);
        if(isLengthCodeLack()) { return 1; }
        return mPayloadLength;
    }

    public int getPayloadLength()
    {
        return mPayloadLength;
    }

    @Override
    public void putPayload(byte[] payload)
    {
        if(payload != null && (payload.length > 268435455 || payload.length < 2)) {
            throw new IndexOutOfBoundsException();
        }
        mPayload = payload;
        mPayloadLength = mPayload == null ? 0 : mPayload.length;
    }

    @Override
    public byte[] payload()
    {
        return mPayload;
    }

    private final static int MQTT_FRAME = FRAME_SERIAL + 2;

    @Override
    public int dataLength()
    {
        return 1 + mPayloadLength +
               (mPayloadLength < 128 ? 1 : mPayloadLength < 16384 ? 2 : mPayloadLength < 2097152 ? 3 : 4);
    }

    @Override
    public int serial()
    {
        return MQTT_FRAME;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        setOpCode(data[pos++]);
        if(pos < data.length) {
            mPayloadLength = IoUtil.readVariableIntLength(ByteBuffer.wrap(data, pos, data.length - pos));
            pos += mPayloadLength;
            mPayload = new byte[mPayloadLength];
            pos = IoUtil.read(data, pos, mPayload);
        }
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeByte(mFrameOpCode, data, pos);
        byte[] lengthVar = IoUtil.variableLength(mPayloadLength);
        pos += IoUtil.write(lengthVar, 0, data, pos, lengthVar.length);
        if(mPayloadLength > 0) {
            pos += IoUtil.write(mPayload, data, pos);
        }
        return pos;
    }

    @Override
    public int command()
    {
        return getType().getValue();
    }
}
