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

package com.tgx.chess.queen.event.handler;

import static com.tgx.chess.queen.event.inf.IError.Type.CLOSED;
import static com.tgx.chess.queen.event.inf.IOperator.Type.CONNECTED;
import static com.tgx.chess.queen.event.inf.IOperator.Type.TRANSFER;
import static com.tgx.chess.queen.event.inf.IOperator.Type.WROTE;

import java.nio.channels.AsynchronousSocketChannel;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.event.inf.IError;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class IoDispatcher<C extends IContext<C>>
        extends
        BaseDispatcher<C>
{
    private final Logger             _Logger = Logger.getLogger(getClass().getName());
    private final RingBuffer<QEvent> _IoWrote;

    @SafeVarargs
    public IoDispatcher(RingBuffer<QEvent> link,
                        RingBuffer<QEvent> cluster,
                        RingBuffer<QEvent> wrote,
                        RingBuffer<QEvent> error,
                        RingBuffer<QEvent>... read)
    {
        super(link, cluster, error, read);
        _IoWrote = wrote;
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean batch) throws Exception
    {
        IError.Type errorType = event.getErrorType();
        switch (errorType)
        {
            case CONNECT_FAILED:
                IPair connectFailedContent = event.getContent();
                Throwable throwable = connectFailedContent.getFirst();
                IConnectActivity<C> connectActive = connectFailedContent.getSecond();
                dispatchError(connectActive.getSort(), errorType, throwable, connectActive, event.getEventOp());
                break;
            case CLOSED:
                /* 将其他 Event Error 转换为 closed 进行定向分发 */
                IOperator<Void,
                          ISession<C>,
                          Void> closedOperator = event.getEventOp();
                IPair closedContent = event.getContent();
                ISession<C> session = closedContent.getSecond();
                if (!session.isClosed()) {
                    dispatchError(session.getContext()
                                         .getSort(),
                                  CLOSED,
                                  closedContent.getFirst(),
                                  session,
                                  closedOperator);
                }
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
                        dispatch(context.getSort(), CONNECTED, context, channel, connectOperator);
                        break;
                    case READ:
                        IPair readContent = event.getContent();
                        session = readContent.getSecond();
                        publish(dispatchWorker(session.getHashKey()), TRANSFER, readContent, event.getEventOp());
                        break;
                    case WROTE:
                        IPair wroteContent = event.getContent();
                        publish(_IoWrote, WROTE, wroteContent, event.getEventOp());
                        break;
                    case CLOSE:// local close
                        IOperator<Void,
                                  ISession<C>,
                                  Void> closeOperator = event.getEventOp();
                        IPair closeContent = event.getContent();
                        session = closeContent.getSecond();
                        if (!session.isClosed()) {
                            error(_Error, CLOSED, closeContent, closeOperator);
                        }
                        break;
                    default:
                        _Logger.warning(String.format(" wrong type %s in IoDispatcher", event.getEventType()));
                        break;
                }
                break;
            default:
                IOperator<Throwable,
                          ISession<C>,
                          ITriple> errorOperator = event.getEventOp();
                IPair errorContent = event.getContent();
                session = errorContent.getSecond();
                if (!session.isClosed()) {
                    throwable = errorContent.getFirst();
                    ITriple result = errorOperator.handle(throwable, session);
                    dispatchError(session.getContext()
                                         .getSort(),
                                  CLOSED,
                                  result.getFirst(),
                                  session,
                                  result.getThird());
                }
                break;
        }
        event.reset();
    }

}
