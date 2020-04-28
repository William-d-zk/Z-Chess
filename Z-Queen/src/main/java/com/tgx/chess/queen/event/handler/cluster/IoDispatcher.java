/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.tgx.chess.queen.event.handler.cluster;

import static com.tgx.chess.queen.event.inf.IError.Type.WAIT_CLOSE;
import static com.tgx.chess.queen.event.inf.IOperator.Type.CONNECTED;
import static com.tgx.chess.queen.event.inf.IOperator.Type.TRANSFER;
import static com.tgx.chess.queen.event.inf.IOperator.Type.WROTE;

import java.nio.channels.AsynchronousSocketChannel;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IError;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.IPipeEventHandler;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class IoDispatcher<C extends IContext<C>>
        implements
        IPipeEventHandler<QEvent>
{
    private final Logger               _Logger = Logger.getLogger(getClass().getSimpleName());
    private final RingBuffer<QEvent>   _IoWrote;
    private final RingBuffer<QEvent>   _Cluster;
    private final RingBuffer<QEvent>[] _Workers;
    private final int                  _WorkerMask;

    @SafeVarargs
    public IoDispatcher(RingBuffer<QEvent> cluster,
                        RingBuffer<QEvent> wrote,
                        RingBuffer<QEvent>... read)
    {
        _Cluster = cluster;
        _IoWrote = wrote;
        _Workers = read;
        _WorkerMask = _Workers.length - 1;
        if (Integer.bitCount(_Workers.length) != 1) {
            throw new IllegalArgumentException("workers' length must be a power of 2");
        }
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean batch) throws Exception
    {
        IError.Type errorType = event.getErrorType();
        switch (errorType)
        {
            case ACCEPT_FAILED:
            case CONNECT_FAILED:
                IPair connectFailedContent = event.getContent();
                Throwable throwable = connectFailedContent.getFirst();
                IConnectActivity<C> connectActive = connectFailedContent.getSecond();
                publishError(connectActive.getSort(), errorType, throwable, connectActive, event.getEventOp());
                break;
            case NO_ERROR:
                switch (event.getEventType())
                {
                    case CONNECTED:
                        IPair connectContent = event.getContent();
                        IConnectActivity<C> context = connectContent.getFirst();
                        AsynchronousSocketChannel channel = connectContent.getSecond();
                        IOperator<IConnectActivity<C>,
                                  AsynchronousSocketChannel,
                                  ITriple> connectOperator = event.getEventOp();
                        publishConnected(context.getSort(), context, channel, connectOperator);
                        break;
                    case READ:
                        IPair readContent = event.getContent();
                        ISession<C> session = readContent.getSecond();
                        publish(dispatchWorker(session.getHashKey()), TRANSFER, readContent, event.getEventOp());
                        break;
                    case WROTE:
                        IPair wroteContent = event.getContent();
                        publish(_IoWrote, WROTE, wroteContent, event.getEventOp());
                        break;
                    case CLOSE:// local close
                        IPair closeContent = event.getContent();
                        session = closeContent.getSecond();
                        if (!session.isClosed()) {
                            publishError(session.getContext()
                                                .getSort(),
                                         WAIT_CLOSE,
                                         null,
                                         session,
                                         event.getEventOp());
                        }
                        break;
                    default:
                        _Logger.warning(String.format(" wrong type %s in IoDispatcher", event.getEventType()));
                        break;
                }
                break;
            default:
                /*未指定类型的错误 来自Decoded/Encoded Dispatcher */
                IOperator<Throwable,
                          ISession<C>,
                          ITriple> errorOperator = event.getEventOp();
                IPair errorContent = event.getContent();
                ISession<C> session = errorContent.getSecond();
                if (!session.isClosed()) {
                    throwable = errorContent.getFirst();
                    ITriple result = errorOperator.handle(throwable, session);
                    publishError(session.getContext()
                                        .getSort(),
                                 WAIT_CLOSE,
                                 null,
                                 session,
                                 result.getThird());
                }
                break;
        }
        event.reset();
    }

    private RingBuffer<QEvent> dispatchWorker(long seq)
    {
        return _Workers[(int) (seq & _WorkerMask)];
    }

    private <V,
             A,
             R> void publishConnected(ISort<C> sorter,
                                      V v,
                                      A a,
                                      IOperator<V,
                                                A,
                                                R> op)
    {
        if (sorter.getMode() == ISort.Mode.CLUSTER) {
            publish(_Cluster, CONNECTED, new Pair<>(v, a), op);
        }
    }

    private <V,
             A,
             R> void publishError(ISort<C> sorter,
                                  IError.Type type,
                                  V v,
                                  A a,
                                  IOperator<V,
                                            A,
                                            R> op)
    {
        if (sorter.getMode() == ISort.Mode.CLUSTER) {
            error(_Cluster, type, new Pair<>(v, a), op);
        }
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
