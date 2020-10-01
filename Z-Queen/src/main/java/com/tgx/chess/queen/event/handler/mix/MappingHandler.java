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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.queen.event.handler.mix;

import static com.tgx.chess.queen.event.inf.IOperator.Type.CONSENSUS;
import static com.tgx.chess.queen.event.inf.IOperator.Type.NOTIFY;
import static com.tgx.chess.queen.event.inf.IOperator.Type.WRITE;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.event.handler.cluster.IClusterCustom;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.IPipeEventHandler;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IAioConnector;
import com.tgx.chess.queen.io.core.inf.IAioServer;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IConsistent;
import com.tgx.chess.queen.io.core.inf.IConsistentNotify;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.inf.ISessionManager;
import com.tgx.chess.queen.io.core.manager.MixManager;

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
    private final RingBuffer<QEvent>   _Transfer;
    private final ISessionManager<C>   _SessionManager;
    private final ILinkCustom<C>       _LinkCustom;
    private final IClusterCustom<C,
                                 T>    _ClusterCustom;

    public MappingHandler(String mapper,
                          MixManager<C> manager,
                          RingBuffer<QEvent> error,
                          RingBuffer<QEvent> writer,
                          RingBuffer<QEvent> transfer,
                          ILinkCustom<C> linkCustom,
                          IClusterCustom<C,
                                         T> clusterCustom)
    {
        _Logger = Logger.getLogger("io.queen.dispatcher." + mapper);
        _SessionManager = manager;
        _Writer = writer;
        _Error = error;
        _Transfer = transfer;
        _LinkCustom = linkCustom;
        _ClusterCustom = clusterCustom;
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch)
    {
        if (event.hasError()) {
            _Logger.warning("mapping error → %s ", event);
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
                    _Logger.warning("mapping handle io error,%s",
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
                    break;
            }
        }
        else {
            _Logger.trace("mapping:%s", event);
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
                case LINK:
                    IControl<C> received = event.getContent()
                                                .getFirst();
                    ISession<C> session = event.getContent()
                                               .getSecond();
                    if (received == null) { return; }
                    try {
                        IPair pair = _LinkCustom.handle(_SessionManager, session, received);
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
                        IConsistent transfer = pair.getSecond();
                        if (transfer != null) {
                            if (_ClusterCustom.waitForCommit()) {
                                publish(_Transfer,
                                        CONSENSUS,
                                        new Pair<>(pair.getSecond(), session),
                                        _LinkCustom.getOperator());
                            }
                            else {
                                List<ITriple> result = _LinkCustom.notify(_SessionManager,
                                                                          pair.getSecond(),
                                                                          transfer.getOrigin());
                                if (result != null && !result.isEmpty()) {
                                    publish(_Writer, result);
                                }
                            }
                        }
                    }
                    catch (Exception e) {
                        _Logger.warning("link mapping handler error", e);
                        session.innerClose();
                    }
                    break;
                case CLUSTER:
                    received = event.getContent()
                                    .getFirst();
                    session = event.getContent()
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
                        IConsistentNotify transferNotify = pair.getSecond();
                        if (transferNotify != null && transferNotify.doNotify()) {
                            publish(_Transfer, NOTIFY, new Pair<>(transferNotify, null), _LinkCustom.getOperator());
                        }
                    }
                    catch (Exception e) {
                        _Logger.warning("cluster mapping handler error", e);
                        session.innerClose();
                    }
                    break;
                case CONSENSUS:
                    session = event.getContent()
                                   .getSecond();
                    try {
                        List<ITriple> result = _ClusterCustom.consensus(_SessionManager,
                                                                        event.getContent()
                                                                             .getFirst());
                        if (result != null && !result.isEmpty()) {
                            publish(_Writer, result);
                        }
                    }
                    catch (Exception e) {
                        _Logger.warning("mapping consensus error, link session close", e);
                        session.innerClose();
                    }
                    break;
                case NOTIFY:
                    IConsistentNotify notify = event.getContent()
                                                    .getFirst();
                    if (notify != null) try {
                        if (notify.byLeader()) {
                            try {
                                _LinkCustom.adjudge(notify);
                            }
                            catch (Throwable e) {
                                _Logger.warning("leader adjudge", e);
                            }
                        }
                        if (notify.doNotify()) {
                            List<ITriple> result = _LinkCustom.notify(_SessionManager,
                                                                      event.getContent()
                                                                           .getFirst(),
                                                                      notify.getOrigin());
                            if (result != null && !result.isEmpty()) {
                                publish(_Writer, result);
                            }
                        }
                    }
                    catch (Exception e) {
                        _Logger.warning("mapping notify error, cluster's session keep alive", e);
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

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
