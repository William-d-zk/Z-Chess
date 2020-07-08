/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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
package com.tgx.chess.bishop.io.ws.filter;

import java.nio.ByteBuffer;

import com.tgx.chess.bishop.io.ws.WsContext;
import com.tgx.chess.bishop.io.ws.WsFrame;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.queen.io.core.async.AioFilterChain;
import com.tgx.chess.queen.io.core.async.AioPacket;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author William.d.zk
 */
public class WsFrameFilter
        extends
        AioFilterChain<ZContext,
                       WsFrame,
                       IPacket>
{

    public WsFrameFilter()
    {
        super("ws_frame");
    }

    @Override
    public boolean checkType(IProtocol protocol)
    {
        return checkType(protocol, IPacket.PACKET_SERIAL);
    }

    @Override
    public ResultType preEncode(ZContext context, IProtocol output)
    {
        return preFrameEncode(context, output);
    }

    @Override
    public IPacket encode(ZContext context, WsFrame output)
    {
        output.setMask(null);
        ByteBuffer toWrite = ByteBuffer.allocate(output.dataLength());
        toWrite.position(output.encodec(toWrite.array(), toWrite.position()))
               .flip();
        return new AioPacket(toWrite);
    }

    @Override
    public ResultType preDecode(ZContext context, IPacket input)
    {
        ResultType result = preFrameDecode(context, input);
        if (ResultType.NEXT_STEP.equals(result)) {
            ByteBuffer recvBuf = input.getBuffer();
            ByteBuffer cRvBuf = context.getRvBuffer();
            WsFrame carrier = context.getCarrier();
            int lack = context.lack();
            switch (context.position())
            {
                case -1:
                    if (lack > 0 && !recvBuf.hasRemaining()) { return ResultType.NEED_DATA; }
                    context.setCarrier(carrier = new WsFrame());
                    byte value = recvBuf.get();
                    carrier.setCtrl(WsFrame.getOpCode(value));
                    carrier.frame_fin = WsFrame.isFrameFin(value);
                    lack = context.lackLength(1, 1);
                case 0:
                    if (lack > 0 && !recvBuf.hasRemaining()) { return ResultType.NEED_DATA; }
                    carrier.setMaskCode(recvBuf.get());
                    cRvBuf.put(carrier.getLengthCode());
                    lack = context.lackLength(1, carrier.payloadLengthLack(context.position()) + context.position());
                case 1:
                default:
                    if (lack > 0 && !recvBuf.hasRemaining()) { return ResultType.NEED_DATA; }
                    int target = context.position() + lack;
                    do {
                        int remain = recvBuf.remaining();
                        int length = Math.min(remain, lack);
                        for (int i = 0; i < length; i++) {
                            cRvBuf.put(recvBuf.get());
                        }
                        lack = context.lackLength(length, target);
                        if (lack > 0 && !recvBuf.hasRemaining()) { return ResultType.NEED_DATA; }
                        cRvBuf.flip();
                        // decoding
                        if (carrier.getPayloadLength() < 0) {
                            switch (target)
                            {
                                case WsFrame.frame_payload_length_7_no_mask_position:
                                    carrier.setPayloadLength(cRvBuf.get());
                                    carrier.setMask(null);
                                    break;
                                case WsFrame.frame_payload_length_16_no_mask_position:
                                    carrier.setPayloadLength(cRvBuf.getShort(1) & 0xFFFF);
                                    carrier.setMask(null);
                                    break;
                                case WsFrame.frame_payload_length_7_mask_position:
                                    carrier.setPayloadLength(cRvBuf.get());
                                    byte[] mask = new byte[4];
                                    cRvBuf.get(mask);
                                    carrier.setMask(mask);
                                    break;
                                case WsFrame.frame_payload_length_16_mask_position:
                                    carrier.setPayloadLength(cRvBuf.getShort(1) & 0xFFFF);
                                    mask = new byte[4];
                                    cRvBuf.get(mask);
                                    carrier.setMask(mask);
                                    break;
                                case WsFrame.frame_payload_length_63_no_mask_position:
                                    carrier.setPayloadLength(cRvBuf.getLong(1));
                                    carrier.setMask(null);
                                    break;
                                case WsFrame.frame_payload_length_63_mask_position:
                                    carrier.setPayloadLength(cRvBuf.getLong(1));
                                    mask = new byte[4];
                                    cRvBuf.get(mask);
                                    carrier.setMask(mask);
                                    break;
                                default:
                                    break;
                            }
                            target = context.position() + (int) carrier.getPayloadLength();
                            lack = context.lackLength(0, target);
                            cRvBuf.clear();
                            if (carrier.getPayloadLength() > ((WsContext) context).getMaxPayloadSize()) {
                                _Logger.warning("payload is too large");
                                return ResultType.ERROR;
                            }
                            else if (carrier.getPayloadLength() == 0) { return ResultType.NEXT_STEP; }
                        }
                        else {
                            if (carrier.getPayloadLength() > 0) {
                                byte[] payload = new byte[(int) carrier.getPayloadLength()];
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
        return result;
    }

    @Override
    public WsFrame decode(ZContext context, IPacket input)
    {
        WsFrame frame = context.getCarrier();
        context.finish();
        return frame;
    }

}
