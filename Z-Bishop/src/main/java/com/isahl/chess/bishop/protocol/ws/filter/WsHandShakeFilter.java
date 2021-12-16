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
import com.isahl.chess.bishop.protocol.ws.ctrl.X101_HandShake;
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
public class WsHandShakeFilter
        extends AioFilterChain<WsContext, X101_HandShake, IPacket>
{
    private final static String CRLF      = "\r\n";
    private final static String CRLF_CRLF = CRLF + CRLF;

    public WsHandShakeFilter()
    {
        super("ws_header");
    }

    @Override
    public IPacket encode(WsContext context, X101_HandShake output)
    {
        if(output.isClientOk() || output.isServerAccept()) {
            context.updateOut();
        }
        return new AioPacket(output.encode());
    }

    private ResultType peek(IPContext context, IProtocol input)
    {
        if(context.isInInit() && context instanceof WsContext ws_ctx && input instanceof IPacket in_packet) {
            X101_HandShake handshake = ws_ctx.getHandshake();
            if(handshake == null) {
                ws_ctx.setHandshake(handshake = new X101_HandShake());
                handshake.wrap(ws_ctx);
            }
            return handshake.lack(in_packet.getBuffer()) > 0 ? NEED_DATA : NEXT_STEP;
        }
        return IGNORE;

    }

    @Override
    public X101_HandShake decode(WsContext context, IPacket input)
    {
        X101_HandShake handshake = context.getHandshake();
        handshake.decode(context.getRvBuffer(), context);
        context.reset();
        if(handshake.isClientOk() || handshake.isServerAccept()) {
            context.updateIn();
        }
        return handshake;
    }

    @Override
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.PROTOCOL_BISHOP_CONTROL_SERIAL)) {
            if(context.isOutInit() && context instanceof WsContext) {
                return new Pair<>(ResultType.NEXT_STEP, context);
            }
            IPContext acting = context;
            while(acting.isProxy()) {
                acting = ((IProxyContext<?>) acting).getActingContext();
                if(acting.isOutInit() && acting instanceof WsContext) {return new Pair<>(ResultType.NEXT_STEP, acting);}
            }
        }
        return new Pair<>(IGNORE, context);
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
        return new Pair<>(IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> I pipeEncode(IPContext context, O output)
    {
        /*
         * SSL->WS->MQTT
         * WS->MQTT
         * 代理结构时，需要区分 context 是否为IWsContext
         */
        return (I) encode((WsContext) context, (X101_HandShake) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((WsContext) context, (IPacket) input);
    }
}