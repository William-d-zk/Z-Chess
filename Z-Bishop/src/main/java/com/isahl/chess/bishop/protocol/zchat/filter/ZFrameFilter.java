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
package com.isahl.chess.bishop.protocol.zchat.filter;

import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.bishop.protocol.zchat.model.base.ZFrame;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IProxyContext;
import com.isahl.chess.queen.io.core.net.socket.AioFilterChain;
import com.isahl.chess.queen.io.core.net.socket.AioPacket;

import java.util.Objects;

import static com.isahl.chess.queen.io.core.features.model.pipe.IFilter.ResultType.*;

/**
 * @author William.d.zk
 */
public class ZFrameFilter
        extends AioFilterChain<ZContext, ZFrame, IPacket>
{
    public final static String NAME = "zchat_frame";

    public ZFrameFilter()
    {
        super(NAME);
    }

    @Override
    public IPacket encode(ZContext context, ZFrame frame)
    {
        return new AioPacket(Objects.requireNonNull(frame.encode()));
    }

    @Override
    public ZFrame decode(ZContext context, IPacket input)
    {
        ZFrame frame = context.getCarrier();
        frame.decode(context.getRvBuffer());
        context.reset();
        return frame;
    }

    @Override
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.PROTOCOL_BISHOP_FRAME_SERIAL)) {
            if(context.isOutFrame() && context instanceof ZContext) {
                return new Pair<>(ResultType.NEXT_STEP, context);
            }
            IPContext acting = context;
            while(acting.isProxy()) {
                acting = ((IProxyContext<?>) acting).getActingContext();
                if(acting.isOutFrame() && acting instanceof ZContext) {
                    return new Pair<>(NEXT_STEP, acting);
                }
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    private ResultType peek(IPContext context, IProtocol input)
    {
        if(context.isInFrame() && context instanceof ZContext z_ctx && input instanceof IPacket in_packet) {
            ByteBuf netBuf = in_packet.getBuffer();
            ByteBuf ctxBuf = z_ctx.getRvBuffer();
            ZFrame carrier = z_ctx.getCarrier();
            if(carrier == null) {
                z_ctx.setCarrier(carrier = new ZFrame());
            }
            ctxBuf.putExactly(netBuf);
            return carrier.lack(ctxBuf) > 0 ? NEED_DATA : NEXT_STEP;
        }
        return ResultType.IGNORE;
    }

    @Override
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.IO_QUEEN_PACKET_SERIAL)) {
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
        return (I) encode((ZContext) context, (ZFrame) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((ZContext) context, (IPacket) input);
    }
}
