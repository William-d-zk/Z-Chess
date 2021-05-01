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

import com.isahl.chess.audience.client.config.ConsumerConfig;
import com.isahl.chess.audience.client.model.Client;
import com.isahl.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.isahl.chess.bishop.io.mqtt.control.X112_QttConnack;
import com.isahl.chess.bishop.io.sort.ZSortHolder;
import com.isahl.chess.bishop.io.ssl.SSLZContext;
import com.isahl.chess.bishop.io.ws.IWsContext;
import com.isahl.chess.bishop.io.ws.WsContext;
import com.isahl.chess.bishop.io.ws.control.X101_HandShake;
import com.isahl.chess.bishop.io.ws.control.X102_Close;
import com.isahl.chess.bishop.io.ws.control.X103_Ping;
import com.isahl.chess.bishop.io.ws.control.X104_Pong;
import com.isahl.chess.bishop.io.ws.zchat.zcrypt.EncryptHandler;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.device.X21_SignUpResult;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.device.X22_SignIn;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.device.X23_SignInResult;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.device.X30_EventMsg;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.device.X31_ConfirmMsg;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.zls.X03_Cipher;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.zls.X05_EncryptStart;
import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.schedule.ScheduleHandler;
import com.isahl.chess.king.base.schedule.TimeWheel;
import com.isahl.chess.king.base.schedule.inf.ICancelable;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.event.QEvent;
import com.isahl.chess.queen.io.core.async.AioSession;
import com.isahl.chess.queen.io.core.async.AioSessionManager;
import com.isahl.chess.queen.io.core.async.BaseAioConnector;
import com.isahl.chess.queen.io.core.executor.ClientCore;
import com.isahl.chess.queen.io.core.inf.IAioClient;
import com.isahl.chess.queen.io.core.inf.IAioConnector;
import com.isahl.chess.queen.io.core.inf.IConnectActivity;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionDismiss;
import com.isahl.chess.queen.io.core.inf.ISort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.isahl.chess.king.base.schedule.TimeWheel.IWheelItem.PRIORITY_NORMAL;

/**
 * @author william.d.zk
 * 
 * @date 2019-05-12
 */
