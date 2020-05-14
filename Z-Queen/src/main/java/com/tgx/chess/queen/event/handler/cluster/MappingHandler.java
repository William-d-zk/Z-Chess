/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.tgx.chess.queen.event.handler.cluster;

import static com.tgx.chess.queen.event.inf.IError.Type.MAPPING_ERROR;
import static com.tgx.chess.queen.event.inf.IOperator.Type.NOTIFY;
import static com.tgx.chess.queen.event.inf.IOperator.Type.WRITE;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;
import java.util.Objects;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.topology.ZUID;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.IPipeEventHandler;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IAioConnector;
import com.tgx.chess.queen.io.core.inf.IAioServer;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IConsistentNotify;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.inf.ISessionManager;
import com.tgx.chess.queen.io.core.inf.ITraceable;

/**
 * @author william.d.zk
 * @date 2020/2/15
 */
public class MappingHandler<C extends IContext<C>,
                            T extends IStorage>
        implements
        IPipeEventHandler<QEvent>
{
    private final Logger               _Logger;
    private final RingBuffer<QEvent>   _Error;
    private final RingBuffer<QEvent>   _Writer;
    private final RingBuffer<QEvent>[] _Notifiers;
    private final ISessionManager<C>   _SessionManager;
    private final IConsistentCustom    _ConsistentCustom;
    private final IClusterCustom<C,
                                 T>    _ClusterCustom;
    private final int                  _NotifyModMask;

    public MappingHandler(String mapper,
                          ISessionManager<C> manager,
                          RingBuffer<QEvent> error,
                          RingBuffer<QEvent> writer,
                          RingBuffer<QEvent>[] notifiers,
                          IConsistentCustom consistentCustom,
                          IClusterCustom<C,
                                         T> clusterCustom)
    {
        _Logger = Logger.getLogger("io.queen.dispatcher." + mapper);
        _SessionManager = manager;
        _Writer = writer;
        _Error = error;
        _Notifiers = notifiers;
        _ConsistentCustom = consistentCustom;
        _ClusterCustom = clusterCustom;
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
                              IAioServer<C>,
                              Void> acceptFailedOperator = event.getEventOp();
                    IPair errorContent = event.getContent();
                    acceptFailedOperator.handle(errorContent.getFirst(), errorContent.getSecond());
                    break;
                case CONNECT_FAILED:
                    IOperator<Throwable,
                              IAioConnector<C>,
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
                              ISession<C>,
                              Void> closeOperator = event.getEventOp();
                    errorContent = event.getContent();
                    ISession<C> session = errorContent.getSecond();
                    ISessionDismiss<C> dismiss = session.getDismissCallback();
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
                    IAioConnector<C> connector = connected.getFirst();
                    IOperator<IConnectActivity<C>,
                              AsynchronousSocketChannel,
                              ITriple> connectedOperator = event.getEventOp();
                    ITriple handled = connectedOperator.handle(connector, channel);
                    boolean success = handled.getFirst();
                    if (success) {
                        ISession<C> session = handled.getSecond();
                        IControl<C>[] toSends = handled.getThird();
                        if (toSends != null) {
                            publish(_Writer,
                                    WRITE,
                                    new Pair<>(toSends, session),
                                    session.getContext()
                                           .getSort()
                                           .getTransfer());
                        }
                    }
                    else {
                        Throwable throwable = handled.getThird();
                        _Logger.warning("session connect create failed ,channel error %", throwable, channel);
                        if (handled.getSecond() instanceof AsynchronousSocketChannel) {
                            connector.error();
                        }
                        else {
                            ISession<C> session = handled.getSecond();
                            session.innerClose();
                        }
                    }
                    break;
                case ACCEPTED:
                    connected = event.getContent();
                    channel = connected.getSecond();
                    IAioServer<C> server = connected.getFirst();
                    connectedOperator = event.getEventOp();
                    handled = connectedOperator.handle(server, channel);
                    success = handled.getFirst();
                    if (success) {
                        ISession<C> session = handled.getSecond();
                        IControl<C>[] toSends = handled.getThird();
                        if (toSends != null) {
                            publish(_Writer,
                                    WRITE,
                                    new Pair<>(toSends, session),
                                    session.getContext()
                                           .getSort()
                                           .getTransfer());
                        }
                    }
                    else {
                        Throwable throwable = handled.getThird();
                        _Logger.warning("session accept create failed ,channel error %", throwable, channel);
                        if (handled.getSecond() instanceof ISession) {
                            ISession<C> session = handled.getSecond();
                            session.innerClose();
                        }
                    }
                    break;
                case CLUSTER:
                    IControl<C> received = event.getContent()
                                                .getFirst();
                    ISession<C> session = event.getContent()
                                               .getSecond();
                    if (received == null) { return; }
                    try {
                        IPair pair = _ClusterCustom.handle(_SessionManager, session, received);
                        if (pair == null) return;
                        IControl<C>[] toSends = pair.getFirst();
                        if (toSends != null && toSends.length > 0) {
                            publish(_Writer,
                                    WRITE,
                                    new Pair<>(toSends, session),
                                    session.getContext()
                                           .getSort()
                                           .getTransfer());
                        }
                        IConsistentNotify notify = pair.getSecond();
                        if (notify != null) {
                            if (notify.byLeader()) _ConsistentCustom.adjudge(notify);
                            if (notify.doNotify()) publishNotify(notify, null);
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
                                          e);
                        }
                    }
                    else {
                        _ConsistentCustom.adjudge(event.getContent()
                                                       .getFirst());
                        publishNotify(event.getContent()
                                           .getFirst(),
                                      null);
                    }
                    break;
                case CLUSTER_TIMER://ClusterConsumer Timeout->start_vote
                    /*TIMER 必然是单个IControl,通过前项RingBuffer 向MappingHandler 投递*/
                    T content = event.getContent()
                                     .getFirst();
                    List<ITriple> toSends = _ClusterCustom.onTimer(_SessionManager, content);
                    if (toSends != null && !toSends.isEmpty()) {
                        publish(_Writer, toSends);
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

    private <E extends ITraceable> void publishNotify(E request, Throwable throwable)
    {
        Objects.requireNonNull(request);
        RingBuffer<QEvent> notifier = _Notifiers[(int) (request.getOrigin() >> ZUID.NODE_SHIFT) & _NotifyModMask];
        if (throwable == null) {
            publish(notifier, NOTIFY, new Pair<>(request, null), _ConsistentCustom);
        }
        else {
            error(notifier, MAPPING_ERROR, new Pair<>(request, throwable), _ConsistentCustom);
        }
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
