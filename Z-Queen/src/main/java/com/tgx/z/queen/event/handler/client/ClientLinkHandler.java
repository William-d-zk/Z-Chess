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

package com.tgx.z.queen.event.handler.client;

import static com.tgx.z.queen.event.inf.IError.Type.CONNECT_FAILED;
import static com.tgx.z.queen.event.inf.IOperator.Type.WRITE;

import java.nio.channels.AsynchronousSocketChannel;

import com.lmax.disruptor.EventHandler;
import com.tgx.z.queen.base.log.Logger;
import com.tgx.z.queen.base.util.Pair;
import com.tgx.z.queen.base.util.Triple;
import com.tgx.z.queen.event.inf.IOperator;
import com.tgx.z.queen.event.processor.QEvent;
import com.tgx.z.queen.io.core.inf.ICommand;
import com.tgx.z.queen.io.core.inf.IConnectionContext;
import com.tgx.z.queen.io.core.inf.ISession;
import com.tgx.z.queen.io.core.inf.ISessionDismiss;

public class ClientLinkHandler
        implements
        EventHandler<QEvent>
{
    private final Logger _Log = Logger.getLogger(getClass().getName());

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception {
        if (event.hasError()) {
            switch (event.getErrorType()) {
                case CONNECT_FAILED:
                    event.ignore();
                    break;
                case READ_FAILED:
                case CLOSED:

                    default:break;
            }

            if (event.getErrorType()
                     .equals(CONNECT_FAILED)) {

            }
            else {
                _Log.warning("io read error , transfer -> _ErrorEvent");
            }
        }
        else {
            switch (event.getEventType()) {
                case CONNECTED:
                    IOperator<IConnectionContext, AsynchronousSocketChannel> connectedOperator = event.getEventOp();
                    Pair<IConnectionContext, AsynchronousSocketChannel> connectedContent = event.getContent();
                    Triple<ICommand,
                           ISession,
                           IOperator<ICommand, ISession>> connectedHandled = connectedOperator.handle(connectedContent.first(),
                                                                                                      connectedContent.second());
                    //connectedHandled 不可能为 null
                    ICommand waitToSend = connectedHandled.first();
                    ISession session = connectedHandled.second();
                    IOperator<ICommand, ISession> sendOperator = connectedHandled.third();
                    event.produce(WRITE, waitToSend, session, sendOperator);
                    connectedContent.first()
                                    .getSessionCreated()
                                    .onCreate(session);
                    _Log.info(String.format("link handle %s,connected", session));
                    break;
                case CLOSE://Local Close
                    Pair<Throwable, ISession> closeContent = event.getContent();
                    session = closeContent.second();
                    _Log.info(String.format("link handle %s,close", session));
                    ISessionDismiss sessionDismiss = session.getDismissCallback();
                    sessionDismiss.onDismiss(session);
                    event.ignore();
                    break;
                default:
                    _Log.warning(String.format("link encodeHandler can't handle %s", event.getEventType()));
                    break;
            }
        }
    }
}