@Component
public class ClientPool
        extends
        AioSessionManager<ClientCore>
        implements
        IAioClient,
        ISessionDismiss
{
    private final Logger                   _Logger = Logger.getLogger(getClass().getSimpleName());
    private final ConsumerConfig           _ConsumerConfig;
    private final AsynchronousChannelGroup _ChannelGroup;
    private final ClientCore               _ClientCore;
    private final TimeWheel                _TimeWheel;
    private final ZUID                     _ZUid   = new ZUID();
    private final Map<Long,
                      Client>              _ZClientMap;
    private final X103_Ping                _Ping   = new X103_Ping();

    @Autowired
    public ClientPool(IAioConfig bizIoConfig,
                      ConsumerConfig consumerConfig) throws IOException
    {
        super(bizIoConfig);
        _ZClientMap = new HashMap<>(1 << getConfigPower(getSlot(ZUID.TYPE_PROVIDER)));
        _ConsumerConfig = consumerConfig;
        int ioCount = _ConsumerConfig.getIoCount();
        _ClientCore = new ClientCore(ioCount);
        _TimeWheel = _ClientCore.getTimeWheel();
        _ChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(ioCount, _ClientCore.getWorkerThreadFactory());
        _ClientCore.build((QEvent event, long sequence, boolean endOfBatch) ->
        {
            IControl[] commands;
            final ISession session;
            if (event.getEventType() == OperatorType.LOGIC) {// 与 Server Node 处理过程存在较大的差异，中间去掉一个decoded dispatcher 所以此处入参为 IControl[]
                IPair logicContent = event.getContent();
                commands = logicContent.getFirst();
                session = logicContent.getSecond();
                if (Objects.nonNull(commands)) {
                    commands = Stream.of(commands)
                                     .map(cmd ->
                                     {
                                         _Logger.debug("recv:{ %s }", cmd);
                                         switch (cmd.serial())
                                         {
                                             case X03_Cipher.COMMAND:
                                             case X05_EncryptStart.COMMAND:
                                                 return cmd;
                                             case X21_SignUpResult.COMMAND:
                                                 X21_SignUpResult x21 = (X21_SignUpResult) cmd;
                                                 X22_SignIn x22 = new X22_SignIn();
                                                 byte[] token = x22.getToken();
                                                 Client client = _ZClientMap.get(session.getIndex());
                                                 if (client == null) {
                                                     _Logger.warning("client %x not found", session.getIndex());
                                                 }
                                                 else {
                                                     client.setToken(IoUtil.bin2Hex(token));
                                                 }
                                                 x22.setToken(token);
                                                 x22.setPassword("password");
                                                 return x22;
                                             case X23_SignInResult.COMMAND:
                                                 X23_SignInResult x23 = (X23_SignInResult) cmd;
                                                 if (x23.isSuccess()) {
                                                     _Logger.debug("sign in success token invalid @ %s",
                                                                   Instant.ofEpochMilli(x23.getInvalidTime())
                                                                          .atZone(ZoneId.of("GMT+8")));
                                                 }
                                                 else {
                                                     return new X102_Close("sign in failed! ws_close".getBytes());
                                                 }
                                                 break;
                                             case X30_EventMsg.COMMAND:
                                                 X30_EventMsg x30 = (X30_EventMsg) cmd;
                                                 _Logger.debug("x30 payload: %s",
                                                               new String(x30.getPayload(), StandardCharsets.UTF_8));
                                                 X31_ConfirmMsg x31 = new X31_ConfirmMsg(x30.getMsgId());
                                                 x31.setStatus(X31_ConfirmMsg.STATUS_RECEIVED);
                                                 x31.setToken(x30.getToken());
                                                 return x31;
                                             case X101_HandShake.COMMAND:
                                                 _Logger.debug("ws_handshake ok");
                                                 break;
                                             case X104_Pong.COMMAND:
                                                 _Logger.debug("ws_heartbeat ok");
                                                 break;
                                             case X102_Close.COMMAND:
                                                 close(session.getIndex());
                                                 break;
                                             case X112_QttConnack.COMMAND:
                                                 _Logger.debug("qtt connack %s", cmd);
                                                 break;
                                             default:
                                                 break;
                                         }
                                         return null;
                                     })
                                     .filter(Objects::nonNull)
                                     .toArray(IControl[]::new);
                }
            }
            else {
                _Logger.warning("event type no mappingHandle %s", event.getEventType());
                commands = null;
                session = null;
            }
            if (Objects.nonNull(commands) && commands.length > 0 && Objects.nonNull(session)) {
                event.produce(OperatorType.WRITE, new Pair<>(commands, session), session.getTransfer());
            }
            else {
                event.ignore();
            }
        }, EncryptHandler::new);
        _Logger.debug("device consumer created");
    }

    @PostConstruct
    private void init()
    {

    }

    @Override
    public void connect(IAioConnector connector) throws IOException
    {
        if (_ChannelGroup.isShutdown() || connector.isShutdown()) return;
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
        switch (ZSortHolder)
        {
            case WS_ZCHAT_CONSUMER ->
                {
                    host = _ConsumerConfig.getWs()
                                          .getHost();
                    port = _ConsumerConfig.getWs()
                                          .getPort();
                }
            case QTT_SYMMETRY ->
                {
                    host = _ConsumerConfig.getQtt()
                                          .getHost();
                    port = _ConsumerConfig.getQtt()
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
            public void onCreate(ISession session)
            {
                //这个地方省略了对session.setIndex(type)的操作，Consumer.type == 0
                super.onCreate(session);
                ClientPool.this.addSession(session);
                _ZClientMap.put(client.getClientId(), client);
                _Logger.debug("client %x connected %s:%d", client.getClientId(), host, port);
            }

            @Override
            public ISession createSession(AsynchronousSocketChannel socketChannel,
                                          IConnectActivity activity) throws IOException
            {

                return new AioSession<>(socketChannel,
                                        ZUID.TYPE_CONSUMER,
                                        this,
                                        ZSortHolder.getSort(),
                                        activity,
                                        ClientPool.this,
                                        false);
            }

            @Override
            public IControl[] createCommands(ISession session)
            {
                switch (ZSortHolder)
                {
                    case WS_ZCHAT_CONSUMER:
                    case WS_QTT_CONSUMER:
                        IWsContext wsContext = session.getContext();
                        return new IControl[]{wsContext.handshake(host)};
                    case WS_ZCHAT_CONSUMER_SSL:
                    case WS_QTT_CONSUMER_SSL:
                        SSLZContext<WsContext> sslContext = session.getContext();
                        wsContext = sslContext.getActingContext();
                        return new IControl[]{wsContext.handshake(host)};
                    case QTT_SYMMETRY:
                        try {
                            X111_QttConnect x111 = new X111_QttConnect();
                            x111.setClientId(client.getClientToken());
                            x111.setUserName(client.getUsername());
                            x111.setPassword(client.getPassword());
                            x111.setClean();
                            return new IControl[]{x111};
                        }
                        catch (Exception e) {
                            _Logger.warning("create init commands fetal", e);
                            return null;
                        }
                    default:
                        throw new UnsupportedOperationException();
                }
            }
        };
        connect(connector);
    }

    private ICancelable mHeartbeatTask;

    @Override
    public void onCreate(ISession session)
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
        getCore().send(session, OperatorType.BIZ_LOCAL, _Ping);
    }

    public final void sendLocal(long sessionIndex, IControl... toSends)
    {
        ISession session = findSessionByIndex(sessionIndex);
        if (Objects.nonNull(session)) {
            _ClientCore.send(session, OperatorType.BIZ_LOCAL, toSends);
        }
        else {
            throw new ZException("client-id:%d,is offline;send % failed", sessionIndex, Arrays.toString(toSends));
        }
    }

    public void close(long sessionIndex)
    {
        ISession session = findSessionByIndex(sessionIndex);
        if (Objects.nonNull(session)) {
            _ClientCore.close(session, OperatorType.BIZ_LOCAL);
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
                                                          .multipliedBy(3),
                                                 c ->
                                                 {
                                                     try {
                                                         _Logger.debug("%s retry connect",
                                                                       Thread.currentThread()
                                                                             .getName());
                                                         connect(c);
                                                     }
                                                     catch (IOException e) {
                                                         e.printStackTrace();
                                                     }
                                                 }));
    }

    @Override
    protected ClientCore getCore()
    {
        return _ClientCore;
    }
}
