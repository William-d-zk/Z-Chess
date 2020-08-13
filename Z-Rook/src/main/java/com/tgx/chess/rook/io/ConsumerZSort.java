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

package com.tgx.chess.rook.io;

import com.tgx.chess.bishop.io.mqtt.QttContext;
import com.tgx.chess.bishop.io.mqtt.QttFrame;
import com.tgx.chess.bishop.io.mqtt.filter.QttCommandFactory;
import com.tgx.chess.bishop.io.mqtt.filter.QttCommandFilter;
import com.tgx.chess.bishop.io.mqtt.filter.QttControlFilter;
import com.tgx.chess.bishop.io.mqtt.filter.QttFrameFilter;
import com.tgx.chess.bishop.io.ws.WsContext;
import com.tgx.chess.bishop.io.ws.WsFrame;
import com.tgx.chess.bishop.io.ws.filter.WsControlFilter;
import com.tgx.chess.bishop.io.ws.filter.WsFrameFilter;
import com.tgx.chess.bishop.io.ws.filter.WsHandShakeFilter;
import com.tgx.chess.bishop.io.zfilter.ZCommandFilter;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zfilter.ZTLSFilter;
import com.tgx.chess.bishop.io.zprotocol.ZCommand;
import com.tgx.chess.bishop.io.zprotocol.ZConsumerFactory;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.operator.AioWriter;
import com.tgx.chess.queen.event.operator.CloseOperator;
import com.tgx.chess.queen.event.operator.ErrorOperator;
import com.tgx.chess.queen.event.operator.IgnoreOperator;
import com.tgx.chess.queen.event.operator.PipeDecoder;
import com.tgx.chess.queen.event.operator.PipeEncoder;
import com.tgx.chess.queen.event.operator.TransferOperator;
import com.tgx.chess.queen.io.core.inf.ICommandFactory;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IFilterChain;
import com.tgx.chess.queen.io.core.inf.IPipeDecoder;
import com.tgx.chess.queen.io.core.inf.IPipeEncoder;
import com.tgx.chess.queen.io.core.inf.IPipeTransfer;
import com.tgx.chess.queen.io.core.inf.ISessionCloser;
import com.tgx.chess.queen.io.core.inf.ISessionError;
import com.tgx.chess.queen.io.core.inf.ISessionOption;

/**
 * @author william.d.zk
 */
@SuppressWarnings("unchecked")
public enum ConsumerZSort
        implements
        ISort<ZContext>
{
    /**
     *
     */

    WS_CONSUMER
    {
        @Override
        public ZContext newContext(ISessionOption option)
        {
            return new WsContext(option, this);
        }

        @Override
        public ICommandFactory<ZCommand,
                               WsFrame> getCommandFactory()
        {
            return _ZConsumerFactory;
        }

        @Override
        public IFilterChain<ZContext> getFilterChain()
        {
            return _HandshakeFilter;
        }

    },
    WS_CONSUMER_SSL
    {
        @Override
        public boolean isSSL()
        {
            return true;
        }

        @Override
        public ZContext newContext(ISessionOption option)
        {
            return new WsContext(option, this);
        }

        @Override
        public IFilterChain<ZContext> getFilterChain()
        {
            return _HandshakeFilter;
        }

        @Override
        public ICommandFactory<ZCommand,
                               WsFrame> getCommandFactory()
        {
            return _ZConsumerFactory;
        }
    },
    QTT_SYMMETRY
    {
        @Override
        public IFilterChain<ZContext> getFilterChain()
        {
            return _QttFrameFilter;
        }

        @Override
        public Type getType()
        {
            return Type.SYMMETRY;
        }

        @Override
        public ZContext newContext(ISessionOption option)
        {
            return new QttContext(option, this);
        }

        @Override
        public ICommandFactory<IControl<ZContext>,
                               QttFrame> getCommandFactory()
        {
            return _QttCommandFactory;
        }
    };

    @Override
    public Mode getMode()
    {
        return Mode.LINK;
    }

    @Override
    public Type getType()
    {
        return Type.CONSUMER;
    }

    private final AioWriter<ZContext>      _AioWriter        = new AioWriter<>();
    private final ISessionCloser<ZContext> _CloseOperator    = new CloseOperator<>();
    private final ISessionError<ZContext>  _ErrorOperator    = new ErrorOperator<>();
    private final IPipeEncoder<ZContext>   _ConsumerEncoder  = new PipeEncoder<>(_AioWriter);
    private final IPipeTransfer<ZContext>  _ConsumerTransfer = new TransferOperator<>();
    private final IPipeDecoder<ZContext>   _ConsumerDecoder  = new PipeDecoder<>();
    private final IgnoreOperator<ZContext> _Ignore           = new IgnoreOperator<>();

    final static ZConsumerFactory  _ZConsumerFactory  = new ZConsumerFactory();
    final static QttCommandFactory _QttCommandFactory = new QttCommandFactory();

    final WsHandShakeFilter _HandshakeFilter = new WsHandShakeFilter();
    {
        IFilterChain<ZContext> header = new ZTLSFilter();
        _HandshakeFilter.linkAfter(header)
                        .linkFront(new WsFrameFilter())
                        .linkFront(new WsControlFilter())
                        .linkFront(new ZCommandFilter(new ZConsumerFactory()));
    }

    final QttFrameFilter _QttFrameFilter = new QttFrameFilter();
    {
        _QttFrameFilter.linkAfter(new ZTLSFilter());
        _QttFrameFilter.linkFront(new QttControlFilter())
                       .linkFront(new QttCommandFilter());
    }

    @Override
    public IPipeEncoder<ZContext> getEncoder()
    {
        return _ConsumerEncoder;
    }

    @Override
    public IPipeDecoder<ZContext> getDecoder()
    {
        return _ConsumerDecoder;
    }

    @Override
    public IPipeTransfer<ZContext> getTransfer()
    {
        return _ConsumerTransfer;
    }

    @Override
    public ISessionCloser<ZContext> getCloser()
    {
        return _CloseOperator;
    }

    @Override
    public ISessionError<ZContext> getError()
    {
        return _ErrorOperator;
    }

    @Override
    public IgnoreOperator<ZContext> getIgnore()
    {
        return _Ignore;
    }

}
