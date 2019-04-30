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
package com.tgx.chess.queen.io.core.async.socket;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IError;
import com.tgx.chess.queen.event.inf.IError.Type;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IAioConnector;
import com.tgx.chess.queen.io.core.inf.IAioServer;
import com.tgx.chess.queen.io.core.inf.IAvailable;
import com.tgx.chess.queen.io.core.inf.ICommandCreator;
import com.tgx.chess.queen.io.core.inf.IConnectActive;
import com.tgx.chess.queen.io.core.inf.IConnectionContext;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionCreated;
import com.tgx.chess.queen.io.core.inf.ISessionCreator;
import com.tgx.chess.queen.io.core.manager.ConnectionContext;

/**
 * @author William.d.zk
 */
public class AioWorker
        extends
        Thread
{

    private final Logger                         _Logger = Logger.getLogger(getClass().getSimpleName());
    private final IAvailable<RingBuffer<QEvent>> _Available;
    private final RingBuffer<QEvent>             _Producer;

    public AioWorker(Runnable r, String name, IAvailable<RingBuffer<QEvent>> available, RingBuffer<QEvent> producer) {
        super(r, name);
        _Available = available;
        Objects.requireNonNull(producer);
        _Producer = producer;
    }

    public AioWorker(Runnable r, String name, RingBuffer<QEvent> producer) {
        this(r, name, null, producer);
    }

    @Override
    public void run() {
        try {
            super.run();
        }
        catch (Error e) {
            _Logger.log(Level.FINER, "AioWorker Work UnCatchEx", e);
            if (Objects.nonNull(_Available)) {
                _Available.available(_Producer);
            }
        }
    }

    public <T, A, R> void publish(final IOperator<T, A, R> op, final IError.Type eType, final IOperator.Type type, IPair content) {
        long sequence = _Producer.next();
        try {
            QEvent event = _Producer.get(sequence);
            if (eType.equals(IError.Type.NO_ERROR)) {
                event.produce(type, content, op);
            }
            else {
                event.error(eType, content, op);
            }

        }
        finally {
            _Producer.publish(sequence);
        }
    }

    public <C extends IContext> void publishRead(final IOperator<IPacket, ISession<C>, ITriple> op,
                                                 IPacket pack,
                                                 final ISession<C> session) {
        publish(op, IError.Type.NO_ERROR, IOperator.Type.READ, new Pair<>(pack, session));
    }

    public <C extends IContext> void publishWrote(final IOperator<Integer, ISession<C>, ITriple> op,
                                                  final int wroteCnt,
                                                  final ISession<C> session) {
        publish(op, IError.Type.NO_ERROR, IOperator.Type.WROTE, new Pair<>(wroteCnt, session));
    }

    public <T, C extends IContext> void publishWroteError(final IOperator<T, ISession<C>, ITriple> op,
                                                          final IError.Type eType,
                                                          final T t,
                                                          final ISession<C> session) {
        publish(op, eType, IOperator.Type.NULL, new Pair<>(t, session));
    }

    public <C extends IContext> void publishConnected(final IOperator<IConnectionContext<C>, AsynchronousSocketChannel, ITriple> op,
                                                      final ISort sort,
                                                      final IConnectActive active,
                                                      final ISessionCreator<C> sessionCreator,
                                                      final ICommandCreator<C> commandCreator,
                                                      final ISessionCreated<C> sessionCreated,
                                                      final AsynchronousSocketChannel channel) {
        publish(op,
                IError.Type.NO_ERROR,
                IOperator.Type.CONNECTED,
                new Pair<>(new ConnectionContext<>(sort, active, sessionCreator, commandCreator, sessionCreated), channel));
    }

    public <C extends IContext> void publishConnectingError(final IOperator<Throwable, IAioConnector<C>, IAioConnector<C>> op,
                                                            final Throwable e,
                                                            final IAioConnector<C> cActive) {
        publish(op, IError.Type.CONNECT_FAILED, IOperator.Type.NULL, new Pair<>(e, cActive));
    }

    public <C extends IContext> void publishAcceptError(final IOperator<Throwable, IAioServer<C>, IAioServer<C>> op,
                                                        final Throwable e,
                                                        final IAioServer<C> cActive) {
        publish(op, Type.ACCEPT_FAILED, IOperator.Type.NULL, new Pair<>(e, cActive));
    }

    public <T, C extends IContext> void publishReadError(final IOperator<T, ISession<C>, ?> op,
                                                         final IError.Type eType,
                                                         final T t,
                                                         final ISession<C> session) {
        publish(op, eType, IOperator.Type.NULL, new Pair<>(t, session));
    }

}
