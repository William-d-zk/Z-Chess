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

import static com.tgx.chess.queen.event.inf.IError.Type.LINK_ERROR;
import static com.tgx.chess.queen.event.inf.IError.Type.LINK_LOGIN_ERROR;
import static com.tgx.chess.queen.event.inf.IOperator.Type.WRITE;

import java.nio.channels.AsynchronousSocketChannel;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.exception.LinkRejectException;
import com.tgx.chess.king.base.exception.ZException;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IError;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.IPipeEventHandler;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.manager.QueenManager;

/**
 * @author William.d.zk
 */
public class LinkHandler<C extends IContext<C>>
        implements
        IPipeEventHandler<QEvent>
{
    private final Logger             _Logger = Logger.getLogger(getClass().getName());
    private final RingBuffer<QEvent> _Error;
    private final RingBuffer<QEvent> _Writer;
    private final QueenManager<C>    _QueenManager;
    private final ILinkHandler<C>    _LinkHandler;

    public LinkHandler(QueenManager<C> manager,
                       RingBuffer<QEvent> error,
                       RingBuffer<QEvent> writer,
                       ILinkHandler<C> linkHandler)
    {
        _Error = error;
        _Writer = writer;
        _QueenManager = manager;
        _LinkHandler = linkHandler;
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch)
    {
        if (event.hasError()) {
            switch (event.getErrorType())
            {
                case ACCEPT_FAILED:
                case CONNECT_FAILED:
                    _Logger.info(String.format("error type %s,ignore ", event.getErrorType()));
                    event.ignore();
                    break;
                default:
                    _Logger.warning("server io error , do close session");
                    IOperator<Void,
                              ISession<C>,
                              Void> closeOperator = event.getEventOp();
                    IPair errorContent = event.getContent();
                    ISession<C> session = errorContent.second();
                    ISessionDismiss<C> dismiss = session.getDismissCallback();
                    boolean closed = session.isClosed();
                    closeOperator.handle(null, session);
                    if (!closed) {
                        dismiss.onDismiss(session);
                    }
            }
        }
        else {
            switch (event.getEventType())
            {
                case CONNECTED:
                    try {
                        IPair connectedContent = event.getContent();
                        AsynchronousSocketChannel channel = connectedContent.second();
                        IConnectActivity<C> connectActivity = connectedContent.first();
                        IOperator<IConnectActivity<C>,
                                  AsynchronousSocketChannel,
                                  ITriple> connectedOperator = event.getEventOp();
                        ITriple connectedHandled = connectedOperator.handle(connectActivity, channel);
                        /*connectedHandled 不可能为 null*/
                        ISession<C> session = connectedHandled.second();
                        publish(_Writer,
                                WRITE,
                                new Pair<>(connectedHandled.first(), session),
                                connectedHandled.third());
                    }
                    catch (Exception e) {
                        _Logger.warning("link failed", e);
                        //connection create failed! ignore exception
                    }
                    break;
                case LOGIC:
                    IPair logicContent = event.getContent();
                    ISession<C> session = logicContent.second();
                    try {
                        _LinkHandler.handle(this, _QueenManager, event);
                    }
                    catch (LinkRejectException e) {
                        error(LINK_LOGIN_ERROR,
                              e,
                              session,
                              session.getContext()
                                     .getSort()
                                     .getError());
                    }
                    catch (ZException e) {
                        error(LINK_ERROR,
                              e,
                              session,
                              session.getContext()
                                     .getSort()
                                     .getError());
                    }
                    break;
                default:
                    break;
            }
        }
        event.reset();
    }

    public void error(IError.Type type,
                      Throwable e,
                      ISession<C> session,
                      IOperator<Throwable,
                                ISession<C>,
                                ITriple> errorOperator)
    {
        error(_Error, type, new Pair<>(e, session), errorOperator);
    }

    public void write(IControl[] waitToSends, ISession<C> session)
    {
        publish(_Writer,
                WRITE,
                new Pair<>(waitToSends, session),
                session.getContext()
                       .getSort()
                       .getTransfer());
    }

}