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

import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngineResult;

import com.isahl.chess.bishop.io.zprotocol.control.X105_SslHandShake;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.async.AioFilterChain;
import com.isahl.chess.queen.io.core.async.AioPacket;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IPacket;
import com.isahl.chess.queen.io.core.inf.IProtocol;

/**
 * @author William.d.zk
 */
public class SslHandShakeFilter<A extends IPContext>
        extends
        AioFilterChain<SSLZContext<A>,
                       IControl,
                       IPacket>
{

    public SslHandShakeFilter()
    {
        super("z-ssl-handshake");
    }

    @Override
    public IPacket encode(SSLZContext<A> context, IControl output)
    {

        AioPacket toSend = new AioPacket(ByteBuffer.wrap(output.encode()));
        SSLEngineResult.HandshakeStatus handshakeStatus = context.doWrap(toSend);
        if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) { return toSend; }
        return null;
    }

    @Override
    public ResultType seek(SSLZContext<A> context, IControl output)
    {
        SSLEngineResult.HandshakeStatus handshakeStatus = context.getHandShakeStatus();
        for (;;) {
            switch (handshakeStatus)
            {
                case NOT_HANDSHAKING ->
                    {
                        return ResultType.IGNORE;
                    }
                case FINISHED ->
                    {
                        context.updateOut();
                        return ResultType.IGNORE;
                    }
                case NEED_TASK -> handshakeStatus = context.doTask();
                case NEED_WRAP ->
                    {
                        return ResultType.HANDLED;
                    }
                case NEED_UNWRAP, NEED_UNWRAP_AGAIN ->
                    {
                        return ResultType.NEED_DATA;
                    }
            }
        }
    }

    @Override
    public IControl decode(SSLZContext<A> context, IPacket input)
    {
        byte[] hello = new byte[input.getBuffer()
                                     .remaining()];
        input.getBuffer()
             .get(hello);
        X105_SslHandShake x105 = new X105_SslHandShake(hello);
        x105.setHandshakeStatus(context.getHandShakeStatus());
        return x105;
    }

    @Override
    public ResultType peek(SSLZContext<A> context, IPacket input)
    {
        if (input.getBuffer()
                 .hasRemaining())
        {
            SSLEngineResult.HandshakeStatus handshakeStatus = context.getHandShakeStatus();
            for (int c = 0;; c++) {
                switch (handshakeStatus)
                {
                    case NOT_HANDSHAKING ->
                        {
                            return ResultType.IGNORE;
                        }
                    case FINISHED ->
                        {
                            context.finish();
                            context.updateIn();
                            return ResultType.IGNORE;
                        }
                    case NEED_WRAP ->
                        {
                            return ResultType.HANDLED;
                        }
                    case NEED_TASK -> handshakeStatus = context.doTask();
                    case NEED_UNWRAP, NEED_UNWRAP_AGAIN ->
                        {
                            if (c > 0) {
                                return ResultType.NEED_DATA;
                            }
                            else {
                                handshakeStatus = context.doUnwrap(input);
                            }
                        }
                }
            }
        }
        else {
            return ResultType.NEED_DATA;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol> Pair<ResultType,
                                      IPContext> pipeSeek(IPContext context, O output)
    {
        if (checkType(output, IProtocol.CONTROL_SERIAL)
            && context.isProxy()
            && context instanceof SSLZContext
            && context.isOutFrame())
        {
            return new Pair<>(seek((SSLZContext<A>) context, (IControl) output), context);
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends IProtocol> Pair<ResultType,
                                      IPContext> pipePeek(IPContext context, I input)
    {
        if (checkType(input, IProtocol.PACKET_SERIAL)
            && context.isProxy()
            && context instanceof SSLZContext
            && context.isInFrame())
        {
            return new Pair<>(peek((SSLZContext<A>) context, (IPacket) input), context);
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol,
            I extends IProtocol> I pipeEncode(IPContext context, O output)
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
    public <O extends IProtocol,
            I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((SSLZContext<A>) context, (IPacket) input);
    }
}