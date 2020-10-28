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

import static com.isahl.chess.queen.event.inf.IError.Type.CONNECT_FAILED;
import static com.isahl.chess.queen.event.inf.IOperator.Type.WRITE;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;

import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.event.inf.IOperator;
import com.isahl.chess.queen.event.processor.QEvent;
import com.isahl.chess.queen.io.core.inf.IConnectActivity;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionDismiss;
import com.lmax.disruptor.EventHandler;

/**
 * @author william.d.zk
 */
public class ClientLinkHandler
        implements
        EventHandler<QEvent>
{
    private final Logger _Logger = Logger.getLogger("io.queen.processor." + getClass().getSimpleName());

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        if (event.hasError()) {
            if (event.getErrorType() == CONNECT_FAILED) {
                /* 多路连接不执行 retry */
                event.ignore();
            }
            else {
                _Logger.warning("client io error , do close session");
                IPair errorContent = event.getContent();
                ISession session = errorContent.getSecond();
                ISessionDismiss dismiss = session.getDismissCallback();
                IOperator<Void,
                          ISession,
                          Void> closeOperator = event.getEventOp();
                if (!session.isClosed()) {
                    closeOperator.handle(null, session);
                    dismiss.onDismiss(session);
                }
            }
        }
        else {
            if (event.getEventType() == IOperator.Type.CONNECTED) {
                try {
                    IPair connectedContent = event.getContent();
                    IConnectActivity connectActivity = connectedContent.getFirst();
                    AsynchronousSocketChannel channel = connectedContent.getSecond();
                    IOperator<IConnectActivity,
                              AsynchronousSocketChannel,
                              ITriple> connectedOperator = event.getEventOp();
                    ITriple connectedHandled = connectedOperator.handle(connectActivity, channel);
                    // connectedHandled 不可能为 null
                    IControl[] waitToSend = connectedHandled.getFirst();
                    ISession session = connectedHandled.getSecond();
                    IOperator<IControl[],
                              ISession,
                              List<ITriple>> sendTransferOperator = connectedHandled.getThird();
                    event.produce(WRITE, new Pair<>(waitToSend, session), sendTransferOperator);
                    _Logger.debug(String.format("link mappingHandle %s,connected", session));
                }
                catch (Exception e) {
                    _Logger.fetal("client session create failed", e);
                }
            }
            else {
                _Logger.warning(String.format("client link mappingHandle can't mappingHandle %s",
                                              event.getEventType()));
            }
        }
    }
}
