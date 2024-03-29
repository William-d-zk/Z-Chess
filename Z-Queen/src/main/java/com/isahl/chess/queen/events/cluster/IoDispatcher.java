/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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

package com.isahl.chess.queen.events.cluster;

import com.isahl.chess.king.base.disruptor.components.Health;
import com.isahl.chess.king.base.disruptor.features.debug.IHealth;
import com.isahl.chess.king.base.disruptor.features.flow.IPipeHandler;
import com.isahl.chess.king.base.disruptor.features.functions.IBinaryOperator;
import com.isahl.chess.king.base.disruptor.features.functions.OperateType;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.IError;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.io.core.features.model.channels.IConnectActivity;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.lmax.disruptor.RingBuffer;

import java.nio.channels.AsynchronousSocketChannel;

import static com.isahl.chess.king.base.features.IError.Type.INITIATIVE_CLOSE;
import static com.isahl.chess.king.base.features.IError.Type.PASSIVE_CLOSE;

/**
 * @author william.d.zk
 */
public class IoDispatcher
        implements IPipeHandler<QEvent>
{
    private final Logger               _Logger = Logger.getLogger("io.queen.dispatcher." + getClass().getSimpleName());
    private final RingBuffer<QEvent>   _IoWrote;
    private final RingBuffer<QEvent>   _Cluster;
    private final RingBuffer<QEvent>[] _Workers;
    private final int                  _WorkerMask;
    private final IHealth              _Health = new Health(-1);

    @SafeVarargs
    public IoDispatcher(RingBuffer<QEvent> cluster, RingBuffer<QEvent> wrote, RingBuffer<QEvent>... read)
    {
        _Cluster = cluster;
        _IoWrote = wrote;
        _Workers = read;
        _WorkerMask = _Workers.length - 1;
        if(Integer.bitCount(_Workers.length) != 1) {
            throw new IllegalArgumentException("workers' length must be a power of 2");
        }
    }

    @Override
    public IHealth _Health()
    {
        return _Health;
    }

    @Override
    public void onEvent(QEvent event, long sequence) throws Exception
    {
        IError.Type errorType = event.getErrorType();
        switch(errorType) {
            case ACCEPT_FAILED, CONNECT_FAILED -> {
                _Logger.warning("connection build failed");
                IPair content = event.getComponent();
                Throwable throwable = content.getFirst();
                IConnectActivity connector = content.getSecond();
                error(getNextPipe(connector.getMode()), errorType, new Pair<>(throwable, connector), event.getEventBinaryOp());
            }
            case CONSISTENCY_REJECT -> {
                IPair content = event.getComponent();
                Throwable throwable = content.getFirst();
                _Logger.debug("consistency reject: %s", content);
                ISession session = content.getSecond();
                IBinaryOperator<Throwable, ISession, IPair> op = event.getEventBinaryOp();
                if(!session.isClosed()) {
                    IPair result = op.handle(throwable, session);
                    error(getNextPipe(session.getMode()), INITIATIVE_CLOSE, Pair.of(throwable, session), result.getSecond());
                }
            }
            case NO_ERROR -> {
                switch(event.getEventType()) {
                    case CONNECTED, ACCEPTED -> {
                        _Logger.debug("connected");
                        IPair connectContent = event.getComponent();
                        IConnectActivity connector = connectContent.getFirst();
                        AsynchronousSocketChannel channel = connectContent.getSecond();
                        IBinaryOperator<IConnectActivity, AsynchronousSocketChannel, ITriple> connectOperator = event.getEventBinaryOp();
                        publish(getNextPipe(connector.getMode()), event.getEventType(), Pair.of(connector, channel), connectOperator);
                    }
                    case READ -> {
                        _Logger.debug("read");
                        IPair content = event.getComponent();
                        ISession session = content.getSecond();
                        publish(dispatchWorker(session.hashCode()), OperateType.DECODE, content, event.getEventBinaryOp());
                    }
                    case WROTE -> {
                        _Logger.debug("wrote");
                        IPair wroteContent = event.getComponent();
                        publish(_IoWrote, OperateType.WROTE, wroteContent, event.getEventBinaryOp());
                    }
                    case LOCAL_CLOSE -> {
                        _Logger.debug("local close");
                        IPair content = event.getComponent();
                        ISession session = content.getSecond();
                        if(!session.isClosed()) {
                            error(getNextPipe(session.getMode()), INITIATIVE_CLOSE, Pair.of(new ZException("initiative close"), session), event.getEventBinaryOp());
                        }
                    }
                    default -> _Logger.warning(String.format(" wrong type %s in IoDispatcher", event.getEventType()));
                }
            }
            default -> {
                /* 未指定类型的错误 来自Decoded/Encoded Dispatcher */
                IBinaryOperator<Throwable, ISession, IPair> op = event.getEventBinaryOp();
                IPair content = event.getComponent();
                Throwable throwable = content.getFirst();
                ISession session = content.getSecond();
                _Logger.warning("error %s @ %s, → mapping handler [close] \n", throwable, errorType.getMsg(), session.summary());
                if(!session.isClosed()) {
                    IPair result = op.handle(throwable, session);
                    error(getNextPipe(session.getMode()), PASSIVE_CLOSE, Pair.of(throwable, session), result.getSecond());
                }

            }

        }
    }

    private RingBuffer<QEvent> dispatchWorker(int code)
    {
        return _Workers[code & _WorkerMask];
    }

    protected RingBuffer<QEvent> getNextPipe(ISort.Mode mode)
    {
        return mode == ISort.Mode.CLUSTER ? _Cluster : null;
    }

    @Override
    public Logger _Logger()
    {
        return _Logger;
    }
}
