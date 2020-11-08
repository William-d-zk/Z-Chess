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

package com.isahl.chess.queen.event.handler.cluster;

import static com.isahl.chess.queen.event.inf.IError.Type.MAPPING_ERROR;
import static com.isahl.chess.queen.event.inf.IOperator.Type.NOTIFY;
import static com.isahl.chess.queen.event.inf.IOperator.Type.WRITE;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;
import java.util.Objects;

import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.event.inf.IOperator;
import com.isahl.chess.queen.event.inf.IPipeEventHandler;
import com.isahl.chess.queen.event.processor.QEvent;
import com.isahl.chess.queen.io.core.inf.IAioConnector;
import com.isahl.chess.queen.io.core.inf.IAioServer;
import com.isahl.chess.queen.io.core.inf.IConnectActivity;
import com.isahl.chess.queen.io.core.inf.IConsistentNotify;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.IProtocol;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionDismiss;
import com.isahl.chess.queen.io.core.inf.ISessionManager;
import com.isahl.chess.queen.io.core.inf.ITraceable;
import com.lmax.disruptor.RingBuffer;

/**
 * @author william.d.zk
 * 
 * @date 2020/2/15
 */
public class MappingHandler<T extends IStorage>
        implements
        IPipeEventHandler<QEvent>
{
    private final Logger               _Logger;
    private final RingBuffer<QEvent>   _Error;
    private final RingBuffer<QEvent>   _Writer;
    private final RingBuffer<QEvent>[] _Notifiers;
    private final ISessionManager      _SessionManager;
    private final IClusterCustom<T>    _ClusterCustom;
    private final IConsistentCustom    _ConsistentCustom;
    private final int                  _NotifyModMask;

    public MappingHandler(String mapper,
                          ISessionManager manager,
                          RingBuffer<QEvent> error,
                          RingBuffer<QEvent> writer,
                          RingBuffer<QEvent>[] notifiers,
                          IClusterCustom<T> clusterCustom,
                          IConsistentCustom consistentCustom)
    {
        _Logger = Logger.getLogger("io.queen.dispatcher." + mapper);
        _SessionManager = manager;
        _Writer = writer;
        _Error = error;
        _Notifiers = notifiers;
        _ClusterCustom = clusterCustom;
        _ConsistentCustom = consistentCustom;
        _NotifyModMask = _Notifiers.length - 1;
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch)
    {
        if (event.hasError()) {
            _Logger.debug(String.format("error type %s,ignore ", event.getErrorType()));
            switch (event.getErrorType())
            {
                case ACCEPT_FAILED:
                    IOperator<Throwable,
                              IAioServer,
                              Void> acceptFailedOperator = event.getEventOp();
                    IPair errorContent = event.getContent();
                    acceptFailedOperator.handle(errorContent.getFirst(), errorContent.getSecond());
                    break;
                case CONNECT_FAILED:
                    IOperator<Throwable,
                              IAioConnector,
                              Void> connectFailedOperator = event.getEventOp();
                    errorContent = event.getContent();
                    connectFailedOperator.handle(errorContent.getFirst(), errorContent.getSecond());
                    break;
                case PASSIVE_CLOSE:
                case INITIATIVE_CLOSE:
                    _Logger.warning("mapping handle io error, %s",
                                    event.getErrorType()
                                         .getMsg());
                    IOperator<Void,
                              ISession,
                              Void> closeOperator = event.getEventOp();
                    errorContent = event.getContent();
                    ISession session = errorContent.getSecond();
                    ISessionDismiss dismiss = session.getDismissCallback();
                    boolean closed = session.isClosed();
                    closeOperator.handle(null, session);
                    if (!closed) {
                        dismiss.onDismiss(session);
                    }
                    break;
                default:
                    _Logger.warning("can't handle %s",
                                    event.getErrorType()
                                         .name());
                    break;
            }
        }
        else {
            switch (event.getEventType())
            {
                case CONNECTED:
                    IPair connected = event.getContent();
                    AsynchronousSocketChannel channel = connected.getSecond();
                    IAioConnector connector = connected.getFirst();
                    IOperator<IConnectActivity,
                              AsynchronousSocketChannel,
                              ITriple> connectedOperator = event.getEventOp();
                    ITriple handled = connectedOperator.handle(connector, channel);
                    boolean success = handled.getFirst();
                    if (success) {
                        ISession session = handled.getSecond();
                        IControl[] toSends = handled.getThird();
                        if (toSends != null) {
                            publish(_Writer, WRITE, new Pair<>(toSends, session), session.getTransfer());
                        }
                    }
                    else {
                        Throwable throwable = handled.getThird();
                        _Logger.warning("session connect create failed ,channel error %", throwable, channel);
                        if (handled.getSecond() instanceof AsynchronousSocketChannel) {
                            connector.error();
                        }
                        else {
                            ISession session = handled.getSecond();
                            session.innerClose();
                        }
                    }
                    break;
                case ACCEPTED:
                    connected = event.getContent();
                    channel = connected.getSecond();
                    IAioServer server = connected.getFirst();
                    connectedOperator = event.getEventOp();
                    handled = connectedOperator.handle(server, channel);
                    success = handled.getFirst();
                    if (success) {
                        ISession session = handled.getSecond();
                        IControl[] toSends = handled.getThird();
                        if (toSends != null) {
                            publish(_Writer, WRITE, new Pair<>(toSends, session), session.getTransfer());
                        }
                    }
                    else {
                        Throwable throwable = handled.getThird();
                        _Logger.warning("session accept create failed ,channel error %", throwable, channel);
                        if (handled.getSecond() instanceof ISession) {
                            ISession session = handled.getSecond();
                            session.innerClose();
                        }
                    }
                    break;
                case CLUSTER:
                    IControl received = event.getContent()
                                             .getFirst();
                    ISession session = event.getContent()
                                            .getSecond();
                    if (received == null) { return; }
                    try {
                        IPair pair = _ClusterCustom.handle(_SessionManager, session, received);
                        if (pair == null) return;
                        IControl[] toSends = pair.getFirst();
                        if (toSends != null && toSends.length > 0) {
                            publish(_Writer, WRITE, new Pair<>(toSends, session), session.getTransfer());
                        }
                        IConsistentNotify notify = pair.getSecond();
                        if (notify != null) {
                            if (notify.byLeader()) {
                                try {
                                    _ConsistentCustom.adjudge(notify);
                                }
                                catch (Throwable e) {
                                    _Logger.warning("leader - adjudge ", e);
                                }
                            }
                            if (notify.doNotify()) {
                                publishNotify(pair.getSecond(), null, _ConsistentCustom.getOperator());
                            }
                        }
                    }
                    catch (Exception e) {
                        _Logger.warning("cluster mapping handler error", e);
                        session.innerClose();
                    }
                    break;
                case CONSENSUS:
                    if (_ClusterCustom.waitForCommit()) {
                        try {
                            List<ITriple> broadcast = _ClusterCustom.consensus(_SessionManager,
                                                                               event.getContent()
                                                                                    .getFirst());
                            if (broadcast != null && !broadcast.isEmpty()) {
                                publish(_Writer, broadcast);
                            }
                        }
                        catch (Exception e) {
                            _Logger.warning("mapping consensus error", e);
                            publishNotify(event.getContent()
                                               .getFirst(),
                                          e,
                                          _ConsistentCustom.getOperator());
                        }
                    }
                    else {
                        _ConsistentCustom.adjudge(event.getContent()
                                                       .getFirst());
                        publishNotify(event.getContent()
                                           .getFirst(),
                                      null,
                                      _ConsistentCustom.getOperator());
                    }
                    break;
                case CLUSTER_TIMER:// ClusterConsumer Timeout->start_vote
                    /* TIMER 必然是单个IControl,通过前项RingBuffer 向MappingHandler 投递 */
                    T content = event.getContent()
                                     .getFirst();
                    List<ITriple> toSends = _ClusterCustom.onTimer(_SessionManager, content);
                    if (toSends != null && !toSends.isEmpty()) {
                        publish(_Writer, toSends);
                    }
                    else {

                    }
                    break;
                default:
                    _Logger.warning("mapping handler error %s",
                                    event.getEventType()
                                         .name());
                    break;
            }
        }
        event.reset();
    }

    private <E extends ITraceable & IProtocol> void publishNotify(E request,
                                                                  Throwable throwable,
                                                                  IOperator<E,
                                                                            Throwable,
                                                                            Void> operator)
    {
        Objects.requireNonNull(request);
        RingBuffer<QEvent> notifier = _Notifiers[(int) (request.getOrigin() >> ZUID.NODE_SHIFT) & _NotifyModMask];
        if (throwable == null) {
            publish(notifier, NOTIFY, new Pair<>(request, null), operator);
        }
        else {
            error(notifier, MAPPING_ERROR, new Pair<>(request, throwable), operator);
        }
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
