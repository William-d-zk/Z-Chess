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

package com.isahl.chess.bishop.io;

import com.isahl.chess.bishop.io.mqtt.QttContext;
import com.isahl.chess.bishop.io.mqtt.QttFrame;
import com.isahl.chess.bishop.io.mqtt.filter.QttCommandFactory;
import com.isahl.chess.bishop.io.mqtt.filter.QttCommandFilter;
import com.isahl.chess.bishop.io.mqtt.filter.QttControlFilter;
import com.isahl.chess.bishop.io.mqtt.filter.QttFrameFilter;
import com.isahl.chess.bishop.io.ws.WsProxyContext;
import com.isahl.chess.bishop.io.zfilter.ZContext;
import com.isahl.chess.bishop.io.zprotocol.ZClusterFactory;
import com.isahl.chess.bishop.io.zprotocol.ZCommand;
import com.isahl.chess.bishop.io.zprotocol.ZServerFactory;
import com.isahl.chess.bishop.io.ws.WsContext;
import com.isahl.chess.bishop.io.ws.WsFrame;
import com.isahl.chess.bishop.io.ws.filter.WsControlFilter;
import com.isahl.chess.bishop.io.ws.filter.WsFrameFilter;
import com.isahl.chess.bishop.io.ws.filter.WsHandShakeFilter;
import com.isahl.chess.bishop.io.ws.filter.WsProxyFilter;
import com.isahl.chess.bishop.io.zfilter.ZCommandFilter;
import com.isahl.chess.bishop.io.zfilter.ZTLSFilter;
import com.isahl.chess.queen.event.inf.ISort;
import com.isahl.chess.queen.event.operator.AioWriter;
import com.isahl.chess.queen.event.operator.CloseOperator;
import com.isahl.chess.queen.event.operator.ErrorOperator;
import com.isahl.chess.queen.event.operator.IgnoreOperator;
import com.isahl.chess.queen.event.operator.PipeDecoder;
import com.isahl.chess.queen.event.operator.PipeEncoder;
import com.isahl.chess.queen.event.operator.TransferOperator;
import com.isahl.chess.queen.io.core.inf.ICommandFactory;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.IFilterChain;
import com.isahl.chess.queen.io.core.inf.IFrame;
import com.isahl.chess.queen.io.core.inf.IPipeDecoder;
import com.isahl.chess.queen.io.core.inf.IPipeEncoder;
import com.isahl.chess.queen.io.core.inf.IPipeTransfer;
import com.isahl.chess.queen.io.core.inf.ISessionCloser;
import com.isahl.chess.queen.io.core.inf.ISessionError;
import com.isahl.chess.queen.io.core.inf.ISessionOption;

/**
 * @author william.d.zk
 * 
 * @date 2019-06-30
 */
