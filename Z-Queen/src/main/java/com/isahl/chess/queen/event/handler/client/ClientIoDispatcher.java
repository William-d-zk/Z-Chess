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

package com.isahl.chess.queen.event.handler.client;

import static com.isahl.chess.king.base.inf.IError.Type.INITIATIVE_CLOSE;
import static com.isahl.chess.king.base.inf.IError.Type.PASSIVE_CLOSE;

import java.nio.channels.AsynchronousSocketChannel;

import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.disruptor.event.inf.IOperator;
import com.isahl.chess.king.base.disruptor.event.inf.IPipeEventHandler;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.config.QueenCode;
import com.isahl.chess.queen.event.QEvent;
import com.isahl.chess.queen.io.core.inf.IConnectActivity;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.lmax.disruptor.RingBuffer;

/**
 * @author william.d.zk
 */
public class ClientIoDispatcher
        implements
        IPipeEventHandler<QEvent>
{
    private final Logger             _Logger = Logger.getLogger("io.queen.dispatcher." + getClass().getName());
    private final RingBuffer<QEvent> _Link;
    private final RingBuffer<QEvent> _Decoder;
    private final RingBuffer<QEvent> _Wrote;
    private final RingBuffer<QEvent> _Error;

    public ClientIoDispatcher(RingBuffer<QEvent> linkIoPipe,
                              RingBuffer<QEvent> decoderPipe,
                              RingBuffer<QEvent> wrotePipe,
                              RingBuffer<QEvent> errorPipe)
    {

        _Link = linkIoPipe;
        _Decoder = decoderPipe;
        _Wrote = wrotePipe;
        _Error = errorPipe;
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        // convert & transfer
        switch (event.getErrorType())
        {
            case CONNECT_FAILED ->
                {
                    IPair connectFailedContent = event.getContent();
                    Throwable throwable = connectFailedContent.getFirst();
                    IConnectActivity connectActive = connectFailedContent.getSecond();
                    IOperator<Throwable,
                              IConnectActivity,
                              ITriple> connectFailedOperator = event.getEventOp();
                    error(_Link, event.getErrorType(), new Pair<>(throwable, connectActive), connectFailedOperator);
                }
            case NO_ERROR ->
                {
                    switch (event.getEventType())
                    {
                        case CONNECTED, ACCEPTED ->
                            {
                                IPair connectContent = event.getContent();
                                IConnectActivity connectActivity = connectContent.getFirst();
                                AsynchronousSocketChannel channel = connectContent.getSecond();
                                IOperator<IConnectActivity,
                                          AsynchronousSocketChannel,
                                          ITriple> connectOperator = event.getEventOp();
                                publish(_Link,
                                        event.getEventType(),
                                        new Pair<>(connectActivity, channel),
                                        connectOperator);
                            }
                        case READ ->
                            {
                                IPair readContent = event.getContent();
                                publish(_Decoder, OperatorType.DECODE, readContent, event.getEventOp());
                            }
                        case WROTE ->
                            {
                                IPair wroteContent = event.getContent();
                                publish(_Wrote, OperatorType.WROTE, wroteContent, event.getEventOp());
                            }
                        case LOCAL_CLOSE ->
                            {
                                IOperator<Void,
                                          ISession,
                                          Void> closeOperator = event.getEventOp();
                                IPair closeContent = event.getContent();
                                ISession session = closeContent.getSecond();
                                if (!session.isClosed()) {
                                    error(_Link, INITIATIVE_CLOSE, closeContent, closeOperator);
                                }
                            }
                        default -> _Logger.warning(String.format(" wrong type %s in ClientIoDispatcher",
                                                                 event.getEventType()));
                    }
                }
            default ->
                {
                    IPair errorContent = event.getContent();
                    IOperator<Throwable,
                              ISession,
                              IPair> errorOperator = event.getEventOp();
                    ISession session = errorContent.getSecond();
                    Throwable throwable = errorContent.getFirst();
                    if (!session.isClosed()) {
                        IPair transferResult = errorOperator.handle(throwable, session);
                        error(_Link,
                              PASSIVE_CLOSE,
                              new Pair<>(QueenCode.ERROR_CLOSE, session),
                              transferResult.getSecond());
                    }
                }
        }
        event.reset();
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
