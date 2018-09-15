/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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

package com.tgx.z.queen.event.operator;

import static com.tgx.z.queen.event.inf.IError.Type.READ_EOF;
import static com.tgx.z.queen.event.inf.IError.Type.READ_FAILED;
import static com.tgx.z.queen.event.inf.IError.Type.READ_ZERO;
import static com.tgx.z.queen.event.inf.IError.Type.WRITE_EOF;
import static com.tgx.z.queen.event.inf.IError.Type.WRITE_FAILED;
import static com.tgx.z.queen.event.inf.IError.Type.WRITE_ZERO;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ShutdownChannelGroupException;
import java.util.Arrays;
import java.util.Objects;

import com.tgx.z.king.base.log.Logger;
import com.tgx.z.king.base.util.IoUtil;
import com.tgx.z.king.base.util.Triple;
import com.tgx.z.queen.event.inf.IOperator;
import com.tgx.z.queen.io.core.async.AioPacket;
import com.tgx.z.queen.io.core.async.socket.AioWorker;
import com.tgx.z.queen.io.core.inf.IAioServer;
import com.tgx.z.queen.io.core.inf.ICommand;
import com.tgx.z.queen.io.core.inf.IConnectionContext;
import com.tgx.z.queen.io.core.inf.IFilterChain;
import com.tgx.z.queen.io.core.inf.IPacket;
import com.tgx.z.queen.io.core.inf.IPipeDecode;
import com.tgx.z.queen.io.core.inf.IPipeEncoder;
import com.tgx.z.queen.io.core.inf.ISession;
import com.tgx.z.queen.io.external.filter.ZCommandFilter;
import com.tgx.z.queen.io.external.filter.ZCommandFilter.CommandFactory;
import com.tgx.z.queen.io.external.filter.ZTlsFilter;
import com.tgx.z.queen.io.external.websokcet.WsContext;
import com.tgx.z.queen.io.external.websokcet.ZContext;
import com.tgx.z.queen.io.external.websokcet.bean.device.X50_DeviceMsg;
import com.tgx.z.queen.io.external.websokcet.bean.device.X51_DeviceMsgAck;
import com.tgx.z.queen.io.external.websokcet.filter.WsControlFilter;
import com.tgx.z.queen.io.external.websokcet.filter.WsFrameFilter;
import com.tgx.z.queen.io.external.websokcet.filter.WsHandShakeFilter;

@SuppressWarnings("unchecked")
public class OperatorHolder
{
    private static Logger                                                   LOG = Logger.getLogger(OperatorHolder.class.getName());

    private static IOperator<Void, ISession>                                close_operator;
    private static IOperator<Throwable, ISession>                           error_operator;
    private static IOperator<Throwable, ISession>                           ignore_operator;
    private static IOperator<Integer, ISession>                             wrote_operator;
    private static IOperator<IPacket, ISession>                             server_decoder;
    private static IOperator<IPacket, ISession>                             cluster_decoder;
    private static IOperator<ICommand, ISession>                            cluster_encoder;
    private static IOperator<IPacket, ISession>                             consumer_decoder;
    private static IOperator<ICommand, ISession>                            consumer_encoder;
    private static IOperator<ICommand, ISession>                            server_encoder;
    private static IOperator<ICommand[], ISession>                          server_transfer;
    private static IOperator<ICommand[], ISession>                          consumer_transfer;
    private static IOperator<ICommand[], ISession>                          cluster_transfer;
    private static IOperator<IConnectionContext, AsynchronousSocketChannel> connected_operator;
    private static CompletionHandler<Integer, ISession>                     aio_writer;
    private static CompletionHandler<Integer, ISession>                     aio_reader;
    private static CompletionHandler<AsynchronousSocketChannel, IAioServer> server_acceptor;
    private static CommandFactory                                           command_factory;