public enum ZSort implements
                  ISort<ZContext>
{

    /**
     *
     */
    WS_CLUSTER_CONSUMER
    {
        @Override
        public Type getType()
        {
            return Type.CONSUMER;
        }

        @Override
        public Mode getMode()
        {
            return Mode.CLUSTER;
        }

        @Override
        public IFilterChain<ZContext> getFilterChain()
        {
            return _ClusterFrameFilter;
        }

        @Override
        public ZContext newContext(ISessionOption option)
        {
            return new WsContext(option, this);
        }

        @Override
        public ICommandFactory<ZCommand, WsFrame> getCommandFactory()
        {
            return _ClusterFactory;
        }
    },
    /**
     *
     */
    WS_CLUSTER_SERVER
    {
        @Override
        public Type getType()
        {
            return Type.SERVER;
        }

        @Override
        public Mode getMode()
        {
            return Mode.CLUSTER;
        }

        @Override
        public IFilterChain<ZContext> getFilterChain()
        {
            return _ClusterFrameFilter;
        }

        @Override
        public ZContext newContext(ISessionOption option)
        {
            return new WsContext(option, this);
        }

        @Override
        public ICommandFactory<ZCommand, WsFrame> getCommandFactory()
        {
            return _ClusterFactory;
        }
    },
    WS_CLUSTER_SYMMETRY
    {
        @Override
        public Type getType()
        {
            return Type.SYMMETRY;
        }

        @Override
        public Mode getMode()
        {
            return Mode.CLUSTER;
        }

        @Override
        public IFilterChain<ZContext> getFilterChain()
        {
            return _ClusterFrameFilter;
        }

        @Override
        public ZContext newContext(ISessionOption option)
        {
            return new WsContext(option, this);
        }

        @Override
        public ICommandFactory<ZCommand, WsFrame> getCommandFactory()
        {
            return _ClusterFactory;
        }
    },
    /**
     *
     */
    WS_SERVER
    {

        @Override
        public Mode getMode()
        {
            return Mode.LINK;
        }

        @Override
        public Type getType()
        {
            return Type.SERVER;
        }

        @Override
        public IFilterChain<ZContext> getFilterChain()
        {
            return _WsHandshakeFilter;
        }

        @Override
        public ZContext newContext(ISessionOption option)
        {
            return new WsContext(option, this);
        }

        @Override
        public ICommandFactory<ZCommand, WsFrame> getCommandFactory()
        {
            return _ServerFactory;
        }
    },
    /**
     *
     */
    WS_SERVER_SSL
    {
        @Override
        public IFilterChain<ZContext> getFilterChain()
        {
            return _WsHandshakeFilter;
        }

        @Override
        public Mode getMode()
        {
            return Mode.LINK;
        }

        @Override
        public Type getType()
        {
            return Type.SERVER;
        }

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
        public ICommandFactory<ZCommand, WsFrame> getCommandFactory()
        {
            return _ServerFactory;
        }
    },
    MQ_QTT_SYMMETRY
    {
        @Override
        public IFilterChain<ZContext> getFilterChain()
        {
            return _QttFrameFilter;
        }

        @Override
        public Mode getMode()
        {
            return Mode.CLUSTER;
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
        public ICommandFactory<IControl<ZContext>, QttFrame> getCommandFactory()
        {
            return _QttCommandFactory;
        }
    },
    QTT_SERVER
    {
        @Override
        public IFilterChain<ZContext> getFilterChain()
        {
            return _QttFrameFilter;
        }

        @Override
        public Mode getMode()
        {
            return Mode.LINK;
        }

        @Override
        public Type getType()
        {
            return Type.SERVER;
        }

        @Override
        public ZContext newContext(ISessionOption option)
        {
            return new QttContext(option, this);
        }

        @Override
        public ICommandFactory<IControl<ZContext>, QttFrame> getCommandFactory()
        {
            return _QttCommandFactory;
        }
    },
    WS_QTT_SERVER
    {
        @Override
        public IFilterChain<ZContext> getFilterChain()
        {
            return _QttWsHandshakeFilter;
        }

        @Override
        public Mode getMode()
        {
            return Mode.LINK;
        }

        @Override
        public Type getType()
        {
            return Type.SERVER;
        }

        @Override
        public ZContext newContext(ISessionOption option)
        {
            return new WsProxyContext(option, this, new QttContext(option, this));
        }

        @Override
        public ICommandFactory<IControl<ZContext>, QttFrame> getCommandFactory()
        {
            return _QttCommandFactory;
        }
    }

    ;

    final static ZServerFactory _ServerFactory     = new ZServerFactory();
    final static ZClusterFactory _ClusterFactory    = new ZClusterFactory();
    final static QttCommandFactory _QttCommandFactory = new QttCommandFactory();

    final QttFrameFilter _QttFrameFilter    = new QttFrameFilter();
    {
        _QttFrameFilter.linkAfter(new ZTLSFilter());
        _QttFrameFilter.linkFront(new QttControlFilter()).linkFront(new QttCommandFilter());
    }
    final WsHandShakeFilter _WsHandshakeFilter = new WsHandShakeFilter();
    {
        IFilterChain<ZContext> header = new ZTLSFilter();
        _WsHandshakeFilter.linkAfter(header);
        _WsHandshakeFilter.linkFront(new WsFrameFilter())
                          .linkFront(new WsControlFilter())
                          .linkFront(new ZCommandFilter(new ZServerFactory()));
    }

    final WsFrameFilter _ClusterFrameFilter = new WsFrameFilter();
    {
        _ClusterFrameFilter.linkAfter(new ZTLSFilter());
        _ClusterFrameFilter.linkFront(new WsControlFilter()).linkFront(new ZCommandFilter(new ZClusterFactory()));
    }

    final WsHandShakeFilter _QttWsHandshakeFilter = new WsHandShakeFilter();
    {
        QttFrameFilter mQttFrameFilter = new QttFrameFilter();
        mQttFrameFilter.linkFront(new QttControlFilter()).linkFront(new QttCommandFilter());
        _QttWsHandshakeFilter.linkFront(new WsFrameFilter())
                             .linkFront(new WsControlFilter())
                             .linkFront(new WsProxyFilter(mQttFrameFilter));
    }

    private final AioWriter<ZContext>      _AioWriter     = new AioWriter<>();
    private final ISessionCloser<ZContext> _CloseOperator = new CloseOperator<>();
    private final ISessionError<ZContext>  _ErrorOperator = new ErrorOperator<>();
    private final IPipeEncoder<ZContext>   _Encoder       = new PipeEncoder<>(_AioWriter);
    private final IPipeTransfer<ZContext>  _Transfer      = new TransferOperator<>();
    private final IPipeDecoder<ZContext>   _Decoder       = new PipeDecoder<>();
    private final IgnoreOperator<ZContext> _Ignore        = new IgnoreOperator<>();

    @Override
    public IPipeEncoder<ZContext> getEncoder()
    {
        return _Encoder;
    }

    @Override
    public IPipeDecoder<ZContext> getDecoder()
    {
        return _Decoder;
    }

    @Override
    public IPipeTransfer<ZContext> getTransfer()
    {
        return _Transfer;
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

    public static ICommandFactory<? extends IControl<ZContext>, ? extends IFrame> getCommandFactory(int serial)
    {
        if (serial > 0x110 && serial < 0x11F)
        { return _QttCommandFactory; }
        if (serial > 0x1F && serial < 0x6F)
        { return _ServerFactory; }
        if (serial >= 0x70 && serial <= 0x7F)
        { return _ClusterFactory; }
        throw new IllegalArgumentException();
    }
}
