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
import com.isahl.chess.bishop.sort.ZSortHolder;
import com.isahl.chess.bishop.io.ssl.SSLZContext;
import com.isahl.chess.bishop.protocol.ws.WsContext;
import com.isahl.chess.bishop.protocol.ws.ctrl.X103_Ping;
import com.isahl.chess.bishop.protocol.ws.features.IWsContext;
import com.isahl.chess.bishop.protocol.zchat.zcrypto.Encryptor;
import com.isahl.chess.king.base.cron.ScheduleHandler;
import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.cron.features.ICancelable;
import com.isahl.chess.king.base.disruptor.components.Health;
import com.isahl.chess.king.base.disruptor.features.debug.IHealth;
import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.events.server.ILogicHandler;
import com.isahl.chess.queen.io.core.features.model.channels.IConnectActivity;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.IDismiss;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
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

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.time.Duration;
import java.util.*;

import static com.isahl.chess.king.base.cron.TimeWheel.IWheelItem.PRIORITY_NORMAL;
import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.SINGLE;

/**
 * @author william.d.zk
 * @date 2019-05-12
 */
@Component
public class ClientPool
        extends AioManager
        implements IAioClient,
                   IDismiss
{
    private final Logger                   _Logger = Logger.getLogger(getClass().getSimpleName());
    private final ClientConfig             _ClientConfig;
    private final AsynchronousChannelGroup _ChannelGroup;
    private final ClientCore               _ClientCore;
    private final TimeWheel                _TimeWheel;
    private final ZUID                     _ZUid   = new ZUID();
    private final Map<Long, Client>        _ZClientMap;
    private final X103_Ping                _Ping   = new X103_Ping();

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
        _ChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(ioCount, _ClientCore.getWorkerThreadFactory());
        _ClientCore.build(slot->new ILogicHandler()
        {
            private final IHealth _Health = new Health(0);

            @Override
            public Logger getLogger()
            {
                return _Logger;
            }

            @Override
            public IManager getISessionManager()
            {
                return ClientPool.this;
            }

            @Override
            public List<ITriple> logicHandle(IManager manager,
                                             ISession session,
                                             IControl content) throws Exception
            {
                return null;
            }

            @Override
            public void serviceHandle(IProtocol request) throws Exception
            {

            }

            @Override
            public IHealth getHealth()
            {
                return _Health;
            }

        }, Encryptor::new);
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

    public void connect(ZSortHolder ZSortHolder, Client client) throws IOException
    {
        final String host;
        final int port;
        switch(ZSortHolder) {
            case WS_ZCHAT_CONSUMER -> {
                host = _ClientConfig.getWs()
                                    .getHost();
                port = _ClientConfig.getWs()
                                    .getPort();
            }
            case QTT_SYMMETRY -> {
                host = _ClientConfig.getQtt()
                                    .getHost();
                port = _ClientConfig.getQtt()
                                    .getPort();
            }
            default -> throw new UnsupportedOperationException();
        }
        BaseAioConnector connector = new BaseAioConnector(host,
                                                          port,
                                                          getSocketConfig(ZUID.TYPE_CONSUMER_SLOT),
                                                          ClientPool.this)
        {
            @Override
            public String getProtocol()
            {
                return ZSortHolder.getSort()
                                  .getProtocol();
            }

            @Override
            public ISort.Mode getMode()
            {
                return ISort.Mode.LINK;
            }

            @Override
            public void onCreated(ISession session)
            {
                //这个地方省略了对session.setIndex(type)的操作，Consumer.type == 0
                super.onCreated(session);
                ClientPool.this.addSession(session);
                _ZClientMap.put(client.getClientId(), client);
                _Logger.debug("client %#x connected %s:%d", client.getClientId(), host, port);
            }

            @Override
            public ISession create(AsynchronousSocketChannel socketChannel,
                                   IConnectActivity activity) throws IOException
            {

                return new AioSession<>(socketChannel, this, ZSortHolder.getSort(), activity, ClientPool.this, false);
            }

            @Override
            public ITriple response(ISession session)
            {
                switch(ZSortHolder) {
                    case WS_ZCHAT_CONSUMER, WS_QTT_CONSUMER -> {
                        IWsContext wsContext = session.getContext();
                        return new Triple<>(wsContext.handshake(host), session, SINGLE);
                    }
                    case WS_ZCHAT_CONSUMER_SSL, WS_QTT_CONSUMER_SSL -> {
                        SSLZContext<WsContext> sslContext = session.getContext();
                        IWsContext wsContext = sslContext.getActingContext();
                        return new Triple<>(wsContext.handshake(host), session, SINGLE);
                    }
                    case QTT_SYMMETRY -> {
                        try {
                            X111_QttConnect x111 = new X111_QttConnect();
                            x111.setClientId(client.getClientToken());
                            x111.setUserName(client.getUsername());
                            x111.setPassword(client.getPassword());
                            x111.setClean();
                            return new Triple<>(x111, session, SINGLE);
                        }
                        catch(Exception e) {
                            _Logger.warning("create init commands fetal", e);
                            return null;
                        }
                    }
                    default -> throw new UnsupportedOperationException();
                }
            }
        };

        connect(connector);

    }

    private ICancelable mHeartbeatTask;

    @Override
    public void onCreated(ISession session)
    {
        Duration gap = Duration.ofSeconds(session.getReadTimeOutSeconds() / 2);
        mHeartbeatTask = _TimeWheel.acquire(session,
                                            new ScheduleHandler<>(gap,
                                                                  true,
                                                                  ClientPool.this::heartbeat,
                                                                  PRIORITY_NORMAL));

    }

    @Override
    public void onDismiss(ISession session)
    {
        mHeartbeatTask.cancel();
        rmSession(session);
        _Logger.warning("device consumer dismiss session %s", session);
    }

    private void heartbeat(ISession session)
    {
        _ClientCore.send(session, IOperator.Type.BIZ_LOCAL, _Ping);
    }

    public final void sendLocal(long sessionIndex, IControl... toSends)
    {
        ISession session = findSessionByIndex(sessionIndex);
        if(Objects.nonNull(session)) {
            _ClientCore.send(session, IOperator.Type.BIZ_LOCAL, toSends);
        }
        else {
            throw new ZException("client-id:%d,is offline;send % failed", sessionIndex, Arrays.toString(toSends));
        }
    }

    public void close(long sessionIndex)
    {
        ISession session = findSessionByIndex(sessionIndex);
        if(Objects.nonNull(session)) {
            _ClientCore.close(session, IOperator.Type.BIZ_LOCAL);
        }
        else {
            throw new ZException("client session is not exist");
        }
    }

    public void wsHeartbeat(long sessionIndex, String msg)
    {
        Objects.requireNonNull(msg);
        sendLocal(sessionIndex, new X103_Ping(msg.getBytes()));
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

}
