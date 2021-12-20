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

import com.isahl.chess.bishop.protocol.ws.WsContext;
import com.isahl.chess.bishop.protocol.ws.features.IWsContext;
import com.isahl.chess.bishop.protocol.ws.model.WsFrame;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IProxyContext;
import com.isahl.chess.queen.io.core.net.socket.AioFilterChain;
import com.isahl.chess.queen.io.core.net.socket.AioPacket;

import static com.isahl.chess.queen.io.core.features.model.pipe.IFilter.ResultType.*;

/**
 * @author William.d.zk
 */
public class WsFrameFilter<T extends WsContext>
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
        IPacket packet = new AioPacket(frame.encode());
        context.demotionOut();
        return packet;
    }

    public ResultType peek(IPContext context, IProtocol input)
    {
        if(context instanceof IWsContext && input instanceof IPacket in_packet) {
            WsContext ws_ctx = (WsContext) context;
            WsFrame carrier = ws_ctx.getCarrier();
            if(carrier == null) {
                ws_ctx.setCarrier(carrier = new WsFrame());
            }
            return carrier.lack(in_packet.getBuffer()) > 0 ? NEED_DATA : NEXT_STEP;
        }
        return ResultType.IGNORE;
    }

    @Override
    public WsFrame decode(T context, IPacket input)
    {
        WsFrame frame = context.getCarrier();
        frame.decode(input.getBuffer());
        context.reset();
        context.promotionIn();
        return frame;
    }

    @Override
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.PROTOCOL_BISHOP_FRAME_SERIAL) && context.isOutFrame()) {
            if(context.isOutFrame() && context instanceof IWsContext) {
                return new Pair<>(ResultType.NEXT_STEP, context);
            }
            IPContext acting = context;
            while(acting.isProxy()) {
                acting = ((IProxyContext<?>) acting).getActingContext();
                if(acting.isOutFrame() && acting instanceof WsContext) {
                    return new Pair<>(ResultType.NEXT_STEP, acting);
                }
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.IO_QUEEN_PACKET_SERIAL) && context.isInFrame()) {
            ResultType check = peek(context, input);
            if(check != IGNORE) {return new Pair<>(check, context);}
            IPContext acting = context;
            while(acting.isProxy()) {
                acting = ((IProxyContext<?>) acting).getActingContext();
                check = peek(acting, input);
                if(check == NEXT_STEP || check == NEED_DATA) {
                    return new Pair<>(check, acting);
                }
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
