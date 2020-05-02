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

package com.tgx.chess.pawn.endpoint.spring.device;

import static com.tgx.chess.king.base.schedule.TimeWheel.IWheelItem.PRIORITY_NORMAL;
import static com.tgx.chess.queen.event.inf.IOperator.Type.CLUSTER_LOCAL;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import com.tgx.chess.bishop.io.ZSort;
import com.tgx.chess.bishop.io.ws.control.X104_Ping;
import com.tgx.chess.bishop.io.zcrypt.EncryptHandler;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.control.X106_Identity;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.schedule.ICancelable;
import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.king.topology.ZUID;
import com.tgx.chess.knight.cluster.spring.IClusterNode;
import com.tgx.chess.knight.raft.config.IRaftConfig;
import com.tgx.chess.knight.raft.model.RaftMachine;
import com.tgx.chess.queen.config.IAioConfig;
import com.tgx.chess.queen.config.IMixConfig;
import com.tgx.chess.queen.event.handler.IClusterCustom;
import com.tgx.chess.queen.event.handler.mix.ILinkCustom;
import com.tgx.chess.queen.event.handler.mix.ILogicHandler;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.io.core.async.AioSession;
import com.tgx.chess.queen.io.core.async.BaseAioClient;
import com.tgx.chess.queen.io.core.async.BaseAioServer;
import com.tgx.chess.queen.io.core.executor.ServerCore;
import com.tgx.chess.queen.io.core.inf.IAioClient;
import com.tgx.chess.queen.io.core.inf.IAioServer;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.inf.ISessionOption;
import com.tgx.chess.queen.io.core.manager.MixManager;

/**
 * @author william.d.zk
 * @date 2019-05-12
 */
public class DeviceNode
        extends
        MixManager<ZContext>
        implements
        ISessionDismiss<ZContext>,
        IClusterNode<ServerCore<ZContext>>
{

    private final List<IAioServer<ZContext>> _AioServers;
    private final IAioClient<ZContext>       _PeerClient;
    private final IAioClient<ZContext>       _GateClient;
    private final TimeWheel                  _TimeWheel;
    private final ZUID                       _ZUID;
    private final X104_Ping                  _Ping = new X104_Ping();

    @Override
    public void onDismiss(ISession<ZContext> session)
    {
        _Logger.debug("dismiss %s", session);
        rmSession(session);
    }

    public DeviceNode(List<ITriple> hosts,
                      IAioConfig bizIoConfig,
                      IRaftConfig raftConfig,
                      IMixConfig serverConfig,
                      TimeWheel timeWheel) throws IOException
    {
        super(bizIoConfig, new ServerCore<>(serverConfig));
        _TimeWheel = timeWheel;
        _ZUID = raftConfig.createZUID();
        _Logger.debug(_ZUID);
        IPair bind = raftConfig.getBind();
        hosts.add(new Triple<>(bind.getFirst(), bind.getSecond(), ZSort.WS_CLUSTER_SERVER));
        _AioServers = hosts.stream()
                           .map(triple ->
                           {
                               final String _Host = triple.getFirst();
                               final int _Port = triple.getSecond();
                               final ISort<ZContext> _Sort = triple.getThird();
                               ISort.Mode mode = _Sort.getMode();
                               ISort.Type type = _Sort.getType();
                               final long _SessionType;
                               if (mode == ISort.Mode.CLUSTER && type == ISort.Type.SYMMETRY) {
                                   _SessionType = _Sort == ZSort.MQ_QTT_SYMMETRY ? ZUID.TYPE_INTERNAL
                                                                                 : ZUID.TYPE_CLUSTER;
                               }
                               else if (mode == ISort.Mode.CLUSTER) {
                                   _SessionType = ZUID.TYPE_PROVIDER;
                               }
                               else {
                                   _SessionType = ZUID.TYPE_CONSUMER;
                               }
                               return new BaseAioServer<ZContext>(_Host, _Port, getSocketConfig(getSlot(_SessionType)))
                               {
                                   @Override
                                   public ISession<ZContext> createSession(AsynchronousSocketChannel socketChannel,
                                                                           IConnectActivity<ZContext> activity) throws IOException
                                   {
                                       return new AioSession<>(socketChannel, this, this, activity, DeviceNode.this);
                                   }

                                   @Override
                                   public void onCreate(ISession<ZContext> session)
                                   {
                                       session.setIndex(_ZUID.getId(_SessionType));
                                       DeviceNode.this.addSession(session);
                                   }

                                   @Override
                                   public ISort<ZContext> getSort()
                                   {
                                       return _Sort;
                                   }

                                   @Override
                                   public ZContext createContext(ISessionOption option, ISort<ZContext> sort)
                                   {
                                       return sort.newContext(option);
                                   }

                                   @Override
                                   @SuppressWarnings("unchecked")
                                   public IControl<ZContext>[] createCommands(ISession<ZContext> session)
                                   {
                                       if (_SessionType != ZUID.TYPE_CONSUMER) {
                                           X106_Identity x106 = new X106_Identity(_ZUID.getPeerId());
                                           return new IControl[] { x106 };
                                       }
                                       return null;
                                   }
                               };
                           })
                           .collect(Collectors.toList());
        _GateClient = new BaseAioClient<ZContext>(_TimeWheel, getCore().getClusterChannelGroup())
        {
            @Override
            public void onDismiss(ISession<ZContext> session)
            {
                DeviceNode.this.onDismiss(session);
                super.onDismiss(session);
            }
        };
        _PeerClient = new BaseAioClient<ZContext>(_TimeWheel, getCore().getClusterChannelGroup())
        {
            private ICancelable mHeartbeatTask;

            @Override
            public void onCreate(ISession<ZContext> session)
            {
                Duration gap = Duration.ofSeconds(session.getReadTimeOutSeconds() / 2);
                mHeartbeatTask = _TimeWheel.acquire(session,
                                                    new ScheduleHandler<>(gap,
                                                                          true,
                                                                          DeviceNode.this::heartbeat,
                                                                          PRIORITY_NORMAL));
            }

            @Override
            public void onDismiss(ISession<ZContext> session)
            {
                mHeartbeatTask.cancel();
                DeviceNode.this.onDismiss(session);
                super.onDismiss(session);
            }
        };
        _Logger.debug("Device Node Bean Load");
    }

    public void start(ILogicHandler<ZContext> logicHandler,
                      ILinkCustom<ZContext> linkCustom,
                      IClusterCustom<ZContext,
                                     RaftMachine> clusterCustom) throws IOException
    {
        getCore().build(this, new EncryptHandler(), logicHandler, linkCustom, clusterCustom);
        for (IAioServer<ZContext> server : _AioServers) {
            server.bindAddress(server.getLocalAddress(), getCore().getServiceChannelGroup());
            server.pendingAccept();
            _Logger.debug(String.format("device node start %s", server.getLocalAddress()));
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
                                           ZSort.WS_CLUSTER_CONSUMER,
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
                                           ZSort.WS_CLUSTER_SYMMETRY,
                                           _ZUID));
    }

    @Override
    public ServerCore<ZContext> getCore()
    {
        return super.getCore();
    }

    private void heartbeat(ISession<ZContext> session)
    {
        getCore().send(session, CLUSTER_LOCAL, _Ping);
    }
}
