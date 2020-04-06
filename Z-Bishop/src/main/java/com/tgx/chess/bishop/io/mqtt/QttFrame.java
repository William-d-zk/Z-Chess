/*
 * MIT License
 *
 * Copyright (c) 2016~2020 Z-Chess
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

package com.tgx.chess.bishop.io.mqtt;

import java.nio.ByteBuffer;

import com.tgx.chess.king.base.inf.IReset;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.io.core.inf.IFrame;
import com.tgx.chess.queen.io.core.inf.IVariableLength;

/**
 * @author william.d.zk
 * @date 2019-05-02
 */
public class QttFrame
        extends
        MqttProtocol
        implements
        IReset,
        IFrame,
        IVariableLength
{
    @Override
    public boolean isCtrl()
    {
        int head = getOpCode() & 240;
        return head == QTT_TYPE.CONNECT._Value
               || head == QTT_TYPE.CONNACK._Value
               || head == QTT_TYPE.PINGREQ._Value
               || head == QTT_TYPE.PINGRESP._Value
               || head == QTT_TYPE.DISCONNECT._Value;
    }

    @Override
    public void setCtrl(byte ctrl)
    {
        setOpCode(ctrl);
    }

    @Override
    public byte getCtrl()
    {
        return getOpCode();
    }

    @Override
    public void reset()
    {
        mPayloadLength = -1;
        frame_op_code = 0;
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
    public int payloadLengthLack(int position)
    {
        mPayloadLength += (mLengthCode & 0x7F) << (position * 7);
        if (isLengthCodeLack()) { return 1; }
        return mPayloadLength;
    }

    public int getPayloadLength()
    {
        return mPayloadLength;
    }

    @Override
    public void setPayload(byte[] payload)
    {
        if (payload != null && (payload.length > 268435455 || payload.length < 2)) {
            throw new IndexOutOfBoundsException();
        }
        mPayload = payload;
        mPayloadLength = mPayload == null ? 0
                                          : mPayload.length;
    }

    @Override
    public byte[] getPayload()
    {
        return mPayload;
    }

    private final static int MQTT_FRAME = FRAME_SERIAL + 2;

    @Override
    public int dataLength()
    {
        return 1
               + mPayloadLength
               + (mPayloadLength < 128 ? 1
                                       : mPayloadLength < 16384 ? 2
                                                                : mPayloadLength < 2097152 ? 3
                                                                                           : 4);
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
        if (pos < data.length) {
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
        pos += IoUtil.writeByte(frame_op_code, data, pos);
        byte[] lengthVar = IoUtil.variableLength(mPayloadLength);
        pos += IoUtil.write(lengthVar, 0, data, pos, lengthVar.length);
        if (mPayloadLength > 0) {
            pos += IoUtil.write(mPayload, data, pos);
        }
        return pos;
    }
}
