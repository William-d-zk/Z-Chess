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

import com.tgx.chess.bishop.io.mqtt.bean.QttContext;
import com.tgx.chess.bishop.io.mqtt.filter.QttCommandFilter;
import com.tgx.chess.bishop.io.mqtt.filter.QttControlFilter;
import com.tgx.chess.bishop.io.mqtt.filter.QttFrameFilter;
import com.tgx.chess.bishop.io.ws.bean.WsContext;
import com.tgx.chess.bishop.io.ws.filter.WsControlFilter;
import com.tgx.chess.bishop.io.ws.filter.WsFrameFilter;
import com.tgx.chess.bishop.io.ws.filter.WsHandShakeFilter;
import com.tgx.chess.bishop.io.zfilter.ZCommandFilter;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zfilter.ZTlsFilter;
import com.tgx.chess.bishop.io.zprotocol.ZServerFactory;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.operator.*;
import com.tgx.chess.queen.io.core.inf.*;

/**
 * @author william.d.zk
 * @date 2019-06-30
 */
public enum ZSort
        implements
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
            return _WsFrameFilter;
        }

        @Override
        public ZContext newContext(ISessionOption option, ICommandCreator<ZContext> commandCreator)
        {
            return new WsContext(option, this, commandCreator);
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
            return _WsFrameFilter;
        }

        @Override
        public ZContext newContext(ISessionOption option, ICommandCreator<ZContext> commandCreator)
        {
            return new WsContext(option, this, commandCreator);
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
            return _WsFrameFilter;
        }

        @Override
        public ZContext newContext(ISessionOption option, ICommandCreator<ZContext> commandCreator)
        {
            return new WsContext(option, this, commandCreator);
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
        public ZContext newContext(ISessionOption option, ICommandCreator<ZContext> commandCreator)
        {
            return new WsContext(option, this, commandCreator);
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
        public ZContext newContext(ISessionOption option, ICommandCreator<ZContext> commandCreator)
        {
            return new WsContext(option, this, commandCreator);
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
        public ZContext newContext(ISessionOption option, ICommandCreator<ZContext> commandCreator)
        {
            return new QttContext(option, this, commandCreator);
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
        public ZContext newContext(ISessionOption option, ICommandCreator<ZContext> commandCreator)
        {
            return new QttContext(option, this, commandCreator);
        }
    };

    final QttFrameFilter _QttFrameFilter = new QttFrameFilter();
    {
        _QttFrameFilter.linkAfter(new ZTlsFilter());
        _QttFrameFilter.linkFront(new QttControlFilter())
                       .linkFront(new QttCommandFilter());
    }
    final WsHandShakeFilter _WsHandshakeFilter = new WsHandShakeFilter();
    {
        IFilterChain<ZContext> header = new ZTlsFilter();
        _WsHandshakeFilter.linkAfter(header);
        _WsHandshakeFilter.linkFront(new WsFrameFilter())
                          .linkFront(new WsControlFilter())
                          .linkFront(new ZCommandFilter(new ZServerFactory()
                          {
                          }));
    }
    final WsFrameFilter _WsFrameFilter = new WsFrameFilter();
    {
        _WsFrameFilter.linkAfter(new ZTlsFilter());
        _WsFrameFilter.linkFront(new WsControlFilter())
                      .linkFront(new ZCommandFilter(new ZServerFactory()
                      {
                      }));
    }

    private final AioWriter<ZContext>      _AioWriter     = new AioWriter<>();
    private final ISessionCloser<ZContext> _CloseOperator = new CloseOperator<>();
    private final ISessionError<ZContext>  _ErrorOperator = new ErrorOperator<>();
    private final IPipeEncoder<ZContext>   _Encoder       = new PipeEncoder<>(_AioWriter);
    private final IPipeTransfer<ZContext>  _Transfer      = new TransferOperator<>();
    private final IPipeDecoder<ZContext>   _Decoder       = new PipeDecoder<>();

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

}
