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

import static com.tgx.chess.queen.event.inf.IError.Type.CONNECT_FAILED;
import static com.tgx.chess.queen.event.inf.IOperator.Type.WRITE;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;

import com.lmax.disruptor.EventHandler;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;

/**
 * @author william.d.zk
 */
public class ClientLinkHandler<C extends IContext<C>>
        implements
        EventHandler<QEvent>
{
    private final Logger _Logger = Logger.getLogger(getClass().getName());

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        if (event.hasError()) {
            if (event.getErrorType()
                     .equals(CONNECT_FAILED))
            {
                event.ignore();
            }
            else {
                _Logger.warning("client io error , do close session");
                IPair errorContent = event.getContent();
                ISession<C> session = errorContent.second();
                ISessionDismiss<C> dismiss = session.getDismissCallback();
                IOperator<Void,
                          ISession<C>,
                          Void> closeOperator = event.getEventOp();
                if (!session.isClosed()) {
                    closeOperator.handle(null, session);
                    dismiss.onDismiss(session);
                }
            }
        }
        else {
            switch (event.getEventType())
            {
                case CONNECTED:
                    IPair connectedContent = event.getContent();
                    IConnectActivity<C> connectActivity = connectedContent.first();
                    AsynchronousSocketChannel channel = connectedContent.second();
                    IOperator<IConnectActivity<C>,
                              AsynchronousSocketChannel,
                              ITriple> connectedOperator = event.getEventOp();
                    ITriple connectedHandled = connectedOperator.handle(connectActivity, channel);
                    //connectedHandled 不可能为 null
                    IControl<C>[] waitToSend = connectedHandled.first();
                    ISession<C> session = connectedHandled.second();
                    IOperator<IControl<C>[],
                              ISession,
                              List<ITriple>> sendTransferOperator = connectedHandled.third();
                    event.produce(WRITE, new Pair<>(waitToSend, session), sendTransferOperator);
                    _Logger.debug(String.format("link mappingHandle %s,connected", session));
                    break;
                default:
                    _Logger.warning(String.format("client link mappingHandle can't mappingHandle %s",
                                                  event.getEventType()));
                    break;
            }
        }
    }
}
