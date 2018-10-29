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

package com.tgx.chess.queen.event.handler;

import static com.tgx.chess.queen.event.inf.IError.Type.CLOSED;
import static com.tgx.chess.queen.event.inf.IOperator.Type.*;

import java.nio.channels.AsynchronousSocketChannel;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.event.inf.IError;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IConnectActive;
import com.tgx.chess.queen.io.core.inf.IConnectionContext;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.ISession;

public class IoDispatcher
        extends
        BaseDispatcher
{
    private final Logger             _Log = Logger.getLogger(getClass().getName());
    private final RingBuffer<QEvent> _IoWrote;

    @SafeVarargs
    public IoDispatcher(RingBuffer<QEvent> link, RingBuffer<QEvent> cluster, RingBuffer<QEvent> wrote, RingBuffer<QEvent> error, RingBuffer<QEvent>... read)
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
                IOperator<Throwable, IConnectActive> connectFailedOperator = event.getEventOp();
                Pair<Throwable, IConnectActive> connectFailedContent = event.getContent();
                Throwable throwable = connectFailedContent.first();
                IConnectActive connectActive = connectFailedContent.second();
                dispatchError(connectActive.getMode(), errorType, throwable, connectActive, connectFailedOperator);
                break;
            case CLOSED:
                /* 将其他 Event Error 转换为 closed 进行定向分发 */
                IOperator<Void, ISession> closedOperator = event.getEventOp();
                Pair<Void, ISession> closedContent = event.getContent();
                ISession session = closedContent.second();
                if (!session.isClosed()) dispatchError(session.getMode(), CLOSED, closedContent.first(), session, closedOperator);
                break;
            case ACCEPT_FAILED:
            case READ_ZERO:
            case READ_EOF:
            case READ_FAILED:
            case WRITE_EOF:
            case WRITE_FAILED:
            case WRITE_ZERO:
            case TIME_OUT:
            case FILTER_DECODE:
            case FILTER_ENCODE:
            case ILLEGAL_STATE:
            case ILLEGAL_BIZ_STATE:
            case OUT_OF_LENGTH:
            case SSL_HANDSHAKE:
                IOperator<Throwable, ISession> errorOperator = event.getEventOp();
                Pair<Throwable, ISession> errorContent = event.getContent();
                session = errorContent.second();
                if (!session.isClosed()) {
                    throwable = errorContent.first();
                    Triple<Void, ISession, IOperator<Void, ISession>> transferResult = errorOperator.handle(throwable, session);
                    dispatchError(session.getMode(), CLOSED, transferResult.first(), session, transferResult.third());
                }
                break;
            case NO_ERROR:
                switch (event.getEventType())
                {
                    case CONNECTED:
                        IOperator<IConnectionContext, AsynchronousSocketChannel> connectOperator = event.getEventOp();
                        Pair<IConnectionContext, AsynchronousSocketChannel> connectContent = event.getContent();
                        IConnectionContext connectionContext = connectContent.first();
                        dispatch(connectionContext.getMode(), CONNECTED, connectionContext, connectContent.second(), connectOperator);
                        break;
                    case READ:
                        Pair<IPacket, ISession> readContent = event.getContent();
                        session = readContent.second();
                        publish(dispatchWorker(session.getHashKey()), TRANSFER, readContent.first(), readContent.second(), event.getEventOp());
                        break;
                    case WROTE:
                        Pair<Integer, ISession> wrote_content = event.getContent();
                        publish(_IoWrote, WROTE, wrote_content.first(), wrote_content.second(), event.getEventOp());
                        break;
                    case CLOSE:// local close
                        IOperator<Void, ISession> closeOperator = event.getEventOp();
                        Pair<Void, ISession> closeContent = event.getContent();
                        session = closeContent.second();
                        if (!session.isClosed()) error(_Error, CLOSED, closeContent.first(), closeContent.second(), closeOperator);
                        break;
                    default:
                        _Log.warning(String.format(" wrong type %s in IoDispatcher", event.getEventType()));
                        break;
                }
            default:
                break;
        }
        event.reset();
    }

}
