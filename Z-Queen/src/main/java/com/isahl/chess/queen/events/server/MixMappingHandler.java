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
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.db.model.IStorage;
import com.isahl.chess.queen.events.cluster.IClusterCustom;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.events.routes.IMappingCustom;
import com.isahl.chess.queen.io.core.example.MixManager;
import com.isahl.chess.queen.io.core.features.cluster.IConsistency;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;
import com.isahl.chess.queen.io.core.features.model.channels.IConnectActivity;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.net.socket.features.IAioConnection;
import com.lmax.disruptor.RingBuffer;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Collection;
import java.util.List;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.*;

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
    private final IManager           _SessionManager;
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
    public IHealth _Health()
    {
        return _Health;
    }

    @Override
    public void onEvent(QEvent event, long sequence)
    {
        if(event.hasError()) {
            switch(event.getErrorType()) {
                case ACCEPT_FAILED, CONNECT_FAILED -> {
                    IOperator<Throwable, IAioConnection, Void> fop = event.getEventOp();
                    IPair error = event.getContent();
                    fop.handle(error.getFirst(), error.getSecond());
                }
                case PASSIVE_CLOSE, INITIATIVE_CLOSE -> {
                    _Logger.warning("mapping handle io error,%s",
                                    event.getErrorType()
                                         .getMsg());
                    IPair error = event.getContent();
                    ISession session = error.getSecond();
                    IProtocol closed = _LinkCustom.onClose(session);
                    if(_ClusterCustom.waitForCommit() && closed != null) {
                        publish(_Transfer,
                                CONSISTENT,
                                Pair.of(closed,
                                        Pair.of(session.index(),
                                                session.getFactory()
                                                       .serial())),
                                null);
                    }
                    else if(closed != null) {
                        publish(_Writer, _LinkCustom.onConsistency(_SessionManager, _ClusterCustom.skipConsistency(closed, session.index()), closed));
                    }
                }
            }
        }
        else {
            switch(event.getEventType()) {
                case CONNECTED, ACCEPTED -> {
                    IPair connected = event.getContent();
                    AsynchronousSocketChannel channel = connected.getSecond();
                    IAioConnection connection = connected.getFirst();
                    IOperator<IConnectActivity, AsynchronousSocketChannel, ITriple> cop = event.getEventOp();
                    ITriple result = cop.handle(connection, channel);
                    boolean success = result.getFirst();
                    if(success) {
                        ISession session = result.getSecond();
                        ITriple backload = result.getThird();
                        if(backload != null) {
                            IOperator.Type type = result.getThird();
                            switch(type) {
                                case SINGLE -> publish(_Writer, WRITE, Pair.of(backload.getFirst(), session), session.encoder());
                                case BATCH -> publish(_Writer, backload.getFirst());
                            }
                        }
                    }
                    else {
                        Throwable throwable = result.getThird();
                        if(result.getSecond() instanceof ISession session) {
                            session.innerClose();
                        }
                        connection.error();
                        _Logger.warning("session create failed %s", throwable, connection);
                    }
                }
                case LINK -> {
                    IPair content = event.getContent();
                    IProtocol received = content.getFirst();
                    ISession session = content.getSecond();
                    if(received != null && session != null) {
                        try {
                            ITriple result = doCustom(_LinkCustom, _SessionManager, session, received);
                            if(result != null && _ClusterCustom.waitForCommit()) {
                                IProtocol consistent = result.getSecond();
                                if(consistent != null) {
                                    publish(_Transfer,
                                            CONSISTENT,
                                            Pair.of(consistent,
                                                    Pair.of(session.index(),
                                                            session.getFactory()
                                                                   .serial())),
                                            null);
                                }
                                else {
                                    _Logger.warning("no consistency to do");
                                }
                            }
                            else if(result != null) {
                                IProtocol request = result.getSecond();
                                if(request != null) {
                                    publish(_Writer,
                                            _LinkCustom.onConsistency(_SessionManager, _ClusterCustom.skipConsistency(request, session.index()), request));
                                }
                                else {
                                    _Logger.warning("no consistency to do");
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
                 *  core.ClusterProcessor → core._LinkEvent(_Transfer) → core.LinkProcessor → _LinkCustom
                 *  所有从 cluster 收的远端数据都进入此处处理
                 */
                case CLUSTER -> {
                    IPair content = event.getContent();
                    IProtocol received = content.getFirst();
                    ISession session = content.getSecond();
                    if(received != null && session != null) {
                        try {
                            ITriple result = doCustom(_ClusterCustom, _SessionManager, session, received);
                            /*
                             * doCustom 执行结果 snd 是需要反向投递到linker的内容
                             */
                            if(result != null && result.getSecond() instanceof IConsistent backload) {
                                publish(_Transfer, CONSISTENCY, Pair.of(backload, _SessionManager), _LinkCustom.getUnbox());
                            }
                            else if(result != null && result.getSecond() instanceof Collection<?> collection) {
                                for(Object item : collection) {
                                    if(item instanceof IConsistent backload) {
                                        publish(_Transfer, CONSISTENCY, Pair.of(backload, _SessionManager), _LinkCustom.getUnbox());
                                    }
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
                 *  CONSISTENCY:{
                 *      cluster processor
                 *      core.LinkProcessor → core._ClusterEvent(_Transfer) → core.ClusterProcessor → _ClusterCustom
                 *  }
                 *  CONSISTENCY_SERVICE:{
                 *      consistency open service
                 *      api → open service → core.ClusterProcessor → _ClusterCustom
                 *  }
                 */
                case CONSISTENT, CONSISTENT_SERVICE -> {
                    IPair content = event.getContent();
                    IProtocol request = content.getFirst();
                    IPair routing = content.getSecond();
                    /*
                     *  origin 在不同状态下的来源
                     *  CONSISTENCY:{
                     *      link-session.index
                     *  }
                     *  CONSISTENCY_SERVICE:{
                     *      consistent-service.node-id.http-session
                     *  }
                     */
                    long origin = routing.getFirst();
                    int factory = routing.getSecond();
                    try {
                        _Logger.debug("consistency request: %s", request);
                        publish(_Writer, _ClusterCustom.consistent(_SessionManager, request, origin, factory));
                    }
                    catch(Exception e) {
                        _Logger.warning("mapping consensus error, link session close", e);
                    }
                }
                case CLUSTER_TOPOLOGY -> {
                    IPair content = event.getContent();
                    IoSerial topology = content.getSecond();
                    /*
                     *  core._ConsensusApiEvent → core.ClusterProcessor → _ClusterCustom
                     */
                    try {
                        publish(_Writer, _ClusterCustom.change(_SessionManager, topology));
                    }
                    catch(Exception e) {
                        _Logger.warning("cluster inner service api ");
                    }
                }
                /*
                 *  LINKER(linker) →  CONSISTENCY(cluster) → CLUSTER(cluster) → CONSISTENT_RESULT(linker)
                 *  cluster（cluster-client） → Linker ｜ Linker notify → device.session
                 */
                case CONSISTENCY -> {
                    IPair content = event.getContent();
                    IConsistency consistency = content.getFirst();
                    IManager manager = content.getSecond();
                    IOperator<IConsistency, IManager, IProtocol> unbox = event.getEventOp();
                    if(consistency != null) {
                        try {
                            publish(_Writer, _LinkCustom.onConsistency(_SessionManager, consistency, unbox.handle(consistency, manager)));
                        }
                        catch(Exception e) {
                            _Logger.warning("mapping notify error, cluster's session keep alive", e);
                        }
                    }
                }
                /*
                 *  ClusterConsumer Timeout->start_vote,heartbeat-cycle,step down->follower
                 */
                case CLUSTER_TIMER -> {
                    IPair content = event.getContent();
                    T machine = content.getSecond();
                    publish(_Writer, _ClusterCustom.onTimer(_SessionManager, machine));

                }
                default -> _Logger.warning("mapping handler error %s",
                                           event.getEventType()
                                                .name());
            }
        }
    }

    @Override
    public Logger _Logger()
    {
        return _Logger;
    }

    private ITriple doCustom(IMappingCustom custom, IManager manager, ISession session, IProtocol received)
    {
        ITriple result = custom.inject(manager, session, received);
        /*
         * fst  [response → cluster peer session] : command implements 'IControl', BATCH:List of IControl ; SINGLE: IControl
         * snd  [response → link handler] : command implements 'IConsistentResult', 需要传递给LINK的内容，
         * trd  [operator-type] : operator-type [SINGLE|BATCH]
         */
        _Logger.debug("handled:[ %s ] → [ %s ]", received, result);
        if(result != null) {
            IOperator.Type type = result.getThird();
            switch(type) {
                case SINGLE -> {
                    IProtocol response = result.getFirst();
                    publish(_Writer,
                            WRITE,
                            Pair.of(response, response.session()),
                            response.session()
                                    .encoder());
                }
                case BATCH -> {
                    List<ITriple> responses = result.getFirst();
                    publish(_Writer, responses);
                }
                case NULL -> {
                    //result.fst == null ignore
                }
            }
        }
        return result;
    }

}
