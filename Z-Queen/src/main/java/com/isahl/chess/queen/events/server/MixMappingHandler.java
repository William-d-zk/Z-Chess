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

package com.isahl.chess.queen.events.server;

import com.isahl.chess.king.base.disruptor.components.Health;
import com.isahl.chess.king.base.disruptor.features.debug.IHealth;
import com.isahl.chess.king.base.disruptor.features.flow.IPipeHandler;
import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.features.IError;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.db.model.IStorage;
import com.isahl.chess.queen.events.cluster.IClusterCustom;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;
import com.isahl.chess.queen.io.core.features.model.channels.IConnectActivity;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.ISessionDismiss;
import com.isahl.chess.queen.io.core.features.model.session.ISessionManager;
import com.isahl.chess.queen.io.core.example.MixManager;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.net.socket.features.IAioConnector;
import com.isahl.chess.queen.io.core.net.socket.features.server.IAioServer;
import com.lmax.disruptor.RingBuffer;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;

/**
 * @author william.d.zk
 * @date 2020/2/15
 */
public class MixMappingHandler<T extends IStorage>
        implements IPipeHandler<QEvent>
{
    private final Logger             _Logger;
    private final RingBuffer<QEvent> _Error;
    private final RingBuffer<QEvent> _Writer;
    private final RingBuffer<QEvent> _Transfer;
    private final ISessionManager    _SessionManager;
    private final ILinkCustom        _LinkCustom;
    private final IClusterCustom<T>  _ClusterCustom;
    private final IHealth            _Health = new Health(-1);

    public MixMappingHandler(String mapper,
                             MixManager manager,
                             RingBuffer<QEvent> error,
                             RingBuffer<QEvent> writer,
                             RingBuffer<QEvent> transfer,
                             ILinkCustom linkCustom,
                             IClusterCustom<T> clusterCustom)
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
    public IHealth getHealth()
    {
        return _Health;
    }

    @Override
    public void onEvent(QEvent event, long sequence)
    {
        if(event.hasError()) {
            _Logger.warning("mapping error → %s ", event);
            switch(event.getErrorType()) {
                case ACCEPT_FAILED:
                    IOperator<Throwable, IAioServer, Void> acceptFailedOperator = event.getEventOp();
                    IPair errorContent = event.getContent();
                    acceptFailedOperator.handle(errorContent.getFirst(), errorContent.getSecond());
                    break;
                case CONNECT_FAILED:
                    IOperator<Throwable, IAioConnector, Void> connectFailedOperator = event.getEventOp();
                    errorContent = event.getContent();
                    connectFailedOperator.handle(errorContent.getFirst(), errorContent.getSecond());
                    break;
                case PASSIVE_CLOSE:
                case INITIATIVE_CLOSE:
                    _Logger.warning("mapping handle io error,%s",
                                    event.getErrorType()
                                         .getMsg());
                    IOperator<Void, ISession, Void> closeOperator = event.getEventOp();
                    errorContent = event.getContent();
                    ISession session = errorContent.getSecond();
                    ISessionDismiss dismiss = session.getDismissCallback();
                    boolean closed = session.isClosed();
                    closeOperator.handle(null, session);
                    if(!closed) {
                        dismiss.onDismiss(session);
                        _LinkCustom.close(session);
                    }
                    break;
                default:
                    break;
            }
        }
        else {
            _Logger.trace("mapping:%s", event);
            switch(event.getEventType()) {
                case CONNECTED:
                    IPair connected = event.getContent();
                    AsynchronousSocketChannel channel = connected.getSecond();
                    IAioConnector connector = connected.getFirst();
                    IOperator<IConnectActivity, AsynchronousSocketChannel, ITriple> connectedOperator = event.getEventOp();
                    ITriple handled = connectedOperator.handle(connector, channel);
                    boolean success = handled.getFirst();
                    if(success) {
                        ISession session = handled.getSecond();
                        IControl[] toSends = handled.getThird();
                        if(toSends != null) {
                            publish(_Writer, IOperator.Type.WRITE, new Pair<>(toSends, session), session.getTransfer());
                        }
                    }
                    else {
                        Throwable throwable = handled.getThird();
                        _Logger.warning("session connect create failed ,channel error %", throwable, channel);
                        if(handled.getSecond() instanceof AsynchronousSocketChannel) {
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
                    if(success) {
                        ISession session = handled.getSecond();
                        IControl[] toSends = handled.getThird();
                        if(toSends != null) {
                            publish(_Writer, IOperator.Type.WRITE, new Pair<>(toSends, session), session.getTransfer());
                        }
                    }
                    else {
                        Throwable throwable = handled.getThird();
                        _Logger.warning("session accept create failed ,channel error %", throwable, channel);
                        if(handled.getSecond() instanceof ISession) {
                            ISession session = handled.getSecond();
                            session.innerClose();
                        }
                    }
                    break;
                case LINK:
                    IControl received = event.getContent()
                                             .getFirst();
                    ISession session = event.getContent()
                                            .getSecond();
                    if(received != null && session != null) {
                        try {
                            IPair result = _LinkCustom.handle(_SessionManager, session, received);
                            if(result == null) {return;}
                            IControl[] toSends = result.getFirst();
                            IConsistent consistent = result.getSecond();
                            if(toSends != null && toSends.length > 0) {
                                publish(_Writer,
                                        IOperator.Type.WRITE,
                                        new Pair<>(toSends, session),
                                        session.getTransfer());
                            }
                            if(consistent != null) {
                                if(_ClusterCustom.waitForCommit()) {
                                    publish(_Transfer,
                                            IOperator.Type.CONSISTENCY,
                                            new Pair<>(consistent, session),
                                            _ClusterCustom.getOperator());
                                }
                                else {
                                    publish(_Writer,
                                            _LinkCustom.notify(_SessionManager,
                                                               result.getSecond(),
                                                               consistent.getOrigin()));
                                }
                            }
                        }
                        catch(Exception e) {
                            _Logger.warning("link mapping handler error", e);
                            session.innerClose();
                        }
                    }
                    break;
                case CLUSTER:
                    received = event.getContent()
                                    .getFirst();
                    session = event.getContent()
                                   .getSecond();
                    if(received != null && session != null) {
                        try {
                            IPair result = _ClusterCustom.handle(_SessionManager, session, received);
                            if(result == null) {return;}
                            IControl[] toSends = result.getFirst();
                            if(toSends != null && toSends.length > 0) {
                                publish(_Writer,
                                        IOperator.Type.WRITE,
                                        new Pair<>(toSends, session),
                                        session.getTransfer());
                            }
                            IConsistent adjudge = result.getSecond();
                            if(adjudge != null) {
                                publish(_Transfer,
                                        IOperator.Type.CONSISTENT_RESULT,
                                        new Pair<>(adjudge, session),
                                        _LinkCustom.getOperator());
                            }
                        }
                        catch(Exception e) {
                            _Logger.warning("cluster mapping handler error", e);
                            session.innerClose();
                        }
                    }
                    break;
                case CONSISTENCY://Cluster Processor
                    /*
                        core.LinkProcessor → core._ClusterEvent → core.ClusterProcessor → _ClusterCustom
                    */
                    IConsistent request = event.getContent()
                                               .getFirst();
                    session = event.getContent()
                                   .getSecond();
                    IOperator<IConsistent, ISession, IPair> consistencyOp = event.getEventOp();
                    try {
                        List<ITriple> contents = _ClusterCustom.consistent(_SessionManager, request);
                        _Logger.debug("consistency request: %s ; cluster: %s ", request, contents);
                        if(contents != null) {publish(_Writer, contents);}
                        else {
                            IPair resp = consistencyOp.handle(request, session);
                            error(_Error, IError.Type.CONSISTENCY_REJECT, resp, session.getError());
                        }
                    }
                    catch(Exception e) {
                        _Logger.warning("mapping consensus error, link session close", e);
                    }
                    break;
                case CLUSTER_TOPOLOGY:
                    /*
                        core._ConsensusApiEvent → core.ClusterProcessor → _ClusterCustom
                     */
                    try {
                        publish(_Writer,
                                _ClusterCustom.changeTopology(_SessionManager,
                                                              event.getContent()
                                                                   .getFirst()));
                    }
                    catch(Exception e) {
                        _Logger.warning("cluster inner service api ");
                    }
                    break;
                case CONSISTENT_RESULT:
                    IConsistent consistency = event.getContent()
                                                   .getFirst();
                    session = event.getContent()
                                   .getSecond();
                    IOperator<IConsistent, ISession, IControl> adjudgeOperator = event.getEventOp();
                    if(consistency != null) {
                        try {
                            publish(_Writer,
                                    _LinkCustom.notify(_SessionManager,
                                                       adjudgeOperator.handle(consistency, session),
                                                       consistency.getOrigin()));
                        }
                        catch(Exception e) {
                            _Logger.warning("mapping notify error, cluster's session keep alive", e);
                        }
                    }
                    break;
                case CLUSTER_TIMER:// ClusterConsumer Timeout->start_vote,heartbeat-cycle,step down->follower
                    T content = event.getContent()
                                     .getFirst();
                    publish(_Writer, _ClusterCustom.onTimer(_SessionManager, content));
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
