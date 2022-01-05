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
import com.isahl.chess.bishop.protocol.ws.features.IWsContext;
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
public class WsHandShakeFilter<T extends WsContext>
        extends AioFilterChain<T, X101_HandShake<T>, IPacket>
{
    public WsHandShakeFilter()
    {
        super("ws_header");
    }

    @Override
    public IPacket encode(T context, X101_HandShake<T> output)
    {
        if(output.isClientOk() || output.isServerAccept()) {
            context.promotionOut();
        }
        return new AioPacket(output.encode());
    }

    @Override
    public X101_HandShake<T> decode(T context, IPacket input)
    {
        X101_HandShake<T> handshake = context.getHandshake();
        handshake.decode(context.getRvBuffer(), context);
        if(handshake.isClientOk() || handshake.isServerAccept()) {
            context.promotionIn();
        }
        return handshake;
    }

    @Override
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.PROTOCOL_BISHOP_CONTROL_SERIAL) && output instanceof X101_HandShake) {
            IPContext acting = context;
            do {
                if(acting.isOutInit() && acting instanceof IWsContext) {
                    return Pair.of(ResultType.NEXT_STEP, acting);
                }
                else if(acting.isProxy()) {
                    acting = ((IProxyContext<?>) acting).getActingContext();
                }
                else {
                    acting = null;
                }
            }
            while(acting != null);
        }
        return Pair.of(IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.IO_QUEEN_PACKET_SERIAL) && input instanceof IPacket in_packet) {
            IPContext acting = context;
            do {
                if(acting.isInInit() && acting instanceof IWsContext) {
                    T ws_ctx = (T) acting;
                    X101_HandShake<T> handshake = ws_ctx.getHandshake();
                    if(handshake == null) {
                        ws_ctx.handshake(handshake = new X101_HandShake<>());
                        handshake.wrap(ws_ctx);
                    }
                    return Pair.of(handshake.lack(in_packet.getBuffer()) > 0 ? NEED_DATA : HANDLED, acting);
                }
                else if(acting.isProxy()) {
                    acting = ((IProxyContext<?>) acting).getActingContext();
                }
                else {
                    acting = null;
                }
            }
            while(acting != null);
        }
        return Pair.of(IGNORE, context);
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
        return (I) encode((T) context, (X101_HandShake<T>) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((T) context, (IPacket) input);
    }
}