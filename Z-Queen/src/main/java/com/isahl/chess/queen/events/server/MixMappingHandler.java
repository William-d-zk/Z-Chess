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
import com.isahl.chess.queen.events.routes.IMappingCustom;
import com.isahl.chess.queen.io.core.example.MixManager;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;
import com.isahl.chess.queen.io.core.features.model.channels.IConnectActivity;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.ISessionDismiss;
import com.isahl.chess.queen.io.core.features.model.session.ISessionManager;
import com.isahl.chess.queen.io.core.net.socket.features.IAioConnection;
import com.lmax.disruptor.RingBuffer;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.CONSISTENCY;
import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.WRITE;

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
                case ACCEPT_FAILED, CONNECT_FAILED -> {
                    IOperator<Throwable, IAioConnection, Void> failedOperator = event.getEventOp();
                    IPair errorContent = event.getContent();
                    failedOperator.handle(errorContent.getFirst(), errorContent.getSecond());
                }
                case PASSIVE_CLOSE, INITIATIVE_CLOSE -> {
                    _Logger.warning("mapping handle io error,%s",
                                    event.getErrorType()
                                         .getMsg());
                    IOperator<Void, ISession, Void> closeOperator = event.getEventOp();
                    IPair errorContent = event.getContent();
                    ISession session = errorContent.getSecond();
                    ISessionDismiss dismiss = session.getDismissCallback();
                    boolean closed = session.isClosed();
                    closeOperator.handle(null, session);
                    if(!closed) {
                        dismiss.onDismiss(session);
                        _LinkCustom.close(session);
                    }
                }
            }
        }
        else {
            _Logger.trace("mapping:%s", event);
            switch(event.getEventType()) {
                case CONNECTED, ACCEPTED -> {
                    IPair connected = event.getContent();
                    AsynchronousSocketChannel channel = connected.getSecond();
                    IAioConnection connection = connected.getFirst();
                    IOperator<IConnectActivity, AsynchronousSocketChannel, ITriple> connectedOperator = event.getEventOp();
                    ITriple handled = connectedOperator.handle(connection, channel);
                    boolean success = handled.getFirst();
                    if(success) {
                        ISession session = handled.getSecond();
                        ITriple result = handled.getThird();
                        if(result != null) {
                            IOperator.Type type = result.getThird();
                            switch(type) {
                                case SINGLE -> {
                                    publish(_Writer,
                                            WRITE,
                                            new Pair<>(result.getFirst(), session),
                                            session.getEncoder());
                                }
                                case BATCH -> publish(_Writer, result.getFirst());
                            }
                        }
                    }
                    else {
                        Throwable throwable = handled.getThird();
                        if(handled.getSecond() instanceof ISession) {
                            ISession session = handled.getSecond();
                            session.innerClose();
                        }
                        connection.error();
                        _Logger.warning("session create failed %s", throwable, connection);
                    }
                }
                case LINK -> {
                    IControl received = event.getContent()
                                             .getFirst();
                    ISession session = event.getContent()
                                            .getSecond();
                    if(received != null && session != null) {
                        try {
                            ITriple result = doCustom(_LinkCustom, _SessionManager, session, received);
                            if(result != null && _ClusterCustom.waitForCommit()) {
                                IProtocol request = result.getSecond();
                                if(request != null) {
                                    publish(_Transfer,
                                            CONSISTENCY,
                                            new Pair<>(request, session.getIndex()),
                                            _ClusterCustom.getReject()
                                                          .getOperator());

                                }
                            }
                            else if(result != null) {
                                IControl response = result.getSecond();
                                if(response != null) {
                                    publish(_Writer,
                                            _LinkCustom.notify(_SessionManager, response, session.getIndex(), true));
                                }
                            }
                            else {
                                _Logger.debug("link received ignore:%s", received);
                            }
                        }
                        catch(Exception e) {
                            _Logger.warning("link mapping handler error", e);
                            session.innerClose();
                        }
                    }
                }
                /*
                    core.ClusterProcessor → core._LinkEvent(_Transfer) → core.LinkProcessor → _LinkCustom
                 */
                case CLUSTER -> {
                    IControl received = event.getContent()
                                             .getFirst();
                    ISession session = event.getContent()
                                            .getSecond();
                    if(received != null && session != null) {
                        try {
                            ITriple result = doCustom(_ClusterCustom, _SessionManager, session, received);
                            if(result != null) {
                                IConsistent adjudge = result.getSecond();
                                if(adjudge != null) {
                                    if(!_ClusterCustom.onConsistentCall(adjudge)) {
                                        publish(_Transfer,
                                                IOperator.Type.LINK_CONSISTENT_RESULT,
                                                new Pair<>(adjudge, session),
                                                _LinkCustom.getOperator());
                                    }
                                }else{
                                    _Logger.debug("nothing for transfer to link");
                                }
                            }
                            else {
                                _Logger.debug("cluster received ignore :%s", received);
                            }
                        }
                        catch(Exception e) {
                            _Logger.warning("cluster mapping handler error", e);
                            session.innerClose();
                        }
                    }
                }
                /*
                    CONSISTENCY:{
                        cluster processor
                        core.LinkProcessor → core._ClusterEvent(_Transfer) → core.ClusterProcessor → _ClusterCustom
                    }
                    CONSISTENCY_SERVICE:{
                        consistency open service
                        api → open service → core.ClusterProcessor → _ClusterCustom
                    }
                 */
                case CONSISTENCY, CONSISTENCY_SERVICE -> {
                    IProtocol request = event.getContent()
                                             .getFirst();
                    /*
                        origin state
                        CONSISTENCY:{
                            link-session.index
                        }
                        CONSISTENCY_SERVICE:{
                            consistent-service.node-id
                        }
                     */
                    long origin = event.getContent()
                                       .getSecond();
                    try {
                        List<ITriple> contents = _ClusterCustom.consistent(_SessionManager, request, origin);
                        _Logger.debug("consistency request: %s ; cluster-broadcast: %s ", request, contents);
                        if(contents != null) {publish(_Writer, contents);}
                        else {
                            IOperator<IProtocol, Long, ITriple> reject = event.getEventOp();
                            ITriple resp = reject.handle(request, origin);
                            if(resp != null) {
                                error(_Error,
                                      IError.Type.CONSISTENCY_REJECT,
                                      new Pair<>(resp.getFirst(), resp.getSecond()),
                                      resp.getThird());
                            }
                        }
                    }
                    catch(Exception e) {
                        _Logger.warning("mapping consensus error, link session close", e);
                    }
                }
                case CLUSTER_TOPOLOGY -> {
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
                }

                /*
                    LINKER(linker) →  CONSISTENCY(cluster) → CLUSTER(cluster) → CONSISTENT_RESULT(linker)
                    cluster（cluster-client） → Linker ｜ Linker notify → device.session
                 */
                case LINK_CONSISTENT_RESULT -> {
                    IConsistent consistency = event.getContent()
                                                   .getFirst();
                    /*
                    cluster-session
                     */
                    ISession session = event.getContent()
                                            .getSecond();
                    IOperator<IConsistent, ISession, IControl> adjudgeOperator = event.getEventOp();
                    if(consistency != null) {
                        try {
                            publish(_Writer,
                                    _LinkCustom.notify(_SessionManager,
                                                       adjudgeOperator.handle(consistency, session),
                                                       consistency.getOrigin(),
                                                       consistency.isConsistency()));
                        }
                        catch(Exception e) {
                            _Logger.warning("mapping notify error, cluster's session keep alive", e);
                        }
                    }

                }
                /*
                    ClusterConsumer Timeout->start_vote,heartbeat-cycle,step down->follower
                 */
                case CLUSTER_TIMER -> {
                    T content = event.getContent()
                                     .getFirst();
                    publish(_Writer, _ClusterCustom.onTimer(_SessionManager, content));

                }
                default -> _Logger.warning("mapping handler error %s",
                                           event.getEventType()
                                                .name());
            }
        }
        event.reset();
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }

    private ITriple doCustom(IMappingCustom custom, ISessionManager manager, ISession session, IControl received)
    {
        ITriple result = custom.handle(manager, session, received);
        if(result != null) {
            IOperator.Type type = result.getThird();
            switch(type) {
                case SINGLE -> {
                    IControl response = result.getFirst();
                    if(response != null && response.session() != null) {
                        publish(_Writer,
                                WRITE,
                                new Pair<>(response, response.session()),
                                response.session()
                                        .getEncoder());
                    }
                    else {
                        _Logger.debug("recv:[ %s ],resp:[ %s ]", received, response);

                    }
                }
                case BATCH -> {
                    List<ITriple> responses = result.getFirst();
                    if(responses != null && responses.size() > 0) {
                        publish(_Writer, responses);
                    }
                }
            }
        }
        return result;
    }

}
