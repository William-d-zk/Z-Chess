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
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.queen.io.core.features.model.content.IFrame;

import java.util.Objects;

/**
 * @author william.d.zk
 * @date 2019-05-02
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_FRAME_SERIAL)
public class QttFrame
        extends QttProtocol
        implements IFrame
{

    @Override
    public void header(int header)
    {
        mFrameHeader = (byte) header;
    }

    @Override
    public byte header()
    {
        return mFrameHeader;
    }

    @Override
    public boolean isCtrl()
    {
        return switch(QttType.valueOf(mFrameHeader)) {
            case CONNECT, CONNACK, DISCONNECT, AUTH, PINGREQ, PINGRESP -> true;
            default -> false;
        };
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_00_NETWORK_CONTROL;
    }

    @Override
    public Level getLevel()
    {
        return Level.valueOf((mFrameHeader & QOS_MASK) >> 1);
    }

    @Override
    public byte[] payload()
    {
        return mPayload;
    }

    @Override
    public int length()
    {
        return 1 + ByteBuf.vSizeOf(mPayload == null ? 0 : mPayload.length);
    }

    @Override
    public int lack(ByteBuf input)
    {
        int remain = input.readableBytes();
        if(remain == 0) {
            return 1;
        }
        else {
            header(input.peek(0));
            try {
                int vLength = input.vLength(1);
                return 1 + ByteBuf.vSizeOf(vLength) - remain;
            }
            catch(ZException e) {
                return 1;
            }
        }
    }

    @Override
    public IoSerial subContent()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int _sub()
    {
        return QttType.serialOf(getType());
    }

    @Override
    public QttFrame withSub(IoSerial sub)
    {
        ByteBuf encoded = Objects.requireNonNull(sub)
                                 .encode();
        if(encoded.capacity() > 0) {mPayload = encoded.array();}
        return this;
    }

    @Override
    public QttFrame withSub(byte[] sub)
    {
        mPayload = sub == null || sub.length > 0 ? sub : null;
        return this;
    }

    @Override
    public void deserializeSub(IoFactory factory)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int prefix(ByteBuf input)
    {
        mFrameHeader = input.get();
        return input.vLength();
    }

    @Override
    public void fold(ByteBuf input, int remain)
    {
        if(remain > 0) {
            mPayload = new byte[remain];
            input.get(mPayload);
        }
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output.put(mFrameHeader);
        if(mPayload != null) {
            output.vPutLength(mPayload.length);
            output.put(mPayload);
        }
        else {output.put(0);}
        return output;
    }

    public static int peekSubSerial(ByteBuf buffer)
    {
        return QttType.serialOf(QttType.valueOf(Objects.requireNonNull(buffer)
                                                       .peek(0)));
    }
}
