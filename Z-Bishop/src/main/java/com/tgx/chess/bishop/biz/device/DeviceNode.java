/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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

package com.tgx.chess.bishop.biz.device;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;
import java.util.stream.Collectors;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.bishop.ZUID;
import com.tgx.chess.bishop.biz.config.IClusterConfig;
import com.tgx.chess.bishop.io.ZSort;
import com.tgx.chess.bishop.io.zcrypt.EncryptHandler;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.control.X106_Identity;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.config.IBizIoConfig;
import com.tgx.chess.queen.config.IServerConfig;
import com.tgx.chess.queen.config.QueenCode;
import com.tgx.chess.queen.event.inf.ICustomLogic;
import com.tgx.chess.queen.event.inf.ILogicHandler;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.async.AioSession;
import com.tgx.chess.queen.io.core.async.BaseAioClient;
import com.tgx.chess.queen.io.core.async.BaseAioConnector;
import com.tgx.chess.queen.io.core.async.BaseAioServer;
import com.tgx.chess.queen.io.core.executor.ServerCore;
import com.tgx.chess.queen.io.core.inf.IAioClient;
import com.tgx.chess.queen.io.core.inf.IAioConnector;
import com.tgx.chess.queen.io.core.inf.IAioServer;
import com.tgx.chess.queen.io.core.inf.IClusterPeer;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.inf.ISessionOption;
import com.tgx.chess.queen.io.core.manager.QueenManager;

/**
 * @author william.d.zk
 * @date 2019-05-12
 */
