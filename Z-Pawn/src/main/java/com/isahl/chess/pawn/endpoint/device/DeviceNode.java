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
import com.isahl.chess.queen.event.handler.cluster.IClusterCustom;
import com.isahl.chess.queen.event.handler.mix.ILinkCustom;
import com.isahl.chess.queen.event.handler.mix.ILogicHandler;
import com.isahl.chess.queen.io.core.async.AioSession;
import com.isahl.chess.queen.io.core.async.BaseAioClient;
import com.isahl.chess.queen.io.core.async.BaseAioServer;
import com.isahl.chess.queen.io.core.async.inf.IAioClient;
import com.isahl.chess.queen.io.core.async.inf.IAioServer;
import com.isahl.chess.queen.io.core.async.inf.IAioSort;
import com.isahl.chess.queen.io.core.executor.ServerCore;
import com.isahl.chess.queen.io.core.inf.IConnectActivity;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionDismiss;
import com.isahl.chess.queen.io.core.inf.ISort;
import com.isahl.chess.queen.io.core.manager.MixManager;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static com.isahl.chess.king.base.schedule.TimeWheel.IWheelItem.PRIORITY_NORMAL;

/**
 * @author william.d.zk
 * 
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
    private final ZUID             _ZUID;
    private final X103_Ping        _Ping;

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
        _ZUID = raftConfig.createZUID();
        IPair bind = raftConfig.getBind();
        final String _ClusterHost = bind.getFirst();
        final int _ClusterPort = bind.getSecond();
        hosts.add(new Triple<>(_ClusterHost, _ClusterPort, ZSortHolder.WS_CLUSTER_SERVER));

        _AioServers = hosts.stream()
                           .map(triple ->
                           {
                               final String _Host = triple.getFirst();
                               final int _Port = triple.getSecond();
                               ZSortHolder holder = triple.getThird();
                               return buildAioServer(_Host,
                                                     _Port,
                                                     holder.getType(),
                                                     holder.getSlot(),
                                                     holder.getSort(),
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
                                   new ScheduleHandler<>(gap, true, DeviceNode.this::heartbeat, PRIORITY_NORMAL));
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
                                   new ScheduleHandler<>(gap, true, DeviceNode.this::heartbeat, PRIORITY_NORMAL));
            }

            @Override
            public void onDismiss(ISession session)
            {
                DeviceNode.this.onDismiss(session);
                super.onDismiss(session);
            }
        };
        _Ping = new X103_Ping(String.format("%#x,%s:%d", _ZUID.getPeerId(), _ClusterHost, _ClusterPort)
                                    .getBytes(StandardCharsets.UTF_8));
        _Logger.debug("Device Node Bean Load");
    }

    private <C extends IPContext> IAioServer buildAioServer(final String _Host,
                                                            final int _Port,
                                                            final long _Type,
                                                            final int _SessionSlot,
                                                            final IAioSort<C> _Sort,
                                                            final boolean _MultiBind)
    {
        return new BaseAioServer(_Host, _Port, getSocketConfig(_SessionSlot))
        {
            @Override
            public ISort.Mode getMode()
            {
                return _Sort.getMode();
            }

            @Override
            public ISession createSession(AsynchronousSocketChannel socketChannel,
                                          IConnectActivity activity) throws IOException
            {
                return new AioSession<>(socketChannel, _Type, this, _Sort, activity, DeviceNode.this, _MultiBind);
            }

            @Override
            public void onCreated(ISession session)
            {
                DeviceNode.this.addSession(session);
                session.ready();
            }

            @Override
            public String getProtocol()
            {
                return _Sort.getProtocol();
            }
        };
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
        _PeerClient.connect(buildConnector(remote,
                                           getSocketConfig(ZUID.TYPE_PROVIDER_SLOT),
                                           _PeerClient,
                                           ZUID.TYPE_PROVIDER,
                                           this,
                                           ZSortHolder.WS_CLUSTER_CONSUMER,
                                           _ZUID));
    }

    @Override
    public void addGate(IPair remote) throws IOException
    {
        _GateClient.connect(buildConnector(remote,
                                           getSocketConfig(ZUID.TYPE_INTERNAL_SLOT),
                                           _GateClient,
                                           ZUID.TYPE_INTERNAL,
                                           this,
                                           ZSortHolder.WS_CLUSTER_SYMMETRY,
                                           _ZUID));
    }

    @Override
    public ServerCore getCore()
    {
        return super.getCore();
    }

    private void heartbeat(ISession session)
    {
        _Logger.debug("device_cluster heartbeat => %s ", session.getRemoteAddress());
        getCore().send(session, OperatorType.CLUSTER_LOCAL, _Ping);
    }

    @Override
    public long getZuid()
    {
        return _ZUID.getId();
    }
}
