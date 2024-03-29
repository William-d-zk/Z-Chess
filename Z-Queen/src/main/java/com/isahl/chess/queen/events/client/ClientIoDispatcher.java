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

package com.isahl.chess.queen.events.client;

import com.isahl.chess.king.base.disruptor.components.Health;
import com.isahl.chess.king.base.disruptor.features.debug.IHealth;
import com.isahl.chess.king.base.disruptor.features.flow.IPipeHandler;
import com.isahl.chess.king.base.disruptor.features.functions.IBinaryOperator;
import com.isahl.chess.king.base.disruptor.features.functions.OperateType;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.config.QueenCode;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.io.core.features.model.channels.IConnectActivity;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.lmax.disruptor.RingBuffer;

import java.nio.channels.AsynchronousSocketChannel;

import static com.isahl.chess.king.base.features.IError.Type.INITIATIVE_CLOSE;
import static com.isahl.chess.king.base.features.IError.Type.PASSIVE_CLOSE;

/**
 * @author william.d.zk
 */
public class ClientIoDispatcher
        implements IPipeHandler<QEvent>
{
    private final Logger             _Logger = Logger.getLogger("io.queen.dispatcher." + getClass().getName());
    private final RingBuffer<QEvent> _Reader;
    private final RingBuffer<QEvent> _Wrote;
    private final RingBuffer<QEvent> _Error;
    private final IHealth            _Health = new Health(-1);

    public ClientIoDispatcher(RingBuffer<QEvent> readerPipe, RingBuffer<QEvent> wrotePipe, RingBuffer<QEvent> errorPipe)
    {

        _Reader = readerPipe;
        _Wrote = wrotePipe;
        _Error = errorPipe;
    }

    @Override
    public IHealth _Health()
    {
        return _Health;
    }

    @Override
    public void onEvent(QEvent event, long sequence) throws Exception
    {
        // convert & transfer
        switch(event.getErrorType()) {
            case CONNECT_FAILED -> {
                IPair connectFailedContent = event.getComponent();
                Throwable throwable = connectFailedContent.getFirst();
                IConnectActivity connectActive = connectFailedContent.getSecond();
                IBinaryOperator<Throwable, IConnectActivity, ITriple> connectFailedOperator = event.getEventBinaryOp();
                error(_Reader, event.getErrorType(), new Pair<>(throwable, connectActive), connectFailedOperator);
            }
            case NO_ERROR -> {
                switch(event.getEventType()) {
                    case CONNECTED, ACCEPTED -> {
                        IPair connectContent = event.getComponent();
                        IConnectActivity connectActivity = connectContent.getFirst();
                        AsynchronousSocketChannel channel = connectContent.getSecond();
                        IBinaryOperator<IConnectActivity, AsynchronousSocketChannel, ITriple> connectOperator = event.getEventBinaryOp();
                        publish(_Reader, event.getEventType(), new Pair<>(connectActivity, channel), connectOperator);
                    }
                    case READ -> {
                        IPair readContent = event.getComponent();
                        publish(_Reader, OperateType.DECODE, readContent, event.getEventBinaryOp());
                    }
                    case WROTE -> {
                        IPair wroteContent = event.getComponent();
                        publish(_Wrote, OperateType.WROTE, wroteContent, event.getEventBinaryOp());
                    }
                    case LOCAL_CLOSE -> {
                        IBinaryOperator<Void, ISession, Void> closeOperator = event.getEventBinaryOp();
                        IPair closeContent = event.getComponent();
                        ISession session = closeContent.getSecond();
                        if(!session.isClosed()) {
                            error(_Reader, INITIATIVE_CLOSE, closeContent, closeOperator);
                        }
                    }
                    default -> _Logger.warning(String.format(" wrong type %s in ClientIoDispatcher",
                                                             event.getEventType()));
                }
            }
            case READ_EOF -> {
                IPair errorContent = event.getComponent();
                IBinaryOperator<Throwable, ISession, IPair> errorOperator = event.getEventBinaryOp();
                ISession session = errorContent.getSecond();
                Throwable throwable = errorContent.getFirst();
                if(!session.isClosed()) {
                    IPair transferResult = errorOperator.handle(throwable, session);
                    error(_Reader,
                          PASSIVE_CLOSE,
                          new Pair<>(QueenCode.ERROR_CLOSE, session),
                          transferResult.getSecond());
                }
            }
            case PASSIVE_CLOSE, INITIATIVE_CLOSE -> {
                IPair errorContent = event.getComponent();
                String msg = errorContent.getFirst();
                ISession session = errorContent.getSecond();
                _Logger.warning("closed: [%s] -> %s", session.isClosed(), session);
            }
            default -> {
                _Logger.warning(" default no handle %s", event);
            }
        }
    }

    @Override
    public Logger _Logger()
    {
        return _Logger;
    }
}