public class DeviceNode
        extends
        QueenManager<ZContext>
        implements
        ISessionDismiss<ZContext>,
        IClusterPeer
{

    private final List<IAioServer<ZContext>> _AioServers;
    private final IAioClient<ZContext>       _ClusterClient;
    private final IAioClient<ZContext>       _GateClient;
    private final TimeWheel                  _TimeWheel;
    private final ZUID                       _ZUID;

    @Override
    public void onDismiss(ISession<ZContext> session)
    {
        _Logger.info("dismiss %s", session);
        rmSession(session);
    }

    public DeviceNode(List<ITriple> hosts,
                      IBizIoConfig bizIoConfig,
                      IClusterConfig clusterConfig,
                      IServerConfig serverConfig,
                      TimeWheel timeWheel) throws IOException
    {
        super(bizIoConfig, new ServerCore<ZContext>(serverConfig)
        {
            @Override
            public RingBuffer<QEvent> getLocalPublisher(ISession<ZContext> session)
            {
                return getSlot(session) == QueenCode.CU_XID_LOW ? getBizLocalSendEvent()
                                                                : getClusterLocalSendEvent();
            }

            @Override
            public RingBuffer<QEvent> getLocalCloser(ISession<ZContext> session)
            {
                return getSlot(session) == QueenCode.CU_XID_LOW ? getBizLocalCloseEvent()
                                                                : getClusterLocalCloseEvent();
            }
        });
        _TimeWheel = timeWheel;
        _ZUID = clusterConfig.createZUID(true);
        IPair bind = clusterConfig.getBind();
        hosts.add(new Triple<>(bind.getFirst(), bind.getSecond(), ZSort.WS_CLUSTER_SERVER));
        _AioServers = hosts.stream()
                           .map(triple ->
                           {
                               final String _Host = triple.getFirst();
                               final int _Port = triple.getSecond();
                               final ISort<ZContext> _Sort = triple.getThird();
                               ISort.Mode mode = _Sort.getMode();
                               ISort.Type type = _Sort.getType();
                               final long _SessionInitializeIndex;
                               if (mode == ISort.Mode.CLUSTER && type == ISort.Type.SYMMETRY) {
                                   _SessionInitializeIndex = _Sort == ZSort.MQ_QTT_SYMMETRY ? QueenCode.MQ_XID
                                                                                            : QueenCode.RM_XID;
                               }
                               else if (mode == ISort.Mode.CLUSTER) {
                                   _SessionInitializeIndex = QueenCode.CM_XID;
                               }
                               else {
                                   _SessionInitializeIndex = QueenCode.CU_XID;
                               }
                               return new BaseAioServer<ZContext>(_Host,
                                                                  _Port,
                                                                  getSocketConfig(getSlot(_SessionInitializeIndex)))
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
                                       session.setIndex(_SessionInitializeIndex | _ZUID.getNoTypeId());
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
                                       return sort.newContext(option, this);
                                   }
                               };
                           })
                           .collect(Collectors.toList());
        _GateClient = new BaseAioClient<ZContext>(getTimeWheel(), getServerCore().getClusterChannelGroup())
        {
            @Override
            public void onDismiss(ISession<ZContext> session)
            {
                DeviceNode.this.onDismiss(session);
                super.onDismiss(session);
            }
        };
        _ClusterClient = new BaseAioClient<ZContext>(getTimeWheel(), getServerCore().getClusterChannelGroup())
        {
            @Override
            public void onDismiss(ISession<ZContext> session)
            {
                DeviceNode.this.onDismiss(session);
                super.onDismiss(session);
            }
        };
        _Logger.info("Device Node Bean Load");
    }

    private TimeWheel getTimeWheel()
    {
        return _TimeWheel;
    }

    @SafeVarargs
    public final void localBizSend(long deviceId, IControl<ZContext>... toSends)
    {
        sendByIndex(deviceId, toSends);
    }

    public void bizClose(long deviceId)
    {
        closeByIndex(deviceId);
    }

    @SafeVarargs
    public final void clusterSend(long peerId, IControl<ZContext>... toSends)
    {
        sendByPrefix(peerId, toSends);
    }

    public void clusterClose(long peerId)
    {
        closeByPrefix(peerId);
    }

    private void closeByIndex(long sessionIndex)
    {
        close(findSessionByIndex(sessionIndex));
    }

    private void closeByPrefix(long sessionPrefix)
    {
        close(findSessionByPrefix(sessionPrefix));
    }

    private void sendByIndex(long sessionIndex, IControl<ZContext>... toSends)
    {
        send(findSessionByIndex(sessionIndex), toSends);
    }

    private void sendByPrefix(long sessionPrefix, IControl<ZContext>... toSends)
    {
        send(findSessionByPrefix(sessionPrefix), toSends);
    }

    public void start(ILogicHandler<ZContext> logicHandler,
                      ICustomLogic<ZContext> linkCustom,
                      ICustomLogic<ZContext> clusterCustom) throws IOException
    {
        _ServerCore.build(this, new EncryptHandler(), logicHandler, linkCustom, clusterCustom);
        for (IAioServer<ZContext> server : _AioServers) {
            server.bindAddress(server.getLocalAddress(), _ServerCore.getServiceChannelGroup());
            server.pendingAccept();
            _Logger.info(String.format("device node start %s", server.getLocalAddress()));
        }
    }

    private IAioConnector<ZContext> buildConnector(IPair address, ZSort sort, IAioClient<ZContext> client)
    {
        final String _Host = address.getFirst();
        final int _Port = address.getSecond();
        final ISort<ZContext> _Sort = sort;
        ISort.Mode mode = _Sort.getMode();
        ISort.Type type = _Sort.getType();
        final long _SessionInitializeIndex;
        if (mode == ISort.Mode.CLUSTER && type == ISort.Type.SYMMETRY) {
            _SessionInitializeIndex = QueenCode.RM_XID;
        }
        else {
            _SessionInitializeIndex = QueenCode.CM_XID;
        }
        return new BaseAioConnector<ZContext>(_Host, _Port, getSocketConfig(getSlot(_SessionInitializeIndex)), client)
        {

            @Override
            public ZContext createContext(ISessionOption option, ISort<ZContext> sort)
            {
                return sort.newContext(option, this);
            }

            @Override
            public ISession<ZContext> createSession(AsynchronousSocketChannel socketChannel,
                                                    IConnectActivity<ZContext> activity) throws IOException
            {
                return new AioSession<>(socketChannel, this, this, activity, client);
            }

            @Override
            public void onCreate(ISession<ZContext> session)
            {
                session.setIndex(_SessionInitializeIndex | _ZUID.getNoTypeId());
                DeviceNode.this.addSession(session);
            }

            @Override
            @SuppressWarnings("unchecked")
            public IControl<ZContext>[] createCommands(ISession<ZContext> session)
            {
                X106_Identity x106 = new X106_Identity(_ZUID.getPeerId());
                return new IControl[] { x106 };
            }

            @Override
            public ISort<ZContext> getSort()
            {
                return _Sort;
            }
        };
    }

    @Override
    public void addPeer(IPair remote) throws IOException
    {
        _ClusterClient.connect(buildConnector(remote, ZSort.WS_CLUSTER_CONSUMER, _ClusterClient));

    }

    @Override
    public void addGate(IPair remote) throws IOException
    {
        _GateClient.connect(buildConnector(remote, ZSort.WS_CLUSTER_SYMMETRY, _GateClient));
    }
}
