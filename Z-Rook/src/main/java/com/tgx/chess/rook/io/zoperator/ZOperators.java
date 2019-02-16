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

package com.tgx.chess.rook.io.zoperator;

import java.util.Objects;

import com.tgx.chess.bishop.io.ws.bean.WsContext;
import com.tgx.chess.bishop.io.ws.filter.WsControlFilter;
import com.tgx.chess.bishop.io.ws.filter.WsFrameFilter;
import com.tgx.chess.bishop.io.ws.filter.WsHandShakeFilter;
import com.tgx.chess.bishop.io.zfilter.ZCommandFilter;
import com.tgx.chess.bishop.io.zfilter.ZTlsFilter;
import com.tgx.chess.bishop.io.zoperator.ZCommandFactories;
import com.tgx.chess.bishop.io.zprotocol.ZContext;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.event.inf.AbstractTransfer;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.IFilterChain;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.IoHandler;

@SuppressWarnings("unchecked")
public enum ZOperators
        implements
        IoHandler
{
    CONSUMER,
    CONSUMER_SSL;

    @Override
    public IOperator<IPacket,
                     ISession> getInOperator()
    {
        return CONSUMER_DECODER();
    }

    @Override
    public IOperator<ICommand[],
                     ISession> getOutOperator()
    {
        return CONSUMER_TRANSFER();
    }

    @Override
    public Mode getMode()
    {
        return Mode.LINK;
    }

    public Type getType()
    {
        return Type.CONSUMER;
    }

    private static IOperator<ICommand,
                             ISession>             consumer_encoder;
    private static IOperator<ICommand[],
                             ISession>             consumer_transfer;
    private static IOperator<IPacket,
                             ISession>             consumer_decoder;

    public static IOperator<ICommand[],
                            ISession> CONSUMER_TRANSFER()
    {
        return consumer_transfer;
    }

    private static IOperator<IPacket,
                             ISession> CONSUMER_DECODER()
    {
        return consumer_decoder;
    }

    static {
        consumer_encoder = new IEncoder()
        {
            final WsHandShakeFilter handshakeFilter = new WsHandShakeFilter(CONSUMER);

            {
                IFilterChain<WsContext> header = new ZTlsFilter();
                handshakeFilter.linkAfter(header);
                handshakeFilter.linkFront(new WsFrameFilter())
                               .linkFront(new ZCommandFilter(ZCommandFactories.CONSUMER))
                               .linkFront(new WsControlFilter());
            }

            @Override
            public Triple<Throwable,
                          ISession,
                          IOperator<Throwable,
                                    ISession>> handle(ICommand command, ISession session)
            {
                try {
                    IPacket send = (IPacket) filterWrite(command, handshakeFilter, session.getContext());
                    Objects.requireNonNull(send);
                    LOG.info("consumer send:%s",
                             IoUtil.bin2Hex(send.getBuffer()
                                                .array(),
                                            "."));
                    session.write(send, aio_writer);
                }
                catch (Exception e) {
                    return new Triple<>(e, session, error_operator);
                }
                return null;
            }

            @Override
            public String toString()
            {
                return "consumer_encoder";
            }
        };
        consumer_decoder = new IDecoder()
        {
            final WsHandShakeFilter handshakeFilter = new WsHandShakeFilter(ZOperators.CONSUMER);

            {
                IFilterChain<WsContext> header = new ZTlsFilter();
                handshakeFilter.linkAfter(header);
                handshakeFilter.linkFront(new WsFrameFilter())
                               .linkFront(new ZCommandFilter(ZCommandFactories.CONSUMER))
                               .linkFront(new WsControlFilter());
            }

            @Override
            public String toString()
            {
                return "consumer_decoder";
            }

            @Override
            public Triple<ICommand[],
                          ISession,
                          IOperator<ICommand[],
                                    ISession>> handle(IPacket inPackage, ISession session)
            {
                return new Triple<>(filterRead(inPackage, handshakeFilter, session), session, consumer_transfer);
            }
        };
        consumer_transfer = new AbstractTransfer(consumer_encoder)
        {

            @Override
            public String toString()
            {
                return "consumer_transfer";
            }
        };
    }

}
