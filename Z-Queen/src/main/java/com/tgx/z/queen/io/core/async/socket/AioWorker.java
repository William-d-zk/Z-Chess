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
package com.tgx.z.queen.io.core.async.socket;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lmax.disruptor.RingBuffer;
import com.tgx.z.queen.event.inf.IError;
import com.tgx.z.queen.event.inf.IError.Type;
import com.tgx.z.queen.event.inf.IOperator;
import com.tgx.z.queen.event.operator.MODE;
import com.tgx.z.queen.event.processor.QEvent;
import com.tgx.z.queen.io.core.inf.IAioConnector;
import com.tgx.z.queen.io.core.inf.IAioServer;
import com.tgx.z.queen.io.core.inf.IAvailable;
import com.tgx.z.queen.io.core.inf.ICommandCreator;
import com.tgx.z.queen.io.core.inf.IConnectActive;
import com.tgx.z.queen.io.core.inf.IConnectionContext;
import com.tgx.z.queen.io.core.inf.IPacket;
import com.tgx.z.queen.io.core.inf.ISession;
import com.tgx.z.queen.io.core.inf.ISessionCreated;
import com.tgx.z.queen.io.core.inf.ISessionCreator;
import com.tgx.z.queen.io.core.manager.ConnectionContext;

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

    public <T, A> void publish(final IOperator<T, A> op, final IError.Type eType, final IOperator.Type type, final T t, final A a) {
        long sequence = _Producer.next();
        try {
            QEvent event = _Producer.get(sequence);
            if (eType.equals(IError.Type.NO_ERROR)) {
                event.produce(type, t, a, op);
            }
            else {
                event.error(eType, t, a, op);
            }

        }
        finally {
            _Producer.publish(sequence);
        }
    }

    public void publishRead(final IOperator<IPacket, ISession> op, IPacket pack, final ISession session) {
        publish(op, IError.Type.NO_ERROR, IOperator.Type.READ, pack, session);
    }

    public void publishWrote(final IOperator<Integer, ISession> op, final int wroteCnt, final ISession session) {
        publish(op, IError.Type.NO_ERROR, IOperator.Type.WROTE, wroteCnt, session);
    }

    public <T> void publishWroteError(final IOperator<T, ISession> op, final IError.Type eType, final T t, final ISession session) {
        publish(op, eType, IOperator.Type.NULL, t, session);
    }

    public void publishConnected(final IOperator<IConnectionContext, AsynchronousSocketChannel> op,
                                 final MODE mode,
                                 final IConnectActive active,
                                 final ISessionCreator sessionCreator,
                                 final ICommandCreator commandCreator,
                                 final ISessionCreated sessionCreated,
                                 final AsynchronousSocketChannel channel) {
        publish(op,
                IError.Type.NO_ERROR,
                IOperator.Type.CONNECTED,
                new ConnectionContext(mode, active, sessionCreator, commandCreator, sessionCreated),
                channel);
    }

    public void publishConnectingError(final IOperator<Throwable, IAioConnector> op, final Throwable e, final IAioConnector cActive) {
        publish(op, IError.Type.CONNECT_FAILED, IOperator.Type.NULL, e, cActive);
    }

    public void publishAcceptError(final IOperator<Throwable, IAioServer> op, final Throwable e, final IAioServer cActive) {
        publish(op, Type.ACCEPT_FAILED, IOperator.Type.NULL, e, cActive);
    }

    public <T> void publishReadError(final IOperator<T, ISession> op, final IError.Type eType, final T t, final ISession session) {
        publish(op, eType, IOperator.Type.NULL, t, session);
    }

}
