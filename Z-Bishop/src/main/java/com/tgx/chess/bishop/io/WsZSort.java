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

package com.tgx.chess.bishop.io;

import com.tgx.chess.bishop.io.ws.filter.WsControlFilter;
import com.tgx.chess.bishop.io.ws.filter.WsFrameFilter;
import com.tgx.chess.bishop.io.ws.filter.WsHandShakeFilter;
import com.tgx.chess.bishop.io.zfilter.ZCommandFilter;
import com.tgx.chess.bishop.io.zfilter.ZTlsFilter;
import com.tgx.chess.bishop.io.zprotocol.ZCommandFactories;
import com.tgx.chess.bishop.io.zprotocol.ZContext;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.operator.AioWriter;
import com.tgx.chess.queen.event.operator.CloseOperator;
import com.tgx.chess.queen.event.operator.ErrorOperator;
import com.tgx.chess.queen.event.operator.PipeDecoder;
import com.tgx.chess.queen.event.operator.PipeEncoder;
import com.tgx.chess.queen.event.operator.TransferOperator;
import com.tgx.chess.queen.io.core.inf.IFilterChain;
import com.tgx.chess.queen.io.core.inf.IPipeDecoder;
import com.tgx.chess.queen.io.core.inf.IPipeEncoder;

/**
 * @author william.d.zk
 * @date 2019-04-24
 */

@SuppressWarnings("unchecked")
public enum WsZSort
        implements
        ISort
{
    /**
     *
     */
    CLUSTER_CONSUMER
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
        IFilterChain<ZContext> getFilterChain()
        {
            return _FrameFilter;
        }
    },
    /**
    *
    */
    CLUSTER_SERVER
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
        IFilterChain<ZContext> getFilterChain()
        {
            return _FrameFilter;
        }
    },
    /**
    *
    */
    MQ_CONSUMER
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
        IFilterChain<ZContext> getFilterChain()
        {
            return _FrameFilter;
        }
    },
    /**
    *
    */
    MQ_SERVER
    {

        @Override
        public Mode getMode()
        {
            return Mode.CLUSTER;
        }

        @Override
        public Type getType()
        {
            return Type.SERVER;
        }

        @Override
        IFilterChain<ZContext> getFilterChain()
        {
            return _FrameFilter;
        }
    },
    /**
     *
     */
    SERVER
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
        IFilterChain<ZContext> getFilterChain()
        {
            return _HandshakeFilter;
        }

    },
    /**
     *
     */
    SERVER_SSL
    {
        @Override
        IFilterChain<ZContext> getFilterChain()
        {
            return _HandshakeFilter;
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
    },
    /**
    *
    */
    SYMMETRY
    {
        @Override
        IFilterChain<ZContext> getFilterChain()
        {
            return _FrameFilter;
        }

        @Override
        public Mode getMode()
        {
            return Mode.LINK;
        }

        @Override
        public Type getType()
        {
            return Type.SYMMETRY;
        }

    };

    private final CloseOperator<ZContext> _CloseOperator   = new CloseOperator<>();
    private final ErrorOperator<ZContext> _ErrorOperator   = new ErrorOperator<>(_CloseOperator);
    private final AioWriter<ZContext>     _AioWriter       = new AioWriter<>();
    private final IPipeEncoder<ZContext>  _Encoder         = new PipeEncoder<>(getFilterChain(),
                                                                               _ErrorOperator,
                                                                               _AioWriter);
    private TransferOperator<ZContext>    _Transfer        = new TransferOperator<>(_Encoder);
    private IPipeDecoder<ZContext>        _Decoder         = new PipeDecoder<>(getFilterChain(), _Transfer);
    final WsHandShakeFilter<ZContext>     _HandshakeFilter = new WsHandShakeFilter(this);
    {
        IFilterChain<ZContext> header = new ZTlsFilter<>();
        _HandshakeFilter.linkAfter(header);
        _HandshakeFilter.linkFront(new WsFrameFilter<>())
                        .linkFront(new ZCommandFilter<>(ZCommandFactories.SERVER))
                        .linkFront(new WsControlFilter<>());
    }
    final WsFrameFilter<ZContext> _FrameFilter = new WsFrameFilter();
    {
        _FrameFilter.linkFront(new ZCommandFilter<>(ZCommandFactories.CLUSTER))
                    .linkFront(new WsControlFilter<>());
    }

    public IPipeEncoder<ZContext> getEncoder()
    {
        return _Encoder;
    }

    public IPipeDecoder<ZContext> getDecoder()
    {
        return _Decoder;
    }

    public CloseOperator<ZContext> getCloseOperator()
    {
        return _CloseOperator;
    }

    public TransferOperator<ZContext> getTransfer()
    {
        return _Transfer;
    }

    abstract IFilterChain<ZContext> getFilterChain();

}
