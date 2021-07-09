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

package com.isahl.chess.bishop.io.ssl;

import com.isahl.chess.bishop.io.ws.zchat.zprotocol.control.X105_SslHandShake;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.async.AioFilterChain;
import com.isahl.chess.queen.io.core.async.AioPacket;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IPacket;
import com.isahl.chess.queen.io.core.inf.IProtocol;

import javax.net.ssl.SSLEngineResult;
import java.nio.ByteBuffer;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

/**
 * @author William.d.zk
 */
public class SslHandShakeFilter<A extends IPContext>
        extends AioFilterChain<SSLZContext<A>, IControl, IPacket>
{
    public SslHandShakeFilter()
    {
        super("z-ssl-handshake");
    }

    @Override
    public IPacket encode(SSLZContext<A> context, IControl output)
    {
        SSLEngineResult.HandshakeStatus handshakeStatus;
        ByteBuffer netOutBuffer = null;
        do {
            if(netOutBuffer == null) {
                netOutBuffer = context.doWrap(ByteBuffer.wrap(output.encode()));
            }
            else {
                netOutBuffer = IoUtil.appendBuffer(netOutBuffer, context.doWrap(ByteBuffer.wrap(output.encode())));
            }
            handshakeStatus = context.getHandShakeStatus();
        }
        while(handshakeStatus == NEED_WRAP);
        return new AioPacket(netOutBuffer.flip());
    }

    @Override
    public ResultType seek(SSLZContext<A> context, IControl output)
    {
        SSLEngineResult.HandshakeStatus handshakeStatus = context.getHandShakeStatus();
        boolean loop = false;
        do {
            switch(handshakeStatus) {
                case NOT_HANDSHAKING, FINISHED -> {
                    loop = false;
                    context.updateOut();
                    _Logger.info("SSL ready to write");
                }
                case NEED_TASK -> {
                    handshakeStatus = context.doTask();
                    loop = true;
                }
                case NEED_WRAP -> {
                    return ResultType.HANDLED;
                }
                case NEED_UNWRAP, NEED_UNWRAP_AGAIN -> {
                    return ResultType.NEED_DATA;
                }
            }
        }
        while(loop);
        return ResultType.IGNORE;
    }

    @Override
    public IControl decode(SSLZContext<A> context, IPacket input)
    {
        IPacket unwrapped = context.getCarrier();

        X105_SslHandShake x105;
        if(unwrapped != null && unwrapped.getBuffer()
                                         .hasRemaining())
        {
            byte[] hello = new byte[unwrapped.getBuffer()
                                             .remaining()];
            unwrapped.getBuffer()
                     .get(hello);
            x105 = new X105_SslHandShake(hello);
        }
        else {
            x105 = new X105_SslHandShake(null);
        }
        SSLEngineResult.HandshakeStatus handshakeStatus = context.getHandShakeStatus();
        x105.setHandshakeStatus(handshakeStatus);
        context.finish();
        return x105;
    }

    @Override
    public ResultType peek(SSLZContext<A> context, IPacket input)
    {
        SSLEngineResult.HandshakeStatus handshakeStatus = context.getHandShakeStatus();
        ByteBuffer netInBuffer = input.getBuffer();
        ByteBuffer localBuffer = context.getRvBuffer();
        ByteBuffer appInBuffer = null;
        if(localBuffer.position() > 0) {
            IoUtil.write(netInBuffer, localBuffer);
            localBuffer.flip();
            netInBuffer = localBuffer;
        }
        boolean loop = netInBuffer.hasRemaining();
        while(loop) {
            switch(handshakeStatus) {
                case NOT_HANDSHAKING, FINISHED -> {
                    context.updateIn();
                    _Logger.info("SSL ready to read");
                    return ResultType.IGNORE;
                }
                case NEED_WRAP -> {
                    if(appInBuffer != null) {
                        context.setCarrier(new AioPacket(appInBuffer.flip()));
                    }
                    return ResultType.HANDLED;
                }
                case NEED_TASK -> handshakeStatus = context.doTask();
                case NEED_UNWRAP, NEED_UNWRAP_AGAIN -> {
                    appInBuffer = context.doUnwrap(netInBuffer);
                    handshakeStatus = context.getHandShakeStatus();
                    loop = handshakeStatus != NEED_UNWRAP && handshakeStatus != NEED_UNWRAP_AGAIN ||
                           netInBuffer.hasRemaining();
                }
            }
        }
        return ResultType.NEED_DATA;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.CONTROL_SERIAL) && context.isProxy() && context instanceof SSLZContext &&
           context.isOutFrame())
        {
            return new Pair<>(seek((SSLZContext<A>) context, (IControl) output), context);
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.PACKET_SERIAL) && context.isProxy() && context instanceof SSLZContext &&
           context.isInFrame())
        {
            return new Pair<>(peek((SSLZContext<A>) context, (IPacket) input), context);
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
        return (I) encode((SSLZContext<A>) context, (X105_SslHandShake) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((SSLZContext<A>) context, (IPacket) input);
    }
}