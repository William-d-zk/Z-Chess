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
package com.isahl.chess.bishop.protocol.ws.filter;

import com.isahl.chess.bishop.protocol.ws.features.IWsContext;
import com.isahl.chess.bishop.protocol.ws.model.WsFrame;
import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IProxyContext;
import com.isahl.chess.queen.io.core.net.socket.AioFilterChain;
import com.isahl.chess.queen.io.core.net.socket.AioPacket;

import java.nio.ByteBuffer;

/**
 * @author William.d.zk
 */
public class WsFrameFilter<T extends ZContext & IWsContext>
        extends AioFilterChain<T, WsFrame, IPacket>
{
    public final static String NAME = "ws_frame";

    public WsFrameFilter()
    {
        super(NAME);
    }

    @Override
    public IPacket encode(T context, WsFrame frame)
    {
        frame.setMask(context.getMask());
        return new AioPacket(frame.encode()
                                  .flip());
    }

    @Override
    public ResultType peek(T context, IPacket input)
    {
        ByteBuffer recvBuf = input.getBuffer();
        ByteBuffer cRvBuf = context.getRvBuffer();
        WsFrame carrier = context.getCarrier();
        int lack = context.lack();
        switch(context.position()) {
            case -1:
                if(lack > 0 && !recvBuf.hasRemaining()) {return ResultType.NEED_DATA;}
                context.setCarrier(carrier = new WsFrame());
                byte value = recvBuf.get();
                carrier.put(WsFrame.getOpCode(value));
                carrier.frame_fin = WsFrame.isFrameFin(value);
                lack = context.lackLength(1, 1);
            case 0:
                if(lack > 0 && !recvBuf.hasRemaining()) {return ResultType.NEED_DATA;}
                carrier.setMaskCode(recvBuf.get());
                cRvBuf.put(carrier.getLengthCode());
                lack = context.lackLength(1, carrier.lack(context.position()) + context.position());
            case 1:
            default:
                if(lack > 0 && !recvBuf.hasRemaining()) {return ResultType.NEED_DATA;}
                int target = context.position() + lack;
                do {
                    int remain = recvBuf.remaining();
                    int length = Math.min(remain, lack);
                    for(int i = 0; i < length; i++) {
                        cRvBuf.put(recvBuf.get());
                    }
                    lack = context.lackLength(length, target);
                    if(lack > 0 && !recvBuf.hasRemaining()) {return ResultType.NEED_DATA;}
                    cRvBuf.flip();
                    // decoding
                    if(carrier.getPayloadLength() < 0) {
                        switch(target) {
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
                                cRvBuf.position(3);
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
                                cRvBuf.position(9);
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
                        if(carrier.getPayloadLength() > context.getMaxPayloadSize()) {
                            _Logger.warning("payload is too large");
                            return ResultType.ERROR;
                        }
                        else if(carrier.getPayloadLength() == 0) {return ResultType.NEXT_STEP;}
                    }
                    else {
                        if(carrier.getPayloadLength() > 0) {
                            byte[] payload = new byte[(int) carrier.getPayloadLength()];
                            cRvBuf.get(payload);
                            carrier.put(payload);
                        }
                        cRvBuf.clear();
                        return ResultType.NEXT_STEP;
                    }
                }
                while(recvBuf.hasRemaining());
                return ResultType.NEED_DATA;
        }
    }

    @Override
    public WsFrame decode(T context, IPacket input)
    {
        WsFrame frame = context.getCarrier();
        frame.doMask();
        context.finish();
        return frame;
    }

    @Override
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.PROTOCOL_BISHOP_FRAME_SERIAL)) {
            if(context instanceof IWsContext && context.isOutConvert()) {
                return new Pair<>(ResultType.NEXT_STEP, context);
            }
            IPContext acting = context;
            while(acting.isProxy() || acting instanceof IWsContext) {
                if(acting instanceof IWsContext && acting.isOutConvert()) {
                    return new Pair<>(ResultType.NEXT_STEP, acting);
                }
                else if(acting.isProxy()) {
                    acting = ((IProxyContext<?>) acting).getActingContext();
                }
                else {break;}
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.IO_QUEEN_PACKET_SERIAL)) {
            if(context instanceof IWsContext && context.isInConvert()) {
                return new Pair<>(peek((T) context, (IPacket) input), context);
            }
            IPContext acting = context;
            while(acting.isProxy() || acting instanceof IWsContext) {
                if(acting instanceof IWsContext && acting.isInConvert()) {
                    return new Pair<>(peek((T) acting, (IPacket) input), acting);
                }
                else if(acting.isProxy()) {
                    acting = ((IProxyContext<?>) acting).getActingContext();
                }
                else {break;}
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> I pipeEncode(IPContext context, O output)
    {
        return (I) encode((T) context, (WsFrame) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((T) context, (IPacket) input);
    }
}
