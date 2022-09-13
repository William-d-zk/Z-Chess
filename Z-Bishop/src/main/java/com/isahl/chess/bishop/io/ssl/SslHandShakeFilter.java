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

package com.isahl.chess.bishop.io.ssl;

import com.isahl.chess.bishop.protocol.zchat.model.ctrl.X07_SslHandShake;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.net.socket.AioFilterChain;
import com.isahl.chess.queen.io.core.net.socket.AioPacket;

import javax.net.ssl.SSLEngineResult;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

/**
 * @author William.d.zk
 */
public class SslHandShakeFilter<A extends IPContext>
        extends AioFilterChain<SSLZContext<A>, IProtocol, IPacket>
{
    public SslHandShakeFilter()
    {
        super("z-ssl-handshake");
    }

    @Override
    public IPacket encode(SSLZContext<A> context, IProtocol output)
    {
        SSLEngineResult.HandshakeStatus handshakeStatus;
        ByteBuf netOutBuffer = null;
        do {
            if(netOutBuffer == null) {
                netOutBuffer = context.doWrap(output.encode());
            }
            else {
                netOutBuffer.append(context.doWrap(output.encode()));
            }
            handshakeStatus = context.getHandShakeStatus();
        }
        while(handshakeStatus == NEED_WRAP);
        return new AioPacket(netOutBuffer);
    }

    @Override
    public IControl<A> decode(SSLZContext<A> context, IPacket input)
    {
        IPacket unwrapped = context.getCarrier();
        X07_SslHandShake x07 = new X07_SslHandShake();
        if(unwrapped != null) {
            //FIXME
            x07.decode(unwrapped.getBuffer(), null);
        }
        else {
            x07 = new X07_SslHandShake();
        }
        SSLEngineResult.HandshakeStatus handshakeStatus = context.getHandShakeStatus();
        x07.setHandshakeStatus(handshakeStatus);
        context.reset();
        return null;
    }

    @Override
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.PROTOCOL_BISHOP_CONTROL_SERIAL) && context.isProxy() &&
           context instanceof SSLZContext ssl_ctx && context.isOutFrame())
        {
            SSLEngineResult.HandshakeStatus handshakeStatus = ssl_ctx.getHandShakeStatus();
            boolean loop = false;
            do {
                switch(handshakeStatus) {
                    case NOT_HANDSHAKING, FINISHED -> {
                        loop = false;
                        context.promotionOut();
                        _Logger.info("SSL ready to write");
                    }
                    case NEED_TASK -> {
                        handshakeStatus = ssl_ctx.doTask();
                        loop = true;
                    }
                    case NEED_WRAP -> {
                        return new Pair<>(ResultType.HANDLED, ssl_ctx);
                    }
                    case NEED_UNWRAP, NEED_UNWRAP_AGAIN -> {
                        return new Pair<>(ResultType.NEED_DATA, ssl_ctx);
                    }
                }
            }
            while(loop);
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.IO_QUEEN_PACKET_SERIAL) && context.isProxy() &&
           context instanceof SSLZContext ssl_ctx && context.isInFrame() && input instanceof IPacket in_packet)
        {
            SSLEngineResult.HandshakeStatus handshakeStatus = ssl_ctx.getHandShakeStatus();
            ByteBuf netInBuffer = in_packet.getBuffer();
            ByteBuf appInBuffer = null;
            boolean loop = netInBuffer.isReadable();
            while(loop) {
                switch(handshakeStatus) {
                    case NOT_HANDSHAKING, FINISHED -> {
                        context.promotionIn();
                        _Logger.info("SSL ready to read");
                        return new Pair<>(ResultType.IGNORE, ssl_ctx);
                    }
                    case NEED_WRAP -> {
                        if(appInBuffer != null) {
                            ssl_ctx.setCarrier(new AioPacket(appInBuffer));
                        }
                        return new Pair<>(ResultType.HANDLED, ssl_ctx);
                    }
                    case NEED_TASK -> handshakeStatus = ssl_ctx.doTask();
                    case NEED_UNWRAP, NEED_UNWRAP_AGAIN -> {
                        appInBuffer = ssl_ctx.doUnwrap(netInBuffer);
                        handshakeStatus = ssl_ctx.getHandShakeStatus();
                        loop = handshakeStatus != NEED_UNWRAP && handshakeStatus != NEED_UNWRAP_AGAIN ||
                               netInBuffer.isReadable();
                    }
                }
            }
            return new Pair<>(ResultType.NEED_DATA, context);
        }
        return new Pair<>(ResultType.IGNORE, context);
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
        return (I) encode((SSLZContext<A>) context, output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((SSLZContext<A>) context, (IPacket) input);
    }
}