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

package com.tgx.chess.bishop.io.mqtt.filter;

import java.nio.ByteBuffer;
import java.util.Objects;

import com.tgx.chess.bishop.io.mqtt.bean.QttContext;
import com.tgx.chess.bishop.io.mqtt.bean.QttFrame;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.io.core.async.AioFilterChain;
import com.tgx.chess.queen.io.core.async.AioPacket;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author william.d.zk
 * @date 2019-05-07
 */
public class QttFrameFilter<C extends QttContext>
        extends
        AioFilterChain<C>
{
    private final Logger _Log = Logger.getLogger(getClass().getName());

    protected QttFrameFilter()
    {
        super("mqtt-frame-filter");
    }

    @Override
    public ResultType preEncode(C context, IProtocol output)
    {
        if (Objects.isNull(context) || Objects.isNull(output)) { return ResultType.ERROR; }
        switch (output.superSerial())
        {
            case IProtocol.COMMAND_SERIAL:
            case IProtocol.FRAME_SERIAL:
                return ResultType.NEXT_STEP;
            default:
                return ResultType.ERROR;
        }
    }

    @Override
    public ResultType preDecode(C context, IProtocol input)
    {
        if (context == null || input == null) { return ResultType.ERROR; }
        IPacket _package = (IPacket) input;
        ByteBuffer recvBuf = _package.getBuffer();
        ByteBuffer cRvBuf = context.getRvBuffer();
        QttFrame carrier = context.getCarrier();
        int lack = context.lack();
        switch (context.position())
        {
            case -1:
                if (lack > 0 && !recvBuf.hasRemaining()) { return ResultType.NEED_DATA; }
                context.setCarrier(carrier = new QttFrame());
                byte value = recvBuf.get();
                carrier.setOpCode(value);
                lack = context.lackLength(1, 1);
            case 0:
                if (lack > 0 && !recvBuf.hasRemaining()) { return ResultType.NEED_DATA; }
                carrier.setLengthCode(recvBuf.get());
                lack = context.lackLength(1, carrier.payloadLengthLack() + context.position());
            case 1:
            default:
                if (lack > 0 && !recvBuf.hasRemaining()) { return ResultType.NEED_DATA; }
                int target = context.position() + lack;
                do {
                    if (carrier.isLengthCodeLack()) {
                        carrier.setLengthCode(recvBuf.get());
                        lack = context.lackLength(1, target = carrier.payloadLengthLack() + context.position());
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

    @Override
    public IProtocol encode(C context, IProtocol output)
    {
        QttFrame toEncode = (QttFrame) output;
        ByteBuffer toWrite = ByteBuffer.allocate(toEncode.dataLength());
        toWrite.position(toEncode.encodec(toWrite.array(), toWrite.position()))
               .flip();
        return new AioPacket(toWrite);
    }

    @Override
    public IProtocol decode(C context, IProtocol input)
    {
        QttFrame frame = context.getCarrier();
        context.finish();
        return frame;
    }
}
