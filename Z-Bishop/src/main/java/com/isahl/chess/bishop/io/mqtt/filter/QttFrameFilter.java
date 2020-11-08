/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.bishop.io.mqtt.filter;

import java.nio.ByteBuffer;

import com.isahl.chess.bishop.io.mqtt.QttContext;
import com.isahl.chess.bishop.io.mqtt.QttFrame;
import com.isahl.chess.queen.io.core.async.AioFilterChain;
import com.isahl.chess.queen.io.core.async.AioPacket;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IPacket;
import com.isahl.chess.queen.io.core.inf.IProtocol;

/**
 * @author william.d.zk
 * 
 * @date 2019-05-07
 */
public class QttFrameFilter
        extends
        AioFilterChain<QttContext,
                       QttFrame,
                       IPacket>
{
    public final static String NAME = "mqtt_frame";

    public QttFrameFilter()
    {
        super(NAME);
    }

    @Override
    public ResultType seek(QttContext context, QttFrame output)
    {
        return context.isOutConvert() ? ResultType.NEXT_STEP
                                      : ResultType.IGNORE;
    }

    @Override
    public IPacket encode(QttContext context, QttFrame output)
    {
        ByteBuffer toWrite = ByteBuffer.allocate(output.dataLength());
        toWrite.position(output.encodec(toWrite.array(), toWrite.position()))
               .flip();
        return new AioPacket(toWrite);
    }

    @Override
    public ResultType peek(QttContext context, IPacket input)
    {
        if (context.isInConvert()) {
            ByteBuffer recvBuf = input.getBuffer();
            ByteBuffer cRvBuf = context.getRvBuffer();
            QttFrame carrier = context.getCarrier();
            int lack = context.lack();
            switch (context.position())
            {
                case -1:
                    if (lack > 0 && !recvBuf.hasRemaining()) { return ResultType.NEED_DATA; }
                    context.setCarrier(carrier = new QttFrame());
                    byte value = recvBuf.get();
                    carrier.setCtrl(value);
                    lack = context.lackLength(1, 1);
                case 0:
                    if (lack > 0 && !recvBuf.hasRemaining()) { return ResultType.NEED_DATA; }
                    carrier.setLengthCode(recvBuf.get());
                    lack = context.lackLength(1, carrier.lackLength(context.position()) + context.position() + 1);
                case 1:
                default:
                    if (lack > 0 && !recvBuf.hasRemaining()) { return ResultType.NEED_DATA; }
                    int target = context.position() + lack;
                    do {
                        if (carrier.isLengthCodeLack()) {
                            carrier.setLengthCode(recvBuf.get());
                            lack = context.lackLength(1,
                                                      target = carrier.lackLength(context.position())
                                                               + context.position()
                                                               + 1);
                        }
                        else {
                            int length = Math.min(recvBuf.remaining(), lack);
                            for (int i = 0; i < length; i++) {
                                cRvBuf.put(recvBuf.get());
                            }
                            lack = context.lackLength(length, target);
                            if (lack > 0 && !recvBuf.hasRemaining()) { return ResultType.NEED_DATA; }
                            if (carrier.getPayloadLength() > 0) {
                                byte[] payload = new byte[carrier.getPayloadLength()];
                                cRvBuf.flip();
                                cRvBuf.get(payload);
                                carrier.setPayload(payload);
                            }
                            cRvBuf.clear();
                            return ResultType.NEXT_STEP;
                        }
                    }
                    while (recvBuf.hasRemaining());
                    return ResultType.NEED_DATA;
            }
        }
        return ResultType.IGNORE;
    }

    @Override
    public QttFrame decode(QttContext context, IPacket input)
    {
        QttFrame frame = context.getCarrier();
        context.finish();
        return frame;
    }

    @Override
    public <C extends IPContext,
            O extends IProtocol> ResultType pipeSeek(C context, O output)
    {
        if (checkType(output, IProtocol.FRAME_SERIAL) && context instanceof QttContext) {
            return seek((QttContext) context, (QttFrame) output);
        }
        return ResultType.IGNORE;
    }

    @Override
    public <C extends IPContext,
            I extends IProtocol> ResultType pipePeek(C context, I input)
    {
        if (checkType(input, IProtocol.PACKET_SERIAL) && context instanceof QttContext) {
            return peek((QttContext) context, (IPacket) input);
        }
        return ResultType.IGNORE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends IPContext,
            O extends IProtocol,
            I extends IProtocol> I pipeEncode(C context, O output)
    {
        return (I) encode((QttContext) context, (QttFrame) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends IPContext,
            O extends IProtocol,
            I extends IProtocol> O pipeDecode(C context, I input)
    {
        return (O) decode((QttContext) context, (IPacket) input);
    }

}
