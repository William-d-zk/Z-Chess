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

package com.tgx.chess.knight.cluster;

import static com.tgx.chess.king.base.schedule.TimeWheel.IWheelItem.PRIORITY_NORMAL;
import static com.tgx.chess.queen.event.inf.IOperator.Type.CLUSTER_LOCAL;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.time.Duration;

import javax.annotation.PostConstruct;

import com.tgx.chess.bishop.io.ZSort;
import com.tgx.chess.bishop.io.ws.control.X104_Ping;
import com.tgx.chess.bishop.io.zcrypt.EncryptHandler;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.control.X106_Identity;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.ICancelable;
import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.topology.ZUID;
import com.tgx.chess.knight.cluster.spring.IClusterNode;
import com.tgx.chess.knight.raft.config.IRaftConfig;
import com.tgx.chess.knight.raft.model.RaftMachine;
import com.tgx.chess.queen.config.IAioConfig;
import com.tgx.chess.queen.config.IClusterConfig;
import com.tgx.chess.queen.event.handler.IClusterCustom;
import com.tgx.chess.queen.event.handler.cluster.INotifyCustom;
import com.tgx.chess.queen.event.handler.mix.ILogicHandler;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.io.core.async.AioSession;
import com.tgx.chess.queen.io.core.async.BaseAioClient;
import com.tgx.chess.queen.io.core.async.BaseAioServer;
import com.tgx.chess.queen.io.core.executor.ClusterCore;
import com.tgx.chess.queen.io.core.inf.IAioClient;
import com.tgx.chess.queen.io.core.inf.IAioServer;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.inf.ISessionOption;
import com.tgx.chess.queen.io.core.manager.ClusterManager;

public class ClusterNode
        extends
        ClusterManager<ZContext>
        implements
        ISessionDismiss<ZContext>,
        IClusterNode<ClusterCore<ZContext>>
{
    private final Logger               _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());
    private final TimeWheel            _TimeWheel;
    private final IAioServer<ZContext> _AioServer;
    private final IAioClient<ZContext> _GateClient, _PeerClient;
    private final ZUID                 _ZUID;
    private final X104_Ping            _Ping   = new X104_Ping();

    @Override
    public void onDismiss(ISession<ZContext> session)
    {
        _Logger.debug("dismiss %s", session);
        rmSession(session);
    }

    public ClusterNode(IAioConfig config,
                       IClusterConfig clusterConfig,
                       IRaftConfig raftConfig,
                       TimeWheel timeWheel) throws IOException
    {
        super(config, new ClusterCore<>(clusterConfig));
        _TimeWheel = timeWheel;
        _ZUID = raftConfig.createZUID();
        _Logger.debug(_ZUID);
        IPair bind = raftConfig.getBind();
        final String _Host = bind.getFirst();
        final int _Port = bind.getSecond();
        _AioServer = new BaseAioServer<ZContext>(_Host, _Port, getSocketConfig(getSlot(ZUID.TYPE_CLUSTER)))
        {
            @Override
            public ISort<ZContext> getSort()
            {
                return ZSort.WS_CLUSTER_SERVER;
            }

            @Override
            public void onCreate(ISession<ZContext> session)
            {
                session.setIndex(_ZUID.getId());
                ClusterNode.this.addSession(session);
            }

            @Override
            public ISession<ZContext> createSession(AsynchronousSocketChannel socketChannel,
                                                    IConnectActivity<ZContext> activity) throws IOException
            {
                return new AioSession<>(socketChannel, this, this, activity, ClusterNode.this);
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
                X106_Identity x106 = new X106_Identity(_ZUID.getPeerId());
                return new IControl[] { x106 };
            }
        };
        _GateClient = new BaseAioClient<ZContext>(_TimeWheel, getCore().getClusterChannelGroup())
        {

            @Override
            public void onDismiss(ISession<ZContext> session)
            {
                ClusterNode.this.onDismiss(session);
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
                                                                          ClusterNode.this::heartbeat,
                                                                          PRIORITY_NORMAL));
            }

            @Override
            public void onDismiss(ISession<ZContext> session)
            {
                mHeartbeatTask.cancel();
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

    public void start(INotifyCustom notifyCustom,
                      IClusterCustom<ZContext,
                                     RaftMachine> clusterCustom,
                      ILogicHandler<ZContext> logicHandler) throws IOException
    {
        getCore().build(this, new EncryptHandler(), notifyCustom, clusterCustom, logicHandler);
        _AioServer.bindAddress(_AioServer.getLocalAddress(), getCore().getClusterChannelGroup());
        _AioServer.pendingAccept();
        _Logger.debug(String.format("cluster start: %s", _AioServer.getLocalAddress()));
    }

    @Override
    public void addPeer(IPair remote) throws IOException
    {
        _PeerClient.connect(buildConnector(remote,
                                           getSocketConfig(ZUID.TYPE_CLUSTER_SLOT),
                                           _PeerClient,
                                           ZUID.TYPE_CLUSTER,
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
    public ClusterCore<ZContext> getCore()
    {
        return super.getCore();
    }

    private void heartbeat(ISession<ZContext> session)
    {
        _Logger.debug("cluster heartbeat");
        getCore().send(session, CLUSTER_LOCAL, _Ping);
    }
}
