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
package com.isahl.chess.queen.io.core.async;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;

import com.isahl.chess.queen.event.processor.QEvent;
import com.isahl.chess.queen.io.core.inf.IContext;
import com.lmax.disruptor.RingBuffer;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.event.inf.IError;
import com.isahl.chess.queen.event.inf.IError.Type;
import com.isahl.chess.queen.event.inf.IOperator;
import com.isahl.chess.queen.event.operator.WroteOperator;
import com.isahl.chess.queen.io.core.inf.IAioConnector;
import com.isahl.chess.queen.io.core.inf.IAioServer;
import com.isahl.chess.queen.io.core.inf.IAvailable;
import com.isahl.chess.queen.io.core.inf.IConnectActivity;
import com.isahl.chess.queen.io.core.inf.IPacket;
import com.isahl.chess.queen.io.core.inf.IPipeDecoder;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionError;

/**
 * @author William.d.zk
 */
public class AioWorker
        extends
        Thread
{

    private final Logger                         _Logger = Logger.getLogger("io.queen.operator."
                                                                            + getClass().getSimpleName());
    private final IAvailable<RingBuffer<QEvent>> _Available;
    private final RingBuffer<QEvent>             _Producer;

    public AioWorker(Runnable r, String name, IAvailable<RingBuffer<QEvent>> available, RingBuffer<QEvent> producer)
    {
        super(r, name);
        _Available = available;
        Objects.requireNonNull(producer);
        _Producer = producer;
    }

    @Override
    public void run()
    {
        try
        {
            super.run();
        }
        catch (Error e)
        {
            _Logger.fetal("AioWorker Work UnCatchEx", e);
            if (Objects.nonNull(_Available))
            {
                _Available.available(_Producer);
            }
            throw e;
        }
    }

    public <T, A, R> void publish(RingBuffer<QEvent> producer,
                                  final IOperator<T, A, R> op,
                                  final IError.Type eType,
                                  final IOperator.Type type,
                                  IPair content)
    {
        if (producer.remainingCapacity() == 0)
        {
            _Logger.warning("aio worker %s block", getName());
        }
        long sequence = producer.next();
        try
        {
            QEvent event = producer.get(sequence);
            if (eType.equals(IError.Type.NO_ERROR))
            {
                event.produce(type, content, op);
            }
            else
            {
                event.error(eType, content, op);
            }
        }
        finally
        {
            producer.publish(sequence);
        }
    }

    public <C extends IContext<C>> void publishRead(final IPipeDecoder<C> op, IPacket pack, final ISession<C> session)
    {
        publish(_Producer, op, IError.Type.NO_ERROR, IOperator.Type.READ, new Pair<>(pack, session));
    }

    public <C extends IContext<C>> void publishWrote(final WroteOperator<C> op,
                                                     final int wroteCnt,
                                                     final ISession<C> session)
    {
        publish(_Producer, op, IError.Type.NO_ERROR, IOperator.Type.WROTE, new Pair<>(wroteCnt, session));
    }

    public <T, C extends IContext<C>>
           void
           publishWroteError(final ISessionError<C> op, final IError.Type eType, final T t, final ISession<C> session)
    {
        publish(_Producer, op, eType, IOperator.Type.WROTE, new Pair<>(t, session));
    }

    public <C extends IContext<C>>
           void
           publishConnected(final IOperator<IConnectActivity<C>, AsynchronousSocketChannel, ITriple> op,
                            final IConnectActivity<C> activity,
                            final IOperator.Type type,
                            final AsynchronousSocketChannel channel)
    {
        publish(_Producer, op, IError.Type.NO_ERROR, type, new Pair<>(activity, channel));
    }

    public <C extends IContext<C>> void publishConnectingError(final IOperator<Throwable, IAioConnector<C>, Void> op,
                                                               final Throwable e,
                                                               final IAioConnector<C> cActive)
    {
        publish(_Producer, op, IError.Type.CONNECT_FAILED, IOperator.Type.NULL, new Pair<>(e, cActive));
    }

    public <C extends IContext<C>> void publishAcceptError(final IOperator<Throwable, IAioServer<C>, Void> op,
                                                           final Throwable e,
                                                           final IAioServer<C> cActive)
    {
        publish(_Producer, op, Type.ACCEPT_FAILED, IOperator.Type.NULL, new Pair<>(e, cActive));
    }

    public <T, C extends IContext<C>>
           void
           publishReadError(final ISessionError<C> op, final IError.Type eType, final T t, final ISession<C> session)
    {
        publish(_Producer, op, eType, IOperator.Type.READ, new Pair<>(t, session));
    }
}
