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

package com.isahl.chess.pawn.endpoint.device;

import com.isahl.chess.bishop.protocol.zchat.model.command.X1F_Exchange;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.X0B_Ping;
import com.isahl.chess.bishop.protocol.zchat.zcrypto.Encryptor;
import com.isahl.chess.bishop.sort.ZSortHolder;
import com.isahl.chess.king.base.cron.ScheduleHandler;
import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.cluster.IClusterNode;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.features.IRaftMachine;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.config.IMixConfig;
import com.isahl.chess.queen.config.ISocketConfig;
import com.isahl.chess.queen.events.cluster.IClusterCustom;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.events.server.ILinkCustom;
import com.isahl.chess.queen.events.server.ILogicHandler;
import com.isahl.chess.queen.io.core.example.MixManager;
import com.isahl.chess.queen.io.core.features.cluster.IClusterPeer;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IDismiss;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.net.socket.BaseAioClient;
import com.isahl.chess.queen.io.core.net.socket.features.client.IAioClient;
import com.isahl.chess.queen.io.core.net.socket.features.server.IAioServer;
import com.isahl.chess.queen.io.core.tasks.ServerCore;
import com.lmax.disruptor.RingBuffer;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author william.d.zk
 * @date 2019-05-12
 */
public class DeviceNode
        extends MixManager
        implements IDismiss,
                   IClusterNode
{
    private final IClusterPeer     _ClusterPeer;
    private final List<IAioServer> _AioServers;
    private final IAioClient       _PeerClient;
    private final IAioClient       _GateClient;
    private final TimeWheel        _TimeWheel;
    private final X0B_Ping         _PeerPing, _GatePing;

    @Override
    public void onDismiss(ISession session)
    {
        _Logger.warning("dismiss %s", session);
        rmSession(session);
    }

    public DeviceNode(List<ITriple> hosts,
                      boolean multiBind,
                      IAioConfig bizIoConfig,
                      IRaftConfig raftConfig,
                      IMixConfig serverConfig,
                      TimeWheel timeWheel,
                      IClusterPeer clusterPeer) throws IOException
    {
        super(bizIoConfig, new ServerCore(serverConfig));
        _TimeWheel = timeWheel;
        _ClusterPeer = clusterPeer;

        if(raftConfig.isInCongress()) {
            RaftNode peerBind = raftConfig.getPeerBind();
            final String _PeerBindHost = peerBind.getHost();
            final int _PeerBindPort = peerBind.getPort();
            hosts.add(Triple.of(_PeerBindHost, _PeerBindPort, ZSortHolder.Z_CLUSTER_SYMMETRY));
            _PeerPing = new X0B_Ping(String.format("%#x,%s:%d", _ClusterPeer.peerId(), _PeerBindHost, _PeerBindPort));
        }
        else {
            _PeerPing = null;
        }
        if(raftConfig.isGateNode()) {
            RaftNode gateBind = raftConfig.getPeerBind();
            final String _GateBindHost = gateBind.getGateHost();
            final int _GateBindPort = gateBind.getGatePort();
            hosts.add(Triple.of(_GateBindHost, _GateBindPort, ZSortHolder.Z_CLUSTER_SYMMETRY));
            _GatePing = new X0B_Ping(String.format("%#x,%s:%d", _ClusterPeer.peerId(), _GateBindHost, _GateBindPort));
        }
        else {
            _GatePing = null;
        }
        _AioServers = hosts.stream()
                           .map(triple->{
                               final String _Host = triple.getFirst();
                               final int _Port = triple.getSecond();
                               final ZSortHolder _Holder = triple.getThird();
                               registerFactory(_Holder.getSort()
                                                      .getFactory());
                               return buildServer(_Host,
                                                  _Port,
                                                  getSocketConfig(_Holder.getSlot()),
                                                  _Holder,
                                                  DeviceNode.this,
                                                  DeviceNode.this,
                                                  multiBind,
                                                  _ClusterPeer);
                           })
                           .collect(Collectors.toList());
        _GateClient = new BaseAioClient(_TimeWheel, getClusterChannelGroup(), "GateClient")
        {
            @Override
            public void onCreated(ISession session)
            {
                super.onCreated(session);
                Duration gap = Duration.ofSeconds(session.getReadTimeOutSeconds() / 2);
                _TimeWheel.acquire(session, new ScheduleHandler<>(gap, true, DeviceNode.this::gateHeartbeat));
            }

            @Override
            public void onDismiss(ISession session)
            {
                DeviceNode.this.onDismiss(session);
                super.onDismiss(session);
            }
        };
        _PeerClient = new BaseAioClient(_TimeWheel, getClusterChannelGroup(), "PeerClient")
        {
            @Override
            public void onCreated(ISession session)
            {
                super.onCreated(session);
                Duration gap = Duration.ofSeconds(session.getReadTimeOutSeconds() / 2);
                _TimeWheel.acquire(session, new ScheduleHandler<>(gap, true, DeviceNode.this::peerHeartbeat));
            }

            @Override
            public void onDismiss(ISession session)
            {
                DeviceNode.this.onDismiss(session);
                super.onDismiss(session);
            }
        };
        _Logger.info("Device Node Bean Load");
    }

    public void start(ILogicHandler.factory logicFactory, ILinkCustom linkCustom, IClusterCustom<IRaftMachine> clusterCustom)
    {
        build(logicFactory, linkCustom, clusterCustom, Encryptor::new);
        for(IAioServer server : _AioServers) {
            try {
                server.bindAddress(server.getLocalAddress(), getServiceChannelGroup());
            }
            catch(IOException e) {
                _Logger.warning("server bind error %s", e, server.getLocalAddress());
            }
            server.pendingAccept();
            _Logger.info(String.format("device node start %s %s @ %s", server.getLocalAddress(), server.getMode(), server.getProtocol()));
        }
    }

    @Override
    public void setupPeer(String host, int port) throws IOException
    {
        final ZSortHolder _Holder = ZSortHolder.Z_CLUSTER_SYMMETRY;
        ISocketConfig socketConfig = getSocketConfig(_Holder.getSlot());
        registerFactory(_Holder.getSort()
                               .getFactory());
        _PeerClient.connect(buildConnector(host, port, socketConfig, _PeerClient, DeviceNode.this, _Holder, _ClusterPeer));
    }

    @Override
    public void setupGate(String host, int port) throws IOException
    {
        final ZSortHolder _Holder = ZSortHolder.Z_CLUSTER_SYMMETRY;
        ISocketConfig socketConfig = getSocketConfig(_Holder.getSlot());
        registerFactory(_Holder.getSort()
                               .getFactory());
        _GateClient.connect(buildConnector(host, port, socketConfig, _GateClient, DeviceNode.this, _Holder, _ClusterPeer));
    }

    private void peerHeartbeat(ISession session)
    {
        _Logger.debug("device_cluster heartbeat => %s ", session.getRemoteAddress());
        send(session, IOperator.Type.CLUSTER_LOCAL, _PeerPing);
    }

    private void gateHeartbeat(ISession session)
    {
        _Logger.debug("gate_cluster heartbeat => %s ", session.getRemoteAddress());
        send(session, IOperator.Type.CLUSTER_LOCAL, _GatePing);
    }

    @Override
    public boolean isOwnedBy(long origin)
    {
        return _ClusterPeer.peerId() == (origin & ZUID.PEER_MASK);
    }

    @Override
    public IClusterPeer clusterPeer()
    {
        return _ClusterPeer;
    }

    @Override
    public RingBuffer<QEvent> selectPublisher(IOperator.Type type)
    {
        return getLocalPublisher().selectPublisher(type);
    }

    @Override
    public RingBuffer<QEvent> selectCloser(IOperator.Type type)
    {
        return getLocalPublisher().selectCloser(type);
    }

    @Override
    public ReentrantLock selectLock(IOperator.Type type)
    {
        return getLocalPublisher().selectLock(type);
    }

    @Override
    public void exchange(IProtocol body, long target, int factory, List<ITriple> load)
    {
        if(load == null || body == null || target == INVALID_INDEX) {
            _Logger.warning("exchange failed{ load:%s body:%s origin:%#x }", load, body, target);
            return;
        }
        ISession session = findSessionOverIndex(target);
        if(session == null) {
            _Logger.warning("exchange failed, no session routing");
            return;
        }
        X1F_Exchange x1F = new X1F_Exchange(clusterPeer().generateId());
        x1F.withSub(body);
        x1F.target(target);
        x1F.factory(factory);
        x1F.peer(clusterPeer().peerId());
        load.add(Triple.of(x1F.with(session), session, session.encoder()));
    }
}
