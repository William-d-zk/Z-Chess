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

package com.tgx.chess.queen.event.operator;

import java.util.Arrays;
import java.util.Objects;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.IDispatcher;
import com.tgx.chess.queen.io.core.inf.IFilterChain;
import com.tgx.chess.queen.io.core.inf.IOperatorSupplier;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.IPipeDecode;
import com.tgx.chess.queen.io.core.inf.IPipeEncoder;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.external.websokcet.WsContext;
import com.tgx.chess.queen.io.external.websokcet.ZContext;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X20_SignUp;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X21_SignUpResult;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X22_SignIn;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X23_SignInResult;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X24_UpdateToken;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X25_AuthorisedToken;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X30_EventMsg;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X31_ConfirmMsg;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X32_MsgStatus;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X50_DeviceMsg;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X51_DeviceMsgAck;
import com.tgx.chess.queen.io.external.websokcet.filter.WsControlFilter;
import com.tgx.chess.queen.io.external.websokcet.filter.WsFrameFilter;
import com.tgx.chess.queen.io.external.websokcet.filter.WsHandShakeFilter;
import com.tgx.chess.queen.io.external.zfilter.ZCommandFilter;
import com.tgx.chess.queen.io.external.zfilter.ZTlsFilter;

@SuppressWarnings("unchecked")
public enum ZDispatcher
        implements
        IOperatorSupplier<IPacket,
                          ICommand[],
                          ISession>,
        IDispatcher
{
    CLUSTER_CONSUMER
    {
        @Override
        public Type getType()
        {
            return Type.CONSUMER;
        }
    },
    CLUSTER_SERVER
    {
        @Override
        public IOperator<IPacket,
                         ISession> getInOperator()
        {
            return CLUSTER_DECODER();
        }

        @Override
        public IOperator<ICommand[],
                         ISession> getOutOperator()
        {
            return null;
        }
    },
    MQ_CONSUMER
    {
        @Override
        public Type getType()
        {
            return Type.CONSUMER;
        }
    },
    MQ_SERVER,
    CONSUMER
    {
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

        @Override
        public Type getType()
        {
            return Type.CONSUMER;
        }
    },
    SERVER
    {
        @Override
        public IOperator<IPacket,
                         ISession> getInOperator()
        {
            return SERVER_DECODER();
        }

        @Override
        public IOperator<ICommand[],
                         ISession> getOutOperator()
        {
            return SERVER_TRANSFER();
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
    },
    CONSUMER_SSL
    {
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
    },
    SERVER_SSL
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
    },
    SYMMETRY
    {
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

    @Override
    public IOperator<IPacket,
                     ISession> getInOperator()
    {
        return null;
    }

    @Override
    public IOperator<ICommand[],
                     ISession> getOutOperator()
    {
        return null;
    }

    @Override
    public Mode getMode()
    {
        return Mode.CLUSTER;
    }

    public Type getType()
    {
        return Type.SERVER;
    }

    private static Logger LOG = Logger.getLogger(ZDispatcher.class.getName());

    private static IOperator<IPacket,
                             ISession>             server_decoder;
    private static IOperator<IPacket,
                             ISession>             cluster_decoder;
    private static IOperator<ICommand,
                             ISession>             cluster_encoder;
    private static IOperator<IPacket,
                             ISession>             consumer_decoder;
    private static IOperator<ICommand,
                             ISession>             consumer_encoder;
    private static IOperator<ICommand,
                             ISession>             server_encoder;
    private static IOperator<ICommand[],
                             ISession>             server_transfer;
    private static IOperator<ICommand[],
                             ISession>             consumer_transfer;
    private static IOperator<ICommand[],
                             ISession>             cluster_transfer;

    private static ZCommandFilter.CommandFactory command_factory;

    static {
        //TODO 这是一个很偷懒的做法，正常的抽象需要将指令注入过程划归不同的角色去执行
        command_factory = command -> {
            switch (command)
            {
                case X20_SignUp.COMMAND:
                    return new X20_SignUp();
                case X21_SignUpResult.COMMAND:
                    return new X21_SignUpResult();
                case X22_SignIn.COMMAND:
                    return new X22_SignIn();
                case X23_SignInResult.COMMAND:
                    return new X23_SignInResult();
                case X24_UpdateToken.COMMAND:
                    return new X24_UpdateToken();
                case X25_AuthorisedToken.COMMAND:
                    return new X25_AuthorisedToken();
                case X30_EventMsg.COMMAND:
                    return new X30_EventMsg();
                case X31_ConfirmMsg.COMMAND:
                    return new X31_ConfirmMsg();
                case X32_MsgStatus.COMMAND:
                    return new X32_MsgStatus();
                case X50_DeviceMsg.COMMAND:
                    return new X50_DeviceMsg();
                case X51_DeviceMsgAck.COMMAND:
                    return new X51_DeviceMsgAck();
                default:
                    LOG.warning(String.format("command is not Handled : %x", command));
                    return null;
            }
        };

        consumer_encoder = new IEncoder()
        {
            final WsHandShakeFilter handshakeFilter = new WsHandShakeFilter(ZDispatcher.CONSUMER);
            {
                IFilterChain<WsContext> header = new ZTlsFilter();
                handshakeFilter.linkAfter(header);
                handshakeFilter.linkFront(new WsFrameFilter())
                               .linkFront(new ZCommandFilter(command_factory))
                               .linkFront(new WsControlFilter());
            }

            @Override
            public Triple<Throwable,
                          ISession,
                          IOperator<Throwable,
                                    ISession>> handle(ICommand command, ISession session)
            {
                try {
                    IPacket send = (IPacket) filterWrite(command, handshakeFilter, (ZContext) session.getContext());
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

        server_encoder = new IEncoder()
        {
            final WsHandShakeFilter handshakeFilter = new WsHandShakeFilter(ZDispatcher.SERVER);
            {
                IFilterChain<WsContext> header = new ZTlsFilter();
                handshakeFilter.linkAfter(header);
                handshakeFilter.linkFront(new WsFrameFilter())
                               .linkFront(new ZCommandFilter(command_factory))
                               .linkFront(new WsControlFilter());
            }

            @Override
            public Triple<Throwable,
                          ISession,
                          IOperator<Throwable,
                                    ISession>> handle(ICommand command, ISession session)
            {
                try {
                    IPacket send = (IPacket) filterWrite(command, handshakeFilter, (ZContext) session.getContext());
                    Objects.requireNonNull(send);
                    LOG.info("server send:%s",
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
                return "server_encoder";
            }
        };

        cluster_encoder = new IEncoder()
        {
            final WsFrameFilter header = new WsFrameFilter();
            {
                header.linkFront(new ZCommandFilter(command_factory))
                      .linkFront(new WsControlFilter());
            }

            @Override
            public Triple<Throwable,
                          ISession,
                          IOperator<Throwable,
                                    ISession>> handle(ICommand command, ISession session)
            {
                try {
                    IPacket send = (IPacket) filterWrite(command, header, (ZContext) session.getContext());
                    Objects.requireNonNull(send);
                    LOG.info("cluster send:%s",
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
                return "cluster_encoder";
            }

        };

        server_decoder = new IDecoder()
        {

            final WsHandShakeFilter handshakeFilter = new WsHandShakeFilter(ZDispatcher.SERVER);

            {
                IFilterChain<WsContext> header = new ZTlsFilter();
                handshakeFilter.linkAfter(header);
                handshakeFilter.linkFront(new WsFrameFilter())
                               .linkFront(new ZCommandFilter(command_factory))
                               .linkFront(new WsControlFilter());
            }

            @Override
            public Triple<ICommand[],
                          ISession,
                          IOperator<ICommand[],
                                    ISession>> handle(IPacket inPackage, ISession session)
            {
                return new Triple<>(filterRead(inPackage, handshakeFilter, (ZContext) session.getContext()), session, server_transfer);
            }

            @Override
            public String toString()
            {
                return "server_decoder";
            }
        };
        cluster_decoder = new IDecoder()
        {
            final WsFrameFilter header = new WsFrameFilter();
            {
                header.linkFront(new ZCommandFilter(command_factory))
                      .linkFront(new WsControlFilter());
            }

            @Override
            public String toString()
            {
                return "cluster_decoder";
            }

            @Override
            public Triple<ICommand[],
                          ISession,
                          IOperator<ICommand[],
                                    ISession>> handle(IPacket inPackage, ISession session)
            {
                return new Triple<>(filterRead(inPackage, header, (ZContext) session.getContext()), session, cluster_transfer);
            }
        };
        consumer_decoder = new IDecoder()
        {
            final WsHandShakeFilter handshakeFilter = new WsHandShakeFilter(ZDispatcher.CONSUMER);

            {
                IFilterChain<WsContext> header = new ZTlsFilter();
                handshakeFilter.linkAfter(header);
                handshakeFilter.linkFront(new WsFrameFilter())
                               .linkFront(new ZCommandFilter(command_factory))
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
                return new Triple<>(filterRead(inPackage, handshakeFilter, (ZContext) session.getContext()), session, consumer_transfer);
            }
        };

        server_transfer = new ITransfer()
        {

            @Override
            public String toString()
            {
                return "server_transfer";
            }
        };

        cluster_transfer = new ITransfer()
        {

            @Override
            public String toString()
            {
                return "cluster_transfer";
            }
        };

        consumer_transfer = new ITransfer()
        {

            @Override
            public String toString()
            {
                return "consumer_transfer";
            }
        };

    }

    private interface ITransfer
            extends
            IOperator<ICommand[],
                      ISession>
    {
        @Override
        default Triple<ICommand,
                       ISession,
                       IOperator<ICommand,
                                 ISession>>[] transfer(ICommand[] commands, ISession session)
        {
            Objects.requireNonNull(commands);
            Triple<ICommand,
                   ISession,
                   IOperator<ICommand,
                             ISession>>[] triples = new Triple[commands.length];
            Arrays.setAll(triples, slot -> new Triple<>(commands[slot], session, server_encoder));
            return triples;
        }
    }

    private interface IEncoder
            extends
            IOperator<ICommand,
                      ISession>,
            IPipeEncoder
    {
    }

    private interface IDecoder
            extends
            IOperator<IPacket,
                      ISession>,
            IPipeDecode
    {
    }

    static IOperator<IPacket,
                     ISession> SERVER_DECODER()
    {
        return server_decoder;
    }

    static IOperator<IPacket,
                     ISession> CLUSTER_DECODER()
    {
        return cluster_decoder;
    }

    static IOperator<IPacket,
                     ISession> CONSUMER_DECODER()
    {
        return consumer_decoder;
    }

    public static IOperator<ICommand,
                            ISession> CONSUMER_ENCODER()
    {
        return consumer_encoder;
    }

    public static IOperator<ICommand[],
                            ISession> CONSUMER_TRANSFER()
    {
        return consumer_transfer;
    }

    public static IOperator<ICommand,
                            ISession> SERVER_ENCODER()
    {
        return server_encoder;
    }

    public static IOperator<ICommand[],
                            ISession> SERVER_TRANSFER()
    {
        return server_transfer;
    }

    public static IOperator<ICommand,
                            ISession> CLUSTER_ENCODER()
    {
        return cluster_encoder;
    }

    static IOperator<ICommand[],
                     ISession> CLUSTER_TRANSFER()
    {
        return cluster_transfer;
    }
}