    static {
        command_factory = command -> {
            switch (command) {
                case X50_DeviceMsg.COMMAND:
                    return new X50_DeviceMsg();
                case X51_DeviceMsgAck.COMMAND:
                    return new X51_DeviceMsgAck();
                default:
                    LOG.warning(String.format("command is not consistentHandle: %x", command));
                    return null;
            }
        };
        close_operator = new IOperator<Void, ISession>()
        {

            @Override
            public <T, E> Triple<T, E, IOperator<T, E>> handle(Void v, ISession session) {
                try {
                    session.close();
                }
                catch (IOException e) {
                    LOG.warning("session close: %s", e, session.toString());
                }
                LOG.warning("closed operator %s", session.toString());
                return null;
            }

            @Override
            public String toString() {
                return "close_operator";
            }
        };
        error_operator = new IOperator<Throwable, ISession>()
        {
            @Override
            public Triple<Void, ISession, IOperator<Void, ISession>> handle(Throwable throwable, ISession session) {
                LOG.warning("error session:%s", throwable, session);
                return new Triple<>(null, session, close_operator);
            }

            @Override
            public String toString() {
                return "error_operator";
            }
        };
        ignore_operator = new IOperator<Throwable, ISession>()
        {
            @Override
            public <T, E> Triple<T, E, IOperator<T, E>> handle(Throwable throwable, ISession iSession) {
                LOG.warning("ignore error", throwable);
                return null;
            }

            @Override
            public String toString() {
                return "ignore_operator";
            }
        };
        wrote_operator = new IOperator<Integer, ISession>()
        {
            @Override
            public Triple<Throwable, ISession, IOperator<Throwable, ISession>> handle(Integer wroteCnt, ISession session) {
                try {
                    session.writeNext(wroteCnt, aio_writer);
                }
                catch (Exception e) {
                    return new Triple<>(e, session, ERROR_OPERATOR());
                }
                return null;
            }

            @Override
            public String toString() {
                return "wrote_operator";
            }
        };
        aio_writer = new CompletionHandler<Integer, ISession>()
        {
            @Override
            public void completed(Integer result, ISession session) {
                AioWorker worker = (AioWorker) Thread.currentThread();
                switch (result) {
                    case -1:
                        worker.publishWroteError(error_operator, WRITE_EOF, new EOFException("wrote -1!"), session);
                        break;
                    case 0:
                        worker.publishWroteError(error_operator, WRITE_ZERO, new IllegalArgumentException("wrote zero!"), session);
                        break;
                    default:
                        LOG.info("wrote %d", result);
                        worker.publishWrote(wrote_operator, result, session);
                        break;
                }
            }

            @Override
            public void failed(Throwable exc, ISession session) {
                AioWorker worker = (AioWorker) Thread.currentThread();
                worker.publishWroteError(error_operator, WRITE_FAILED, exc, session);
            }

            @Override
            public String toString() {
                return "aio_writer";
            }
        };
        aio_reader = new CompletionHandler<Integer, ISession>()
        {
            @Override
            public void completed(Integer read, ISession session) {
                AioWorker worker = (AioWorker) Thread.currentThread();
                switch (read) {
                    case -1:
                        worker.publishReadError(error_operator, READ_EOF, new EOFException("Read Negative"), session);
                        break;
                    case 0:
                        worker.publishReadError(error_operator, READ_ZERO, new IllegalStateException("Read Zero"), session);
                        session.readNext(this);
                        break;
                    default:
                        LOG.info("read count: %d", read);
                        ByteBuffer recvBuf = session.read(read);
                        worker.publishRead(session.getDecodeOperator(), new AioPacket(recvBuf), session);
                        try {
                            session.readNext(this);
                        }
                        catch (NotYetConnectedException |
                               ShutdownChannelGroupException e) {
                            worker.publishReadError(error_operator, READ_FAILED, e, session);
                        }
                        break;
                }
            }

            @Override
            public void failed(Throwable exc, ISession session) {
                AioWorker worker = (AioWorker) Thread.currentThread();
                worker.publishReadError(error_operator, READ_FAILED, exc, session);
            }

            @Override
            public String toString() {
                return "aio_reader";
            }
        };
        consumer_encoder = new IEncoder()
        {
            final WsHandShakeFilter handshakeFilter = new WsHandShakeFilter(MODE.CONSUMER);
            {
                IFilterChain<WsContext> header = new ZTlsFilter();
                handshakeFilter.linkAfter(header);
                handshakeFilter.linkFront(new WsFrameFilter())
                               .linkFront(new ZCommandFilter(command_factory))
                               .linkFront(new WsControlFilter());
            }

            @Override
            public Triple<Throwable, ISession, IOperator<Throwable, ISession>> handle(ICommand command, ISession session) {
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
            public String toString() {
                return "consumer_encoder";
            }
        };

        server_encoder = new IEncoder()
        {
            final WsHandShakeFilter handshakeFilter = new WsHandShakeFilter(MODE.SERVER);
            {
                IFilterChain<WsContext> header = new ZTlsFilter();
                handshakeFilter.linkAfter(header);
                handshakeFilter.linkFront(new WsFrameFilter())
                               .linkFront(new ZCommandFilter(command_factory))
                               .linkFront(new WsControlFilter());
            }

            @Override
            public Triple<Throwable, ISession, IOperator<Throwable, ISession>> handle(ICommand command, ISession session) {
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
            public String toString() {
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
            public Triple<Throwable, ISession, IOperator<Throwable, ISession>> handle(ICommand command, ISession session) {
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
            public String toString() {
                return "cluster_encoder";
            }

        };
        connected_operator = new IOperator<IConnectionContext, AsynchronousSocketChannel>()
        {
            //都在 LinkIo-Processor 中处理
            @Override
            public Triple<ICommand[], ISession, IOperator<ICommand[], ISession>> handle(IConnectionContext ctx,
                                                                                        AsynchronousSocketChannel channel) {
                ISession session = ctx.getSessionCreator()
                                      .createSession(channel, ctx.getConnectActive());
                ICommand[] commands = ctx.getCommandCreator()
                                         .createCommands(session);
                session.readNext(aio_reader);
                return new Triple<>(commands,
                                    session,
                                    session.getMode()
                                           .getOutOperator());
            }

            @Override
            public String toString() {
                return "connected_operator";
            }
        };
        server_acceptor = new CompletionHandler<AsynchronousSocketChannel, IAioServer>()
        {

            @Override
            public void completed(AsynchronousSocketChannel channel, IAioServer server) {
                AioWorker worker = (AioWorker) Thread.currentThread();
                worker.publishConnected(server.getConnectedOperator(),
                                        server.getMode(),
                                        server,
                                        server.getSessionCreator(),
                                        server.getCommandCreator(),
                                        server.getSessionCreated(),
                                        channel);
                server.pendingAccept();
            }

            @Override
            public void failed(Throwable exc, IAioServer server) {
                AioWorker worker = (AioWorker) Thread.currentThread();
                worker.publishAcceptError(server.getErrorOperator(), exc, server);
                server.pendingAccept();
            }

            @Override
            public String toString() {
                return "server_acceptor";
            }
        };
        server_decoder = new IDecoder()
        {

            final WsHandShakeFilter handshakeFilter = new WsHandShakeFilter(MODE.SERVER);

            {
                IFilterChain<WsContext> header = new ZTlsFilter();
                handshakeFilter.linkAfter(header);
                handshakeFilter.linkFront(new WsFrameFilter())
                               .linkFront(new ZCommandFilter(command_factory))
                               .linkFront(new WsControlFilter());
            }

            @Override
            public Triple<ICommand[], ISession, IOperator<ICommand[], ISession>> handle(IPacket inPackage, ISession session) {
                return new Triple<>(filterRead(inPackage, handshakeFilter, (ZContext) session.getContext()), session, server_transfer);
            }

            @Override
            public String toString() {
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
            public String toString() {
                return "cluster_decoder";
            }

            @Override
            public Triple<ICommand[], ISession, IOperator<ICommand[], ISession>> handle(IPacket inPackage, ISession session) {
                return new Triple<>(filterRead(inPackage, header, (ZContext) session.getContext()), session, cluster_transfer);
            }
        };
        consumer_decoder = new IDecoder()
        {
            final WsHandShakeFilter handshakeFilter = new WsHandShakeFilter(MODE.CONSUMER);

            {
                IFilterChain<WsContext> header = new ZTlsFilter();
                handshakeFilter.linkAfter(header);
                handshakeFilter.linkFront(new WsFrameFilter())
                               .linkFront(new ZCommandFilter(command_factory))
                               .linkFront(new WsControlFilter());
            }

            @Override
            public String toString() {
                return "consumer_decoder";
            }

            @Override
            public Triple<ICommand[], ISession, IOperator<ICommand[], ISession>> handle(IPacket inPackage, ISession session) {
                return new Triple<>(filterRead(inPackage, handshakeFilter, (ZContext) session.getContext()), session, consumer_transfer);
            }
        };

        server_transfer = new IOperator<ICommand[], ISession>()
        {

            @Override
            public Triple<ICommand, ISession, IOperator<ICommand, ISession>>[] transfer(ICommand[] commands, ISession session) {
                Objects.requireNonNull(commands);
                Triple<ICommand, ISession, IOperator<ICommand, ISession>>[] triples = new Triple[commands.length];
                Arrays.setAll(triples, slot -> new Triple<>(commands[slot], session, server_encoder));
                return triples;
            }

            @Override
            public String toString() {
                return "server_transfer";
            }
        };

        cluster_transfer = new IOperator<ICommand[], ISession>()
        {

            @Override
            public Triple<ICommand, ISession, IOperator<ICommand, ISession>>[] transfer(ICommand[] commands, ISession session) {
                Objects.requireNonNull(commands);
                Triple<ICommand, ISession, IOperator<ICommand, ISession>>[] triples = new Triple[commands.length];
                Arrays.setAll(triples, slot -> new Triple<>(commands[slot], session, cluster_encoder));
                return triples;
            }

            @Override
            public String toString() {
                return "cluster_transfer";
            }
        };

        consumer_transfer = new IOperator<ICommand[], ISession>()
        {

            @Override
            public Triple<ICommand, ISession, IOperator<ICommand, ISession>>[] transfer(ICommand[] commands, ISession session) {
                Objects.requireNonNull(commands);
                Triple<ICommand, ISession, IOperator<ICommand, ISession>>[] triples = new Triple[commands.length];
                Arrays.setAll(triples, slot -> new Triple<>(commands[slot], session, consumer_encoder));
                return triples;
            }

            @Override
            public String toString() {
                return "consumer_transfer";
            }
        };

    }

    private interface IEncoder
            extends
            IOperator<ICommand, ISession>,
            IPipeEncoder
    {
    }

    private interface IDecoder
            extends
            IOperator<IPacket, ISession>,
            IPipeDecode
    {
    }

    public static IOperator<Void, ISession> CLOSE_OPERATOR() {
        return close_operator;
    }

    public static IOperator<Throwable, ISession> ERROR_OPERATOR() {
        return error_operator;
    }

    public static IOperator<Throwable, ISession> IGNORE_OPERATOR() {
        return ignore_operator;
    }

    public static IOperator<Integer, ISession> WROTE_OPERATOR() {
        return wrote_operator;
    }

    public static IOperator<IConnectionContext, AsynchronousSocketChannel> CONNECTED_OPERATOR() {
        return connected_operator;
    }

    public static CompletionHandler<Integer, ISession> AIO_WRITER() {
        return aio_writer;
    }

    public static CompletionHandler<Integer, ISession> AIO_READER() {
        return aio_reader;
    }

    static IOperator<IPacket, ISession> SERVER_DECODER() {
        return server_decoder;
    }

    static IOperator<IPacket, ISession> CLUSTER_DECODER() {
        return cluster_decoder;
    }

    static IOperator<IPacket, ISession> CONSUMER_DECODER() {
        return consumer_decoder;
    }

    public static IOperator<ICommand, ISession> CONSUMER_ENCODER() {
        return consumer_encoder;
    }

    static IOperator<ICommand[], ISession> CONSUMER_TRANSFER() {
        return consumer_transfer;
    }

    public static IOperator<ICommand, ISession> SERVER_ENCODER() {
        return server_encoder;
    }

    public static IOperator<ICommand[], ISession> SERVER_TRANSFER() {
        return server_transfer;
    }

    public static IOperator<ICommand, ISession> CLUSTER_ENCODER() {
        return cluster_encoder;
    }

    static IOperator<ICommand[], ISession> CLUSTER_TRANSFER() {
        return cluster_transfer;
    }

    public static CompletionHandler<AsynchronousSocketChannel, IAioServer> SERVER_ACCEPTOR() {
        return server_acceptor;
    }

}
