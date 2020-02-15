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

import static com.tgx.chess.queen.io.core.inf.IQoS.Level.ALMOST_ONCE;
import static com.tgx.chess.queen.io.core.inf.IQoS.Level.EXACTLY_ONCE;
import static java.lang.Integer.min;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.*;
import java.util.stream.Collectors;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.bishop.biz.config.IClusterConfig;
import com.tgx.chess.bishop.biz.db.dao.DeviceEntry;
import com.tgx.chess.bishop.biz.db.dao.MessageEntry;
import com.tgx.chess.bishop.biz.db.dao.ZUID;
import com.tgx.chess.bishop.io.ZSort;
import com.tgx.chess.bishop.io.mqtt.bean.QttContext;
import com.tgx.chess.bishop.io.mqtt.control.*;
import com.tgx.chess.bishop.io.mqtt.handler.IQttRouter;
import com.tgx.chess.bishop.io.mqtt.handler.QttRouter;
import com.tgx.chess.bishop.io.ws.control.X101_HandShake;
import com.tgx.chess.bishop.io.ws.control.X103_Close;
import com.tgx.chess.bishop.io.ws.control.X104_Ping;
import com.tgx.chess.bishop.io.ws.control.X105_Pong;
import com.tgx.chess.bishop.io.zcrypt.EncryptHandler;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zhandler.ZLinkedControl;
import com.tgx.chess.bishop.io.zprotocol.control.X106_Identity;
import com.tgx.chess.bishop.io.zprotocol.device.*;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.config.IBizIoConfig;
import com.tgx.chess.queen.config.IServerConfig;
import com.tgx.chess.queen.config.QueenCode;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.event.handler.LogicHandler;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.async.AioSession;
import com.tgx.chess.queen.io.core.async.BaseAioClient;
import com.tgx.chess.queen.io.core.async.BaseAioConnector;
import com.tgx.chess.queen.io.core.async.BaseAioServer;
import com.tgx.chess.queen.io.core.executor.ServerCore;
import com.tgx.chess.queen.io.core.inf.*;
import com.tgx.chess.queen.io.core.manager.QueenManager;

/**
 * @author william.d.zk
 * @date 2019-05-12
 */
