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

package com.isahl.chess.queen.event.handler.cluster;

import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.disruptor.event.inf.IHealth;
import com.isahl.chess.king.base.disruptor.event.inf.IOperator;
import com.isahl.chess.king.base.disruptor.event.inf.IPipeEventHandler;
import com.isahl.chess.king.base.disruptor.processor.Health;
import com.isahl.chess.king.base.inf.IError;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.config.QueenCode;
import com.isahl.chess.queen.event.QEvent;
import com.isahl.chess.queen.io.core.inf.IConnectActivity;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISort;
import com.lmax.disruptor.RingBuffer;

import java.nio.channels.AsynchronousSocketChannel;

import static com.isahl.chess.king.base.inf.IError.Type.INITIATIVE_CLOSE;
import static com.isahl.chess.king.base.inf.IError.Type.PASSIVE_CLOSE;

/**
 * @author william.d.zk
 */
public class IoDispatcher
        implements IPipeEventHandler<QEvent>
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
    public IHealth getHealth()
    {
        return _Health;
    }

    @Override
    public void onEvent(QEvent event, long sequence) throws Exception
    {
        IError.Type errorType = event.getErrorType();
        switch(errorType) {
            case ACCEPT_FAILED:
            case CONNECT_FAILED:
                IPair connectFailedContent = event.getContent();
                Throwable throwable = connectFailedContent.getFirst();
                IConnectActivity connectActive = connectFailedContent.getSecond();
                error(getNextPipe(connectActive.getMode()),
                      errorType,
                      new Pair<>(throwable, connectActive),
                      event.getEventOp());
                break;
            case CONSISTENCY_REJECT:
                IPair consistencyRejected = event.getContent();
                IControl reject = consistencyRejected.getFirst();
                _Logger.debug("consistency reject: %s", consistencyRejected);
                ISession session = consistencyRejected.getSecond();
                IOperator<Throwable, ISession, IPair> errorOperator = event.getEventOp();
                if(!session.isClosed()) {
                    IPair result = errorOperator.handle(null, session);
                    error(getNextPipe(session.getMode()),
                          INITIATIVE_CLOSE,
                          new Pair<>(QueenCode.ERROR_CLOSE, session),
                          result.getSecond());
                }
                break;
            case NO_ERROR:
                switch(event.getEventType()) {
                    case CONNECTED, ACCEPTED -> {
                        _Logger.trace("connected");
                        IPair connectContent = event.getContent();
                        connectActive = connectContent.getFirst();
                        AsynchronousSocketChannel channel = connectContent.getSecond();
                        IOperator<IConnectActivity, AsynchronousSocketChannel, ITriple> connectOperator = event.getEventOp();
                        publish(getNextPipe(connectActive.getMode()),
                                event.getEventType(),
                                new Pair<>(connectActive, channel),
                                connectOperator);
                    }
                    case READ -> {
                        _Logger.trace("read");
                        IPair readContent = event.getContent();
                        session = readContent.getSecond();
                        publish(dispatchWorker(session.hashCode()),
                                OperatorType.DECODE,
                                readContent,
                                event.getEventOp());
                    }
                    case WROTE -> {
                        _Logger.trace("wrote");
                        IPair wroteContent = event.getContent();
                        publish(_IoWrote, OperatorType.WROTE, wroteContent, event.getEventOp());
                    }
                    case LOCAL_CLOSE -> {
                        _Logger.trace("local close");
                        IPair closeContent = event.getContent();
                        session = closeContent.getSecond();
                        if(!session.isClosed()) {
                            error(getNextPipe(session.getMode()),
                                  INITIATIVE_CLOSE,
                                  new Pair<>(QueenCode.LOCAL_CLOSE, session),
                                  event.getEventOp());
                        }
                    }
                    default -> _Logger.warning(String.format(" wrong type %s in IoDispatcher", event.getEventType()));
                }
                break;
            default:
                /* 未指定类型的错误 来自Decoded/Encoded Dispatcher */
                errorOperator = event.getEventOp();
                IPair errorContent = event.getContent();
                throwable = errorContent.getFirst();
                session = errorContent.getSecond();
                _Logger.warning("error %s @ %s, → mapping handler [close] \n",
                                throwable,
                                errorType.getMsg(),
                                session.summary());
                if(!session.isClosed()) {
                    IPair result = errorOperator.handle(throwable, session);
                    error(getNextPipe(session.getMode()),
                          PASSIVE_CLOSE,
                          new Pair<>(QueenCode.ERROR_CLOSE, session),
                          result.getSecond());
                }
                break;
        }
        event.reset();
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
    public Logger getLogger()
    {
        return _Logger;
    }
}
