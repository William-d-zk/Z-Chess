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

package com.isahl.chess.knight.cluster;

import com.isahl.chess.bishop.io.sort.ZSortHolder;
import com.isahl.chess.bishop.io.ws.control.X103_Ping;
import com.isahl.chess.bishop.io.ws.zchat.zcrypt.EncryptHandler;
import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.schedule.ScheduleHandler;
import com.isahl.chess.king.base.schedule.TimeWheel;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.model.RaftMachine;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.config.IClusterConfig;
import com.isahl.chess.queen.config.ISocketConfig;
import com.isahl.chess.queen.event.handler.cluster.IClusterCustom;
import com.isahl.chess.queen.event.handler.cluster.IConsistentCustom;
import com.isahl.chess.queen.event.handler.mix.ILogicHandler;
import com.isahl.chess.queen.io.core.async.BaseAioClient;
import com.isahl.chess.queen.io.core.async.inf.IAioClient;
import com.isahl.chess.queen.io.core.async.inf.IAioServer;
import com.isahl.chess.queen.io.core.executor.ClusterCore;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionDismiss;
import com.isahl.chess.queen.io.core.manager.ClusterManager;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static com.isahl.chess.king.base.schedule.TimeWheel.IWheelItem.PRIORITY_NORMAL;

public class ClusterNode
        extends
        ClusterManager
        implements
        ISessionDismiss,
        IClusterNode
{
    private final Logger     _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());
    private final TimeWheel  _TimeWheel;
    private final IAioServer _PeerServer, _GateServer;
    private final IAioClient _PeerClient, _GateClient;
    private final ZUID       _ZUid;
    private final X103_Ping  _PeerPing, _GatePing;

    @Override
    public void onDismiss(ISession session)
    {
        _Logger.debug("dismiss %s", session);
        rmSession(session);
    }

    public ClusterNode(IAioConfig config,
                       IClusterConfig clusterConfig,
                       IRaftConfig raftConfig,
                       TimeWheel timeWheel) throws IOException
    {
        super(config, new ClusterCore(clusterConfig));
        _TimeWheel = timeWheel;
        _ZUid = raftConfig.createZUID();
        _Logger.debug(_ZUid);
        if (raftConfig.isInCongress()) {
            IPair peerBind = raftConfig.getPeerBind();
            final ZSortHolder _PeerHolder = ZSortHolder.WS_CLUSTER_SYMMETRY;
            _PeerServer = buildServer(peerBind,
                                      getSocketConfig(_PeerHolder.getSlot()),
                                      _PeerHolder,
                                      this,
                                      this,
                                      _ZUid,
                                      false);
            String peerHost = peerBind.getFirst();
            int peerPort = peerBind.getSecond();
            _PeerPing = new X103_Ping(String.format("%#x,%s:%d", _ZUid.getPeerId(), peerHost, peerPort)
                                            .getBytes(StandardCharsets.UTF_8));
        }
        else {
            _PeerServer = null;
            _PeerPing = null;
        }
        if (raftConfig.isGateNode()) {
            final ZSortHolder _GateHolder = ZSortHolder.WS_CLUSTER_SYMMETRY;
            IPair gateBind = raftConfig.getGateBind();
            _GateServer = buildServer(gateBind,
                                      getSocketConfig(_GateHolder.getSlot()),
                                      _GateHolder,
                                      this,
                                      this,
                                      _ZUid,
                                      false);
            String gateHost = gateBind.getFirst();
            int gatePort = gateBind.getSecond();
            _GatePing = new X103_Ping(String.format("%#x,%s:%d", _ZUid.getPeerId(), gateHost, gatePort)
                                            .getBytes(StandardCharsets.UTF_8));
        }
        else {
            _GateServer = null;
            _GatePing = null;
        }
        _GateClient = new BaseAioClient(_TimeWheel, getClusterChannelGroup())
        {
            @Override
            public void onCreated(ISession session)
            {
                super.onCreated(session);
                Duration gap = Duration.ofSeconds(session.getReadTimeOutSeconds() / 2);
                _TimeWheel.acquire(session,
                                   new ScheduleHandler<>(gap, true, ClusterNode.this::gateHeartbeat, PRIORITY_NORMAL));
            }

            @Override
            public void onDismiss(ISession session)
            {
                ClusterNode.this.onDismiss(session);
                super.onDismiss(session);
            }
        };
        _PeerClient = new BaseAioClient(_TimeWheel, getClusterChannelGroup())
        {

            @Override
            public void onCreated(ISession session)
            {
                super.onCreated(session);
                Duration gap = Duration.ofSeconds(session.getReadTimeOutSeconds() / 2);
                _TimeWheel.acquire(session,
                                   new ScheduleHandler<>(gap, true, ClusterNode.this::peerHeartbeat, PRIORITY_NORMAL));
            }

            @Override
            public void onDismiss(ISession session)
            {
                ClusterNode.this.onDismiss(session);
                super.onDismiss(session);
            }
        };
    }

    @PostConstruct
    void init()
    {
        _Logger.debug("load cluster node");
    }

    public void start(IClusterCustom<RaftMachine> clusterCustom,
                      IConsistentCustom consistentCustom,
                      ILogicHandler logicHandler) throws IOException
    {
        build(clusterCustom, consistentCustom, logicHandler, EncryptHandler::new);
        _PeerServer.bindAddress(_PeerServer.getLocalAddress(), getClusterChannelGroup());
        _PeerServer.pendingAccept();
        _Logger.debug("cluster start→peer:%s", _PeerServer.getLocalAddress());
        if (_GateServer != null) {
            _GateServer.bindAddress(_GateServer.getLocalAddress(), getClusterChannelGroup());
            _GateServer.pendingAccept();
            _Logger.debug("cluster start:gate: %s", _GateServer.getLocalAddress());
        }
    }

    @Override
    public void setupPeer(IPair remote) throws IOException
    {
        final ZSortHolder _Holder = ZSortHolder.WS_CLUSTER_SYMMETRY;
        ISocketConfig socketConfig = getSocketConfig(_Holder.getSlot());
        _PeerClient.connect(buildConnector(remote, socketConfig, _PeerClient, ClusterNode.this, _Holder, _ZUid));
    }

    @Override
    public void setupGate(IPair remote) throws IOException
    {
        final ZSortHolder _Holder = ZSortHolder.WS_CLUSTER_SYMMETRY;
        ISocketConfig socketConfig = getSocketConfig(_Holder.getSlot());
        _GateClient.connect(buildConnector(remote, socketConfig, _GateClient, ClusterNode.this, _Holder, _ZUid));
    }

    private void peerHeartbeat(ISession session)
    {
        _Logger.debug("cluster heartbeat =>%s", session.getRemoteAddress());
        send(session, OperatorType.CLUSTER_LOCAL, _PeerPing);
    }

    private void gateHeartbeat(ISession session)
    {
        _Logger.debug("gate heartbeat =>%s", session.getRemoteAddress());
        send(session, OperatorType.CLUSTER_LOCAL, _GatePing);
    }

    @Override
    public long getZUid()
    {
        return _ZUid.getId();
    }

}