public class DeviceNode
        extends
        QueenManager<ZContext>
        implements
        ISessionDismiss<ZContext>
{

    private final List<IAioServer<ZContext>>    _AioServers;
    private final IRepository<IPair>            _DeviceRepository;
    private final IQttRouter                    _QttRouter = new QttRouter();
    private final IAioClient<ZContext>          _ClusterClient;
    private final List<IAioConnector<ZContext>> _ClusterConnectors;
    private final IAioClient<ZContext>          _GateClient;
    private final List<IAioConnector<ZContext>> _GateConnectors;

    @Override
    public void onDismiss(ISession<ZContext> session)
    {
        rmSession(session);
    }

    public DeviceNode(List<ITriple> hosts,
                      IRepository<IPair> deviceRepository,
                      IBizIoConfig bizIoConfig,
                      IClusterConfig clusterConfig,
                      IServerConfig serverConfig) throws IOException
    {
        super(bizIoConfig, new ServerCore<ZContext>(serverConfig)
        {
            @Override
            public RingBuffer<QEvent> getLocalPublisher(ISession<ZContext> session)
            {
                switch (getSlot(session))
                {
                    case QueenCode.CU_XID_LOW:
                        return getBizLocalSendEvent();
                    default:
                        return getClusterLocalSendEvent();
                }
            }

            @Override
            public RingBuffer<QEvent> getLocalCloser(ISession<ZContext> session)
            {
                switch (getSlot(session))
                {
                    case QueenCode.CU_XID_LOW:
                        return getBizLocalCloseEvent();
                    default:
                        return getClusterLocalCloseEvent();

                }
            }
        });
        IPair bind = clusterConfig.getBind();
        hosts.add(new Triple<>(bind.first(), bind.second(), ZSort.WS_CLUSTER_SERVER));
        _AioServers = hosts.stream()
                           .map(triple ->
                           {
                               final String _Host = triple.first();
                               final int _Port = triple.second();
                               final ISort<ZContext> _Sort = triple.third();
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
                                       session.setIndex(_SessionInitializeIndex);
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
        List<IPair> clusterPeers = clusterConfig.getPeers();
        List<IPair> parents = clusterConfig.getGates();
        if (parents != null && !parents.isEmpty()) {
            _GateClient = new BaseAioClient<>(getServerCore().getTimeWheel(), getServerCore().getClusterChannelGroup());
            _GateConnectors = new LinkedList<>();
            for (IPair pair : parents) {
                //parent -> RM_XID
                IAioConnector<ZContext> connector = buildConnector(pair, ZSort.WS_CLUSTER_SYMMETRY, clusterConfig);
                connector.attach(_GateClient);
                _GateConnectors.add(connector);
            }
        }
        else {
            _GateClient = null;
            _GateConnectors = null;
        }
        if (clusterPeers != null && !clusterPeers.isEmpty()) {
            _ClusterClient = new BaseAioClient<>(getServerCore().getTimeWheel(),
                                                 getServerCore().getClusterChannelGroup());
            _ClusterConnectors = new LinkedList<>();
            for (IPair pair : clusterPeers) {
                //peer -> CM_XID
                IAioConnector<ZContext> connector = buildConnector(pair, ZSort.WS_CLUSTER_CONSUMER, clusterConfig);
                connector.attach(_ClusterClient);
                _ClusterConnectors.add(connector);
            }
        }
        else {
            _ClusterClient = null;
            _ClusterConnectors = null;
        }
        _DeviceRepository = deviceRepository;
        _Logger.info("Device Node Bean Load");
    }

    @SafeVarargs
    public final void localBizSend(long deviceId, IControl<ZContext>... toSends)
    {
        localSend(deviceId, toSends);
    }

    public void localBizClose(long deviceId)
    {
        localClose(deviceId);
    }

    @SafeVarargs
    public final void localClusterSend(long nodeZuid, IControl<ZContext>... toSends)
    {
        localSend(nodeZuid, toSends);
    }

    public void localClusterClose(long nodeZuid)
    {
        localClose(nodeZuid);
    }

    @SafeVarargs
    private final void localSend(long sessionIndex, IControl<ZContext>... toSends)
    {
        ISession<ZContext> session = findSessionByIndex(sessionIndex);
        if (session != null) {
            localSend(session,
                      session.getContext()
                             .getSort()
                             .getTransfer(),
                      toSends);
        }
    }

    private void localClose(long sessionIndex)
    {
        ISession<ZContext> session = findSessionByIndex(sessionIndex);
        if (session != null) {
            localClose(session,
                       session.getContext()
                              .getSort()
                              .getCloser());
        }
    }

    @SuppressWarnings("unchecked")
    public void start() throws IOException
    {
        _ServerCore.build(new LogicHandler<>((command, session) ->
        {
            //前置的 dispatcher 将 ICommands 拆分了
            _Logger.info(" node logic %s", command);
            switch (command.serial())
            {
                case X30_EventMsg.COMMAND:
                    X30_EventMsg x30 = (X30_EventMsg) command;
                    return new IControl[] { new X31_ConfirmMsg(x30.getMsgId()) };
                case X31_ConfirmMsg.COMMAND:
                    X31_ConfirmMsg x31 = (X31_ConfirmMsg) command;
                    return new IControl[] { new X32_MsgStatus(x31.getMsgId()) };
                case X50_DeviceMsg.COMMAND:
                    X50_DeviceMsg x50 = (X50_DeviceMsg) command;
                    return new IControl[] { new X51_DeviceMsgAck(x50.getMsgId()) };
                case X103_Close.COMMAND:
                    //session is not null
                    localClose(session,
                               session.getContext()
                                      .getSort()
                                      .getCloser());
                    break;
                case X104_Ping.COMMAND:
                    return new IControl[] { new X105_Pong("Server pong".getBytes()) };
                case X101_HandShake.COMMAND:
                    return new IControl[] { command };
                case X51_DeviceMsgAck.COMMAND:
                case X105_Pong.COMMAND:
                    break;
                case X113_QttPublish.COMMAND:
                    X113_QttPublish x113 = (X113_QttPublish) command;
                    _DeviceRepository.receive(command);
                    List<IControl<ZContext>> pushList = new LinkedList<>();
                    switch (x113.getLevel())
                    {
                        case EXACTLY_ONCE:
                            X115_QttPubrec x115 = new X115_QttPubrec();
                            x115.setMsgId(x113.getMsgId());
                            _QttRouter.register(x115, session.getIndex());
                            return new IControl[] { x115 };
                        case AT_LEAST_ONCE:
                            X114_QttPuback x114 = new X114_QttPuback();
                            x114.setMsgId(x113.getMsgId());
                            pushList.add(x114);
                        default:
                            brokerTopic(x113.getTopic(), x113.getPayload(), x113.getLevel(), pushList);
                            return pushList.toArray(new IControl[0]);
                    }
                case X114_QttPuback.COMMAND:
                    X114_QttPuback x114 = (X114_QttPuback) command;
                    _DeviceRepository.receive(x114);
                    _QttRouter.ack(x114, session.getIndex());
                    break;
                case X115_QttPubrec.COMMAND:
                    X115_QttPubrec x115 = (X115_QttPubrec) command;
                    _DeviceRepository.receive(x115);
                    _QttRouter.ack(x115, session.getIndex());
                    X116_QttPubrel x116 = new X116_QttPubrel();
                    x116.setMsgId(x115.getMsgId());
                    _DeviceRepository.send(x116);
                    _QttRouter.register(x116, session.getIndex());
                    return new IControl[] { x116 };
                case X116_QttPubrel.COMMAND:
                    x116 = (X116_QttPubrel) command;
                    X117_QttPubcomp x117 = new X117_QttPubcomp();
                    x117.setMsgId(x116.getMsgId());
                    _QttRouter.ack(x116, session.getIndex());
                    pushList = new LinkedList<>();
                    IPair result = _DeviceRepository.receive(x116);
                    List<MessageEntry> msgList = result.first();
                    msgList.forEach(messageEntry -> brokerTopic(messageEntry.getTopic(),
                                                                messageEntry.getPayload(),
                                                                EXACTLY_ONCE,
                                                                pushList));
                    pushList.add(x117);
                    return pushList.toArray(new IControl[0]);
                case X117_QttPubcomp.COMMAND:
                    x117 = (X117_QttPubcomp) command;
                    _QttRouter.ack(x117, session.getIndex());
                    break;
                case X11C_QttPingreq.COMMAND:
                    return new IControl[] { new X11D_QttPingresp() };
                default:
                    break;
            }
            return null;
        }), this, new ZLinkedControl(), new EncryptHandler());
        for (IAioServer<ZContext> server : _AioServers) {
            server.bindAddress(server.getLocalAddress(), _ServerCore.getServiceChannelGroup());
            server.pendingAccept();
            _Logger.info(String.format("device node start %s", server.getLocalAddress()));
        }
        if (_GateClient != null && _GateConnectors != null) {
            for (IAioConnector<ZContext> connector : _GateConnectors) {
                _GateClient.connect(connector);
                _Logger.info(String.format("device router gate connect %s", connector.getRemoteAddress()));
            }
        }
        if (_ClusterClient != null && _ClusterConnectors != null) {
            for (IAioConnector<ZContext> connector : _ClusterConnectors) {
                _ClusterClient.connect(connector);
                _Logger.info(String.format("device cluster connect %s", connector.getRemoteAddress()));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public IControl<ZContext>[] mappingHandle(IControl<ZContext> input, ISession<ZContext> session)
    {
        _Logger.info("Manage LinkHandle mappingHandle input %s", input);
        switch (input.serial())
        {
            case X20_SignUp.COMMAND:
                IPair result = _DeviceRepository.save(input);
                X21_SignUpResult x21 = result.second();
                DeviceEntry deviceEntry = result.first();
                if (x21.isSuccess()) {
                    mapSession(deviceEntry.getPrimaryKey(), session);
                }
                return new IControl[] { x21 };
            case X22_SignIn.COMMAND:
                result = _DeviceRepository.find(input);
                X23_SignInResult x23 = result.second();
                if (x23.isSuccess()) {
                    deviceEntry = result.first();
                    mapSession(deviceEntry.getPrimaryKey(), session);
                }
                else {
                    x23.setFailed();
                }
                return new IControl[] { x23 };
            case X111_QttConnect.COMMAND:
                X111_QttConnect x111 = (X111_QttConnect) input;
                result = _DeviceRepository.find(x111);
                X112_QttConnack x112 = result.second();
                int[] supportVersions = QttContext.getSupportVersion()
                                                  .second();
                if (Arrays.stream(supportVersions)
                          .noneMatch(version -> version == x111.getProtocolVersion()))
                {
                    x112.rejectUnacceptableProtocol();
                }
                else if (!x111.isClean() && x111.getClientIdLength() == 0) {
                    x112.rejectIdentifier();
                }
                else if (!x112.isIllegalState()) {
                    deviceEntry = result.first();
                    mapSession(deviceEntry.getPrimaryKey(), session);
                }
                return new IControl[] { x112 };
            case X118_QttSubscribe.COMMAND:
                X118_QttSubscribe x118 = (X118_QttSubscribe) input;
                X119_QttSuback x119 = new X119_QttSuback();
                x119.setMsgId(x118.getMsgId());
                List<Pair<String,
                          IQoS.Level>> topics = x118.getTopics();
                if (topics != null) {
                    topics.forEach(topic -> x119.addResult(_QttRouter.addTopic(topic,
                                                                               session.getIndex()) ? topic.second()
                                                                                                   : IQoS.Level.FAILURE));
                }
                return new IControl[] { x119 };
            case X11A_QttUnsubscribe.COMMAND:
                X11A_QttUnsubscribe x11A = (X11A_QttUnsubscribe) input;
                x11A.getTopics()
                    .forEach(topic -> _QttRouter.removeTopic(topic, session.getIndex()));
                X11B_QttUnsuback x11B = new X11B_QttUnsuback();
                x11B.setMsgId(x11A.getMsgId());
                return new IControl[] { x11B };
        }
        return new IControl[0];
    }

    private void brokerTopic(String topic, byte[] payload, IQoS.Level level, List<IControl<ZContext>> pushList)
    {
        Objects.requireNonNull(pushList);
        Objects.requireNonNull(topic);
        Objects.requireNonNull(payload);
        Objects.requireNonNull(level);
        Map<Long,
            IQoS.Level> route = _QttRouter.broker(topic);
        _Logger.debug("route %s", route);
        route.entrySet()
             .stream()
             .map(entry ->
             {
                 ISession<ZContext> targetSession = findSessionByIndex(entry.getKey());
                 if (targetSession != null) {
                     IQoS.Level subscribeLevel = entry.getValue();
                     X113_QttPublish push = new X113_QttPublish();
                     push.setLevel(IQoS.Level.valueOf(min(subscribeLevel.getValue(), level.getValue())));
                     push.setTopic(topic);
                     push.setPayload(payload);
                     push.setSession(targetSession);
                     if (push.getLevel() == ALMOST_ONCE) { return push; }
                     long packIdentity = _QttRouter.nextPackIdentity();
                     push.setMsgId(packIdentity);
                     _DeviceRepository.send(push);
                     _QttRouter.register(push, entry.getKey());
                     return push;
                 }
                 return null;
             })
             .filter(Objects::nonNull)
             .forEach(push ->
             {
                 _DeviceRepository.find(push);
                 pushList.add(push);
             });
        _Logger.info("push %s", pushList);
    }

    private IAioConnector<ZContext> buildConnector(IPair address, ZSort sort, IClusterConfig clusterConfig)
    {
        {
            final String _Host = address.first();
            final int _Port = address.second();
            final ISort<ZContext> _Sort = sort;
            final ZUID _ZUid = clusterConfig.createZUID();

            ISort.Mode mode = _Sort.getMode();
            ISort.Type type = _Sort.getType();
            final long _SessionInitializeIndex;
            if (mode == ISort.Mode.CLUSTER && type == ISort.Type.SYMMETRY) {
                _SessionInitializeIndex = QueenCode.RM_XID | _ZUid.getNoTypeId();
            }
            else {
                _SessionInitializeIndex = QueenCode.CM_XID | _ZUid.getNoTypeId();
            }
            return new BaseAioConnector<ZContext>(_Host,
                                                  _Port,
                                                  getSocketConfig(getSlot(_SessionInitializeIndex)),
                                                  getServerCore())
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
                    return new AioSession<>(socketChannel, this, this, activity, DeviceNode.this);
                }

                @Override
                public void onCreate(ISession<ZContext> session)
                {
                    session.setIndex(_SessionInitializeIndex);
                    DeviceNode.this.addSession(session);
                }

                @Override
                @SuppressWarnings("unchecked")
                public IControl<ZContext>[] createCommands(ISession<ZContext> session)
                {
                    X106_Identity x106 = new X106_Identity(_ZUid.getName());
                    return new IControl[] { x106 };
                }

                @Override
                public ISort<ZContext> getSort()
                {
                    return _Sort;
                }
            };
        }
    }
}
