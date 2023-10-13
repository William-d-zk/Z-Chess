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

package com.isahl.chess.audience.client.component;

import com.isahl.chess.audience.client.config.ClientConfig;
import com.isahl.chess.audience.client.model.Client;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X111_QttConnect;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X11C_QttPingreq;
import com.isahl.chess.bishop.protocol.ws.ctrl.X103_Ping;
import com.isahl.chess.bishop.protocol.zchat.zcrypto.Encryptor;
import com.isahl.chess.bishop.sort.ZSortHolder;
import com.isahl.chess.king.base.cron.ScheduleHandler;
import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.cron.features.ICancelable;
import com.isahl.chess.king.base.disruptor.components.Health;
import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.io.core.features.model.channels.IActivity;
import com.isahl.chess.queen.io.core.features.model.channels.IConnectActivity;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IDismiss;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.isahl.chess.queen.io.core.net.socket.AioManager;
import com.isahl.chess.queen.io.core.net.socket.AioSession;
import com.isahl.chess.queen.io.core.net.socket.BaseAioConnector;
import com.isahl.chess.queen.io.core.net.socket.features.IAioConnector;
import com.isahl.chess.queen.io.core.net.socket.features.client.IAioClient;
import com.isahl.chess.queen.io.core.tasks.ClientCore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.SINGLE;

/**
 * @author william.d.zk
 * @date 2019-05-12
 */
