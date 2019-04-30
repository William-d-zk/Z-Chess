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

package com.tgx.chess.bishop.io.zoperator;

import java.util.Objects;

import com.tgx.chess.bishop.io.ws.filter.WsControlFilter;
import com.tgx.chess.bishop.io.ws.filter.WsFrameFilter;
import com.tgx.chess.bishop.io.ws.filter.WsHandShakeFilter;
import com.tgx.chess.bishop.io.zfilter.ZCommandFilter;
import com.tgx.chess.bishop.io.zfilter.ZTlsFilter;
import com.tgx.chess.bishop.io.zprotocol.ZContext;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.operator.AioWriter;
import com.tgx.chess.queen.event.operator.CloseOperator;
import com.tgx.chess.queen.event.operator.ErrorOperator;
import com.tgx.chess.queen.event.operator.TransferOperator;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.IFilterChain;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.IPipeDecoder;
import com.tgx.chess.queen.io.core.inf.IPipeEncoder;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */

@SuppressWarnings("unchecked")
public enum ZSort
        implements
        ISort
{
    /**
     * 
     */
    CLUSTER_CONSUMER {
        @Override
        public Type getType() {
            return Type.CONSUMER;
        }

    },
    /**
    *
    */
    CLUSTER_SERVER {
        @Override
        public IPipeEncoder<ZContext> getEncoder() {
            return super._ClusterEncoder;
        }

        @Override
        public IPipeDecoder<ZContext> getDecoder() {
            return super._ClusterDecoder;
        }
    },
    /**
    *
    */
    MQ_CONSUMER {
        @Override
        public Type getType() {
            return Type.CONSUMER;
        }
    },
    /**
    *
    */
    MQ_SERVER,
    /**
     *
     */
    SERVER {

        @Override
        public Mode getMode() {
            return Mode.LINK;
        }

        @Override
        public Type getType() {
            return Type.SERVER;
        }
    },
    /**
     *
     */
    SERVER_SSL {
        @Override
        public Mode getMode() {
            return Mode.LINK;
        }

        @Override
        public Type getType() {
            return Type.SERVER;
        }
    },
    /**
    *
    */
    SYMMETRY {
        @Override
        public Mode getMode() {
            return Mode.LINK;
        }

        @Override
        public Type getType() {
            return Type.SYMMETRY;
        }

    };

    @Override
    public Mode getMode() {
        return Mode.CLUSTER;
    }

    @Override
    public Type getType() {
        return Type.SERVER;
    }

    private static final Logger              _Log             = Logger.getLogger(ZSort.class.getName());
    private final CloseOperator<ZContext>    _CloseOperator   = new CloseOperator<>();
    private final ErrorOperator<ZContext>    _ErrorOperator   = new ErrorOperator<>(_CloseOperator);
    private final AioWriter<ZContext>        _AioWriter       = new AioWriter<>();
    private final IPipeEncoder<ZContext>     _ServerEncoder   = new IPipeEncoder<ZContext>()
                                                              {

                                                                  final WsHandShakeFilter<ZContext> handshakeFilter = new WsHandShakeFilter(SERVER);
                                                                  {
                                                                      IFilterChain<ZContext> header = new ZTlsFilter<>();
                                                                      handshakeFilter.linkAfter(header);
                                                                      handshakeFilter.linkFront(new WsFrameFilter<>())
                                                                                     .linkFront(new ZCommandFilter<>(ZCommandFactories.SERVER))
                                                                                     .linkFront(new WsControlFilter<>());
                                                                  }

                                                                  @Override
                                                                  public ITriple handle(ICommand command, ISession<ZContext> session) {
                                                                      try {
                                                                          IPacket send = (IPacket) filterWrite(command,
                                                                                                               handshakeFilter,
                                                                                                               session.getContext());
                                                                          Objects.requireNonNull(send);
                                                                          _Log.info("_ServerEncoder send:%s",
                                                                                    IoUtil.bin2Hex(send.getBuffer()
                                                                                                       .array(),
                                                                                                   "."));
                                                                          session.write(send, _AioWriter);
                                                                      }
                                                                      catch (Exception e) {
                                                                          return new Triple<>(e, session, _ErrorOperator);
                                                                      }
                                                                      return null;
                                                                  }

                                                                  @Override
                                                                  public String toString() {
                                                                      return "_ServerEncoder";
                                                                  }
                                                              };
    private TransferOperator<ZContext>       _ServerTransfer  = new TransferOperator<>(_ServerEncoder);
    private IPipeDecoder<ZContext>           _ServerDecoder   = new IPipeDecoder<ZContext>()
                                                              {

                                                                  final WsHandShakeFilter<ZContext> handshakeFilter = new WsHandShakeFilter<>(SERVER);
                                                                  {
                                                                      IFilterChain<ZContext> header = new ZTlsFilter<>();
                                                                      handshakeFilter.linkAfter(header);
                                                                      handshakeFilter.linkFront(new WsFrameFilter<>())
                                                                                     .linkFront(new ZCommandFilter<>(ZCommandFactories.SERVER))
                                                                                     .linkFront(new WsControlFilter<>());
                                                                  }

                                                                  @Override
                                                                  public ITriple handle(IPacket inPackage, ISession<ZContext> session) {
                                                                      return new Triple<>(filterRead(inPackage, handshakeFilter, session),
                                                                                          session,
                                                                                          _ServerTransfer);
                                                                  }

                                                                  @Override
                                                                  public String toString() {
                                                                      return "_ServerDecoder";
                                                                  }
                                                              };

    private final IPipeEncoder<ZContext>     _ClusterEncoder  = new IPipeEncoder<ZContext>()
                                                              {

                                                                  final WsFrameFilter<ZContext> header = new WsFrameFilter();
                                                                  {
                                                                      header.linkFront(new ZCommandFilter<>(ZCommandFactories.CLUSTER))
                                                                            .linkFront(new WsControlFilter<>());
                                                                  }

                                                                  @Override
                                                                  public ITriple handle(ICommand command, ISession<ZContext> session) {
                                                                      try {
                                                                          IPacket send = (IPacket) filterWrite(command,
                                                                                                               header,
                                                                                                               session.getContext());
                                                                          Objects.requireNonNull(send);
                                                                          _Log.info("cluster send:%s",
                                                                                    IoUtil.bin2Hex(send.getBuffer()
                                                                                                       .array(),
                                                                                                   "."));
                                                                          session.write(send, _AioWriter);
                                                                      }
                                                                      catch (Exception e) {
                                                                          return new Triple<>(e, session, _ErrorOperator);
                                                                      }
                                                                      return null;
                                                                  }

                                                                  @Override
                                                                  public String toString() {
                                                                      return "_ClusterEncoder";
                                                                  }
                                                              };
    private final TransferOperator<ZContext> _ClusterTransfer = new TransferOperator<>(_ClusterEncoder);
    private final IPipeDecoder<ZContext>     _ClusterDecoder  = new IPipeDecoder<ZContext>()
                                                              {
                                                                  final WsFrameFilter header = new WsFrameFilter<>();
                                                                  {
                                                                      header.linkFront(new ZCommandFilter<>(ZCommandFactories.CLUSTER))
                                                                            .linkFront(new WsControlFilter<>());
                                                                  }

                                                                  @Override
                                                                  public String toString() {
                                                                      return "_ClusterDecoder";
                                                                  }

                                                                  @Override
                                                                  public ITriple handle(IPacket input, ISession<ZContext> session) {
                                                                      return new Triple<>(filterRead(input, header, session),
                                                                                          session,
                                                                                          _ClusterTransfer);
                                                                  }
                                                              };

    public IPipeEncoder<ZContext> getEncoder() {
        return _ServerEncoder;
    }

    public IPipeDecoder<ZContext> getDecoder() {
        return _ServerDecoder;
    }

    public CloseOperator<ZContext> getCloseOperator() {
        return _CloseOperator;
    }

    public TransferOperator<ZContext> getTransfer() {
        return _ServerTransfer;
    }

    /*
    
    static {
    
        cluster_encoder = new IEncoder()
        {
            final WsFrameFilter header = new WsFrameFilter();
            {
                header.linkFront(new ZCommandFilter(ZCommandFactories.CLUSTER))
                      .linkFront(new WsControlFilter());
            }
    
            @Override
            public Triple<Throwable, ISession, IOperator<Throwable, ISession<ZContext>>> handle(ICommand command,
                                                                                                ISession<ZContext> session) {
                try {
                    IPacket send = (IPacket) filterWrite(command, header, session.getContext());
                    Objects.requireNonNull(send);
                    _Log.info("cluster send:%s",
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
            public String toString() {
                return "cluster_encoder";
            }
    
        };
    
        _ServerDecoder = cluster_decoder = new IDecoder()
        {
            final WsFrameFilter header = new WsFrameFilter();
            {
                header.linkFront(new ZCommandFilter(ZCommandFactories.CLUSTER))
                      .linkFront(new WsControlFilter());
            }
    
            @Override
            public String toString() {
                return "cluster_decoder";
            }
    
            @Override
            public Triple<ICommand[], ISession<ZContext>, IOperator<ICommand[], ISession<ZContext>>> handle(IPacket inPackage,
                                                                                                            ISession<ZContext> session) {
                return new Triple<>(filterRead(inPackage, header, session), session, cluster_transfer);
            }
        };
    
        cluster_transfer = new AbstractTransfer(cluster_encoder)
        {
    
            @Override
            public String toString() {
                return "cluster_transfer";
            }
        };
    
    }
    */
}
