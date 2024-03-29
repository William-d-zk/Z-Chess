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

import com.isahl.chess.king.base.disruptor.features.flow.IPipeHandler;
import com.isahl.chess.king.base.disruptor.features.functions.IBinaryOperator;
import com.isahl.chess.king.base.disruptor.features.functions.OperateType;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.events.pipe.DecodeHandler;
import com.isahl.chess.queen.io.core.features.model.session.IDismiss;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.zls.IEncryptor;
import com.isahl.chess.queen.io.core.net.socket.features.IAioConnection;

import java.nio.channels.AsynchronousSocketChannel;

import static com.isahl.chess.king.base.disruptor.features.functions.OperateType.DISPATCH;
import static com.isahl.chess.king.base.disruptor.features.functions.OperateType.WRITE;
import static com.isahl.chess.king.base.features.IError.Type.CONNECT_FAILED;

/**
 * @author william.d.zk
 */
public class ClientReaderHandler
        extends DecodeHandler
        implements IPipeHandler<QEvent>
{

    public ClientReaderHandler(IEncryptor encryptHandler, int slot)
    {
        super(encryptHandler, slot);
    }

    @Override
    public void onEvent(QEvent event, long sequence) throws Exception
    {
        if(event.hasError()) {
            if(event.getErrorType() == CONNECT_FAILED) {
                /* 多路连接不执行 retry */
                event.ignore();
            }
            else {
                _Logger.warning("client io error , do close session");
                IPair errorContent = event.getComponent();
                ISession session = errorContent.getSecond();
                IDismiss dismiss = session.getDismissCallback();
                IBinaryOperator<Void, ISession, Void> closeOperator = event.getEventBinaryOp();
                if(!session.isClosed()) {
                    closeOperator.handle(null, session);
                    dismiss.onDismiss(session);
                }
            }
        }
        else {
            if(event.getEventType() == OperateType.CONNECTED) {
                try {
                    IPair connected = event.getComponent();
                    IAioConnection connection = connected.getFirst();
                    AsynchronousSocketChannel channel = connected.getSecond();
                    IBinaryOperator<IAioConnection, AsynchronousSocketChannel, ITriple> connectedOperator = event.getEventBinaryOp();
                    ITriple handled = connectedOperator.handle(connection, channel);
                    boolean success = handled.getFirst();
                    if(success) {
                        ISession session = handled.getSecond();
                        ITriple result = handled.getThird();
                        if(result != null) {
                            OperateType type = result.getThird();
                            switch(type) {
                                case SINGLE -> event.produce(WRITE,
                                                             new Pair<>(result.getFirst(), session),
                                                             session.encoder());
                                case BATCH -> event.produce(DISPATCH, result.getFirst());
                            }
                            return;
                        }
                    }
                    else {
                        Throwable throwable = handled.getThird();
                        if(handled.getSecond() instanceof ISession session) {
                            session.innerClose();
                        }
                        connection.error();
                        _Logger.warning("session create failed %s", throwable, connection);
                    }
                }
                catch(Exception e) {
                    _Logger.fetal("client session create failed", e);
                }
                event.ignore();
            }
            else {
                super.onEvent(event, sequence);
            }
        }
    }

    @Override
    public Logger _Logger()
    {
        return _Logger;
    }
}