@Component
public class ClientPool
        extends AioManager
        implements IAioClient,
                   IDismiss,
                   IActivity
{
    private final Logger _Logger = Logger.getLogger(getClass().getSimpleName());

    private final ClientConfig                       _ClientConfig;
    private final AsynchronousChannelGroup           _ChannelGroup;
    private final ClientCore                         _ClientCore;
    private final TimeWheel                          _TimeWheel;
    private final Map<Long, Client>                  _ZClientMap;
    private final Map<Integer, IoFactory<IProtocol>> _FactoryMap;

    @Autowired
    public ClientPool(
            @Qualifier("io_consumer_config")
            IAioConfig bizIoConfig, ClientConfig clientConfig) throws IOException
    {
        super(bizIoConfig);
        _ZClientMap = new HashMap<>(1 << getConfigPower(getSlot(ZUID.TYPE_CONSUMER)));
        _ClientConfig = clientConfig;
        int ioCount = _ClientConfig.getIoCount();
        _ClientCore = new ClientCore(ioCount);
        _TimeWheel = _ClientCore.getTimeWheel();
        _FactoryMap = new HashMap<>();
        _ChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(ioCount, _ClientCore.getWorkerThreadFactory());
        _ClientCore.build(slot->new ClientHandler(new Health(slot), ClientPool.this), Encryptor::new);
        _Logger.debug("device consumer created");
    }

    @PostConstruct
    private void init()
    {

    }

    @Override
    public void connect(IAioConnector connector) throws IOException
    {
        if(_ChannelGroup.isShutdown() || connector.isShutdown()) {return;}
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(_ChannelGroup);
        socketChannel.connect(connector.getRemoteAddress(), socketChannel, connector);
    }

    @Override
    public void shutdown(ISession session)
    {

    }

    public void connect(ZSortHolder zsort, Client client) throws IOException
    {
        final String host;
        final int port;
        switch(zsort) {
            case Z_CLUSTER_SYMMETRY -> {
                host = _ClientConfig.getChat()
                                    .getHost();
                port = _ClientConfig.getChat()
                                    .getPort();
            }
            case QTT_CONSUMER -> {
                host = _ClientConfig.getQtt()
                                    .getHost();
                port = _ClientConfig.getQtt()
                                    .getPort();
            }
            case WS_PLAIN_TEXT_CONSUMER -> {
                host = _ClientConfig.getWs()
                                    .getHost();
                port = _ClientConfig.getWs()
                                    .getPort();
            }
            default -> throw new UnsupportedOperationException();
        }
        BaseAioConnector connector = new BaseAioConnector(host, port, getSocketConfig(ZUID.TYPE_CONSUMER_SLOT), ClientPool.this)
        {
            @Override
            public String getProtocol()
            {
                return zsort.getSort()
                            .getProtocol();
            }

            @Override
            public ISort.Mode getMode()
            {
                return zsort.getSort()
                            .getMode();
            }

            @Override
            public void onCreated(ISession session)
            {
                //这个地方省略了对session.setIndex(type)的操作，Consumer.type == 0
                super.onCreated(session);
                ClientPool.this.addSession(session);
                client.setSession(session);
            }

            @Override
            public ISession create(AsynchronousSocketChannel socketChannel, IConnectActivity activity) throws IOException
            {
                return new AioSession<>(socketChannel, this, zsort.getSort(), activity, ClientPool.this, false);
            }

            @Override
            public ITriple afterConnected(ISession session)
            {
                if (zsort == ZSortHolder.QTT_CONSUMER) {
                    X111_QttConnect x111 = new X111_QttConnect();
                    x111.setClientId("3FA073405AC0BF4B348BFBA7FAAE86B09A33643A88EF24AEBA69F8BB87D39B28");
                    x111.setUserName("lab-meter");
                    x111.setPassword("Yh6@Y3*5teP~#y67n_j0L4");
                    x111.setClean();
                    x111.setKeepAlive(60);
                    mHeartbeatTask = _TimeWheel.acquire(session, new ScheduleHandler<>(Duration.ofSeconds(60), true, ClientPool.this::qttHeartbeat));
                    return new Triple<>(x111, session, SINGLE);
                }
                return null;
            }
        };
        IoFactory<IProtocol> factory = zsort.getSort()
                                            .getFactory();
        _FactoryMap.putIfAbsent(factory.serial(), factory);
        connect(connector);
    }

    private ICancelable mHeartbeatTask;

    @Override
    public void onCreated(ISession session)
    {
        Duration gap = Duration.ofSeconds(session.getReadTimeOutSeconds() / 2);
    }

    @Override
    public void onDismiss(ISession session)
    {
        if(mHeartbeatTask != null) {mHeartbeatTask.cancel();}
        rmSession(session);
        _Logger.warning("device consumer dismiss session %s", session);
    }

    public final void sendLocal(long sessionIndex, IProtocol... toSends)
    {
        ISession session = findSessionByIndex(sessionIndex);
        if(session != null) {
            send(session, IOperator.Type.BIZ_LOCAL, toSends);
        }
        else {
            throw new ZException("client-id:%d,is offline;send % failed", sessionIndex, Arrays.toString(toSends));
        }
    }

    public final void sendLocal(ISession session, IProtocol... request)
    {
        if(session != null) {
            send(session, IOperator.Type.BIZ_LOCAL, request);
        }
        else {
            throw new ZException("client offline");
        }
    }

    public void close(long sessionIndex)
    {
        ISession session = findSessionByIndex(sessionIndex);
        if(Objects.nonNull(session)) {
            close(session, IOperator.Type.BIZ_LOCAL);
        }
        else {
            throw new ZException("client session is not exist");
        }
    }

    public void qttHeartbeat(ISession session)
    {
        send(session, IOperator.Type.BIZ_LOCAL, new X11C_QttPingreq());
    }

    public void wsHeartbeat(ISession session)
    {
        send(session, IOperator.Type.BIZ_LOCAL, new X103_Ping<>());
    }

    @Override
    public void onFailed(IAioConnector connector)
    {
        _TimeWheel.acquire(connector,
                           new ScheduleHandler<>(connector.getConnectTimeout()
                                                          .multipliedBy(3), c->{
                               try {
                                   _Logger.debug("%s retry connect",
                                                 Thread.currentThread()
                                                       .getName());
                                   connect(c);
                               }
                               catch(IOException e) {
                                   e.printStackTrace();
                               }
                           }));
    }

    @Override
    public boolean send(ISession session, IOperator.Type type, IProtocol... outputs)
    {
        if(session == null || outputs == null || outputs.length == 0) {return false;}
        return _ClientCore.publish(type,
                                   session.encoder(),
                                   Stream.of(outputs)
                                         .map(pro->Pair.of(pro, session))
                                         .toArray(IPair[]::new));
    }

    @Override
    public void close(ISession session, IOperator.Type type)
    {
        if(session == null) {return;}
        _ClientCore.close(type, Pair.of(null, session), session.getCloser());
    }

    @Override
    public IoFactory<IProtocol> findIoFactoryBySerial(int factorySerial)
    {
        return _FactoryMap.get(factorySerial);
    }

    @Override
    public void exchange(IProtocol body, long target, int factory, List<ITriple> load)
    {
        throw new UnsupportedOperationException("client unsupported exchange,no routing");
    }
}

