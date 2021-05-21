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

package com.isahl.chess.pawn.endpoint.device;

import com.isahl.chess.bishop.io.sort.ZSortHolder;
import com.isahl.chess.bishop.io.ws.control.X103_Ping;
import com.isahl.chess.bishop.io.ws.zchat.zcrypt.EncryptHandler;
import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.schedule.ScheduleHandler;
import com.isahl.chess.king.base.schedule.TimeWheel;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.knight.cluster.IClusterNode;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.model.RaftMachine;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.config.IMixConfig;
import com.isahl.chess.queen.config.ISocketConfig;
import com.isahl.chess.queen.event.handler.cluster.IClusterCustom;
import com.isahl.chess.queen.event.handler.mix.ILinkCustom;
import com.isahl.chess.queen.event.handler.mix.ILogicHandler;
import com.isahl.chess.queen.io.core.async.BaseAioClient;
import com.isahl.chess.queen.io.core.async.inf.IAioClient;
import com.isahl.chess.queen.io.core.async.inf.IAioServer;
import com.isahl.chess.queen.io.core.executor.ServerCore;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionDismiss;
import com.isahl.chess.queen.io.core.manager.MixManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static com.isahl.chess.king.base.schedule.TimeWheel.IWheelItem.PRIORITY_NORMAL;

/**
 * @author william.d.zk
 * @date 2019-05-12
 */
public class DeviceNode
        extends
        MixManager
        implements
        ISessionDismiss,
        IClusterNode<ServerCore>
{

    private final List<IAioServer> _AioServers;
    private final IAioClient       _PeerClient;
    private final IAioClient       _GateClient;
    private final TimeWheel        _TimeWheel;
    private final ZUID             _ZUid;
    private final X103_Ping        _PeerPing, _GatePing;

    @Override
    public void onDismiss(ISession session)
    {
        _Logger.debug("dismiss %s", session);
        rmSession(session);
    }

    public DeviceNode(List<ITriple> hosts,
                      boolean multiBind,
                      IAioConfig bizIoConfig,
                      IRaftConfig raftConfig,
                      IMixConfig serverConfig,
                      TimeWheel timeWheel) throws IOException
    {
        super(bizIoConfig, new ServerCore(serverConfig));
        _TimeWheel = timeWheel;
        _ZUid = raftConfig.createZUID();
        IPair peerBind = raftConfig.getPeerBind();
        final String _PeerBindHost = peerBind.getFirst();
        final int _PeerBindPort = peerBind.getSecond();
        hosts.add(new Triple<>(_PeerBindHost, _PeerBindPort, ZSortHolder.WS_CLUSTER_SERVER));
        _PeerPing = new X103_Ping(String.format("%#x,%s:%d", _ZUid.getPeerId(), _PeerBindHost, _PeerBindPort)
                                        .getBytes(StandardCharsets.UTF_8));
        if (raftConfig.isGateNode()) {
            IPair gateBind = raftConfig.getGateBind();
            final String _GateBindHost = gateBind.getFirst();
            final int _GateBindPort = gateBind.getSecond();
            hosts.add(new Triple<>(_GateBindHost, _GateBindPort, ZSortHolder.WS_CLUSTER_SYMMETRY));
            _GatePing = new X103_Ping(String.format("%#x,%s:%d", _ZUid.getPeerId(), _GateBindHost, _GateBindPort)
                                            .getBytes(StandardCharsets.UTF_8));
        }
        else {
            _GatePing = null;
        }
        _AioServers = hosts.stream()
                           .map(triple ->
                           {
                               final String _Host = triple.getFirst();
                               final int _Port = triple.getSecond();
                               final ZSortHolder _Holder = triple.getThird();
                               return buildServer(_Host,
                                                  _Port,
                                                  getSocketConfig(_Holder.getSlot()),
                                                  _Holder,
                                                  DeviceNode.this,
                                                  DeviceNode.this,
                                                  _ZUid,
                                                  multiBind);
                           })
                           .collect(Collectors.toList());
        _GateClient = new BaseAioClient(_TimeWheel, getCore().getClusterChannelGroup())
        {
            @Override
            public void onCreated(ISession session)
            {
                super.onCreated(session);
                Duration gap = Duration.ofSeconds(session.getReadTimeOutSeconds() / 2);
                _TimeWheel.acquire(session,
                                   new ScheduleHandler<>(gap, true, DeviceNode.this::gateHeartbeat, PRIORITY_NORMAL));
            }

            @Override
            public void onDismiss(ISession session)
            {
                DeviceNode.this.onDismiss(session);
                super.onDismiss(session);
            }
        };
        _PeerClient = new BaseAioClient(_TimeWheel, getCore().getClusterChannelGroup())
        {

            @Override
            public void onCreated(ISession session)
            {
                super.onCreated(session);
                Duration gap = Duration.ofSeconds(session.getReadTimeOutSeconds() / 2);
                _TimeWheel.acquire(session,
                                   new ScheduleHandler<>(gap, true, DeviceNode.this::peerHeartbeat, PRIORITY_NORMAL));
            }

            @Override
            public void onDismiss(ISession session)
            {
                DeviceNode.this.onDismiss(session);
                super.onDismiss(session);
            }
        };

        _Logger.debug("Device Node Bean Load");
    }

    public void start(ILogicHandler logicHandler,
                      ILinkCustom linkCustom,
                      IClusterCustom<RaftMachine> clusterCustom) throws IOException
    {
        getCore().build(this, logicHandler, linkCustom, clusterCustom, EncryptHandler::new);
        for (IAioServer server : _AioServers) {
            server.bindAddress(server.getLocalAddress(), getCore().getServiceChannelGroup());
            server.pendingAccept();
            _Logger.info(String.format("device node start %s %s @ %s",
                                       server.getLocalAddress(),
                                       server.getMode(),
                                       server.getProtocol()));
        }
    }

    @Override
    public void addPeer(IPair remote) throws IOException
    {
        final ZSortHolder _Holder = ZSortHolder.WS_CLUSTER_CONSUMER;
        ISocketConfig socketConfig = getSocketConfig(_Holder.getSlot());
        _PeerClient.connect(buildConnector(remote, socketConfig, _PeerClient, DeviceNode.this, _Holder, _ZUid));
    }

    @Override
    public void addGate(IPair remote) throws IOException
    {
        final ZSortHolder _Holder = ZSortHolder.WS_CLUSTER_SYMMETRY;
        ISocketConfig socketConfig = getSocketConfig(_Holder.getSlot());
        _GateClient.connect(buildConnector(remote, socketConfig, _GateClient, DeviceNode.this, _Holder, _ZUid));
    }

    @Override
    public ServerCore getCore()
    {
        return super.getCore();
    }

    private void peerHeartbeat(ISession session)
    {
        _Logger.debug("device_cluster heartbeat => %s ", session.getRemoteAddress());
        getCore().send(session, OperatorType.CLUSTER_LOCAL, _PeerPing);
    }

    private void gateHeartbeat(ISession session)
    {
        _Logger.debug("device_cluster heartbeat => %s ", session.getRemoteAddress());
        getCore().send(session, OperatorType.CLUSTER_LOCAL, _GatePing);
    }

    @Override
    public long getZUid()
    {
        return _ZUid.getId();
    }

}
