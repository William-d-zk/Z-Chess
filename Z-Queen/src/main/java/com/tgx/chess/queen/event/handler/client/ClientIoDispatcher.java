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

package com.tgx.chess.queen.event.handler.client;

import static com.tgx.chess.queen.event.inf.IError.Type.CLOSED;
import static com.tgx.chess.queen.event.inf.IOperator.Type.CONNECTED;
import static com.tgx.chess.queen.event.inf.IOperator.Type.TRANSFER;
import static com.tgx.chess.queen.event.inf.IOperator.Type.WROTE;

import java.nio.channels.AsynchronousSocketChannel;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.handler.BasePipeEventHandler;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IConnectActive;
import com.tgx.chess.queen.io.core.inf.IConnectionContext;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class ClientIoDispatcher<C extends IContext>
        extends
        BasePipeEventHandler<C>
{
    private final RingBuffer<QEvent> _LinkIoPipe;
    private final RingBuffer<QEvent> _DecoderPipe;
    private final RingBuffer<QEvent> _WrotePipe;
    private final RingBuffer<QEvent> _ErrorPipe;
    private final Logger             _Log = Logger.getLogger(getClass().getName());

    public ClientIoDispatcher(RingBuffer<QEvent> linkIoPipe,
                              RingBuffer<QEvent> decoderPipe,
                              RingBuffer<QEvent> wrotePipe,
                              RingBuffer<QEvent> errorPipe) {

        _LinkIoPipe = linkIoPipe;
        _DecoderPipe = decoderPipe;
        _WrotePipe = wrotePipe;
        _ErrorPipe = errorPipe;
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception {
        switch (event.getErrorType()) {
            case CONNECT_FAILED:
                IPair connectFailedContent = event.getContent();
                Throwable throwable = connectFailedContent.first();
                IConnectActive connectActive = connectFailedContent.second();
                IOperator<Throwable, IConnectActive, ITriple> connectFailedOperator = event.getEventOp();
                error(_LinkIoPipe, event.getErrorType(), new Pair<>(throwable, connectActive), connectFailedOperator);
                break;
            case CLOSED:
                //transfer
                IPair closedContent = event.getContent();
                ISession<C> session = closedContent.second();
                IOperator<Void, ISession<C>, Void> closedOperator = event.getEventOp();
                if (!session.isClosed()) {
                    error(_LinkIoPipe, event.getErrorType(), new Pair<>(null, session), closedOperator);
                }
                break;
            case NO_ERROR: {
                switch (event.getEventType()) {
                    case CONNECTED:
                        IPair connectContent = event.getContent();
                        IConnectionContext<C> context = connectContent.first();
                        AsynchronousSocketChannel channel = connectContent.second();
                        IOperator<IConnectionContext, AsynchronousSocketChannel, ITriple> connectOperator = event.getEventOp();
                        publish(_LinkIoPipe, CONNECTED, new Pair<>(context, channel), connectOperator);
                        break;
                    case READ:
                        IPair readContent = event.getContent();
                        publish(_DecoderPipe, TRANSFER, readContent, event.getEventOp());
                        break;
                    case WROTE:
                        IPair wroteContent = event.getContent();
                        publish(_WrotePipe, WROTE, wroteContent, event.getEventOp());
                        break;
                    case CLOSE:
                        IOperator<Void, ISession<C>, Void> closeOperator = event.getEventOp();
                        IPair closeContent = event.getContent();
                        session = closeContent.second();
                        if (!session.isClosed()) {
                            error(_ErrorPipe, CLOSED, closeContent, closeOperator);
                        }
                        break;
                    default:
                        _Log.warning(String.format(" wrong type %s in ClientIoDispatcher", event.getEventType()));
                        break;
                }
            }
                break;
            default:
                //convert & transfer
                IPair errorContent = event.getContent();
                IOperator<Throwable, ISession<C>, ITriple> errorOperator = event.getEventOp();
                session = errorContent.second();
                if (!session.isClosed()) {
                    throwable = errorContent.first();
                    ITriple transferResult = errorOperator.handle(throwable, session);
                    error(_LinkIoPipe, event.getErrorType(), new Pair<>(transferResult.first(), session), transferResult.third());
                }
                break;
        }
        event.reset();
    }
}
