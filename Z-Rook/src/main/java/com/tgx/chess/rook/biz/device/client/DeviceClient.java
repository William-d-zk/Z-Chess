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

package com.tgx.chess.rook.biz.device.client;

import static com.tgx.chess.queen.event.inf.IOperator.Type.WRITE;
import static com.tgx.chess.rook.io.zoperator.ZSort.CONSUMER;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.io.zcrypt.EncryptHandler;
import com.tgx.chess.bishop.io.zprotocol.ZContext;
import com.tgx.chess.bishop.io.zprotocol.control.X101_HandShake;
import com.tgx.chess.bishop.io.zprotocol.control.X103_Close;
import com.tgx.chess.bishop.io.zprotocol.control.X104_Ping;
import com.tgx.chess.bishop.io.zprotocol.control.X105_Pong;
import com.tgx.chess.bishop.io.zprotocol.device.X21_SignUpResult;
import com.tgx.chess.bishop.io.zprotocol.device.X22_SignIn;
import com.tgx.chess.bishop.io.zprotocol.device.X23_SignInResult;
import com.tgx.chess.bishop.io.zprotocol.device.X30_EventMsg;
import com.tgx.chess.bishop.io.zprotocol.device.X31_ConfirmMsg;
import com.tgx.chess.bishop.io.zprotocol.ztls.X03_Cipher;
import com.tgx.chess.bishop.io.zprotocol.ztls.X05_EncryptStart;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.config.Config;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.async.AioCreator;
import com.tgx.chess.queen.io.core.async.AioSession;
import com.tgx.chess.queen.io.core.async.BaseAioConnector;
import com.tgx.chess.queen.io.core.executor.ClientCore;
import com.tgx.chess.queen.io.core.inf.IAioClient;
import com.tgx.chess.queen.io.core.inf.IAioConnector;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ICommandCreator;
import com.tgx.chess.queen.io.core.inf.IConnectionContext;
import com.tgx.chess.queen.io.core.inf.IPipeDecoder;
import com.tgx.chess.queen.io.core.inf.IPipeEncoder;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionCreated;
import com.tgx.chess.queen.io.core.inf.ISessionCreator;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.inf.ISessionOption;
import com.tgx.chess.rook.io.zoperator.ZSort;

/**
 * @author william.d.zk
 */
@Component
@PropertySource("classpath:client.properties")
public class DeviceClient
        implements
        IAioClient,
        ISessionDismiss<ZContext>,
        ISessionCreated<ZContext>
{
    private final Logger _Log = Logger.getLogger(getClass().getName());

    private final String                    _TargetName;
    private final String                    _TargetHost;
    private final int                       _TargetPort;
    private final Config                    _Config;
    private final ISessionCreator<ZContext> _SessionCreator;
    private final ICommandCreator<ZContext> _CommandCreator;
    private final IAioConnector<ZContext>   _DeviceConnector;
    private final AsynchronousChannelGroup  _ChannelGroup;
    private final ClientCore<ZContext>      _ClientCore      = new ClientCore<>();
    private final TimeWheel                 _TimeWheel       = _ClientCore.getTimeWheel();
    private final AtomicInteger             _State           = new AtomicInteger();
    private final IPipeEncoder<ZContext>    _ConsumerEncoder = CONSUMER.getConsumerEncoder();
    private final IPipeDecoder<ZContext>    _ConsumerDecoder = CONSUMER.getConsumerDecoder();
    private ISession<ZContext>              clientSession;
    private final AtomicReference<byte[]>   currentTokenRef  = new AtomicReference<>();

    enum STATE
    {
        STOP,
        CONNECTING,
        OFFLINE,
        ONLINE,
        SHUTDOWN
    }

    private void updateState(STATE state)
    {
        for (int s = _State.get(), retry = 0; !_State.compareAndSet(s, state.ordinal()); s = _State.get(), retry++) {
            _Log.warning("update state failed! retry: %d", retry);
        }
    }

    @SuppressWarnings("unchecked")
    public DeviceClient(@Value("${client.target.name}") String targetName,
                        @Value("${client.target.host}") String targetHost,
                        @Value("${client.target.port}") int targetPort) throws IOException
    {
        _State.set(STATE.STOP.ordinal());
        _TargetName = targetName;
        _TargetHost = targetHost;
        _TargetPort = targetPort;
        _Config = new Config();
        _ChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(1, _ClientCore.getWorkerThreadFactory());
        _SessionCreator = new AioCreator<ZContext>(_Config)
        {
            @Override
            public ISession<ZContext> createSession(AsynchronousSocketChannel socketChannel,
                                                    IConnectionContext<ZContext> context)
            {
                try {
                    return new AioSession<>(socketChannel, context.getConnectActive(), this, this, DeviceClient.this);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public ZContext createContext(ISessionOption option, ISort sorter)
            {
                return new ZContext(option, sorter);
            }
        };
        _CommandCreator = (session) -> new ICommand[] { new X101_HandShake(_TargetHost,
                                                                           session.getContext()
                                                                                  .getSeKey(),
                                                                           13) };
        _DeviceConnector = new BaseAioConnector<ZContext>(_TargetHost, _TargetPort, _ConsumerEncoder, _ConsumerDecoder)
        {

            @Override
            public ISort getSort()
            {
                return CONSUMER;
            }

            @Override
            public ISessionCreator getSessionCreator()
            {
                return _SessionCreator;
            }

            @Override
            public ISessionCreated getSessionCreated()
            {
                return DeviceClient.this;
            }

            @Override
            public ICommandCreator getCommandCreator()
            {
                return _CommandCreator;
            }

        };

    }

    @PostConstruct
    private void init()
    {
        _ClientCore.build((QEvent event, long sequence, boolean endOfBatch) ->
        {
            ICommand[] commands = null;
            ISession session = null;
            switch (event.getEventType())
            {
                case LOGIC:
                    //与 Server Node 处理过程存在较大的差异，中间去掉一个decoded dispatcher 所以此处入参为 ICommand[]
                    IPair logicContent = event.getContent();
                    commands = logicContent.first();
                    session = logicContent.second();
                    if (Objects.nonNull(commands)) {
                        commands = Stream.of(commands)
                                         .map(cmd ->
                                         {
                                             _Log.info("recv:%x ", cmd.getSerial());
                                             switch (cmd.getSerial())
                                             {
                                                 case X03_Cipher.COMMAND:
                                                 case X05_EncryptStart.COMMAND:
                                                     return cmd;
                                                 case X21_SignUpResult.COMMAND:
                                                     X21_SignUpResult x21 = (X21_SignUpResult) cmd;
                                                     X22_SignIn x22 = new X22_SignIn();
                                                     currentTokenRef.set(x21.getToken());
                                                     x22.setToken(currentTokenRef.get());
                                                     x22.setPassword("password");
                                                     return x22;
                                                 case X23_SignInResult.COMMAND:
                                                     X23_SignInResult x23 = (X23_SignInResult) cmd;
                                                     if (x23.isSuccess()) {
                                                         _Log.info("sign in success token invalid @ %s",
                                                                   Instant.ofEpochMilli(x23.getInvalidTime())
                                                                          .atZone(ZoneId.of("GMT+8")));
                                                     }
                                                     else {
                                                         return new X103_Close<ZContext>("sign in failed! close".getBytes());
                                                     }
                                                     break;
                                                 case X30_EventMsg.COMMAND:
                                                     X30_EventMsg x30 = (X30_EventMsg) cmd;
                                                     _Log.info("x30 payload: %s",
                                                               new String(x30.getPayload(), StandardCharsets.UTF_8));
                                                     X31_ConfirmMsg x31 = new X31_ConfirmMsg(x30.getUID());
                                                     x31.setStatus(X31_ConfirmMsg.STATUS_RECEIVED);
                                                     x31.setToken(x30.getToken());
                                                     return x31;
                                                 case X101_HandShake.COMMAND:
                                                     _Log.info("handshake ok");
                                                     break;
                                                 case X105_Pong.COMMAND:
                                                     _Log.info("heartbeat ok");
                                                     break;
                                                 case X103_Close.COMMAND:
                                                     close();
                                                     break;
                                             }
                                             return null;
                                         })
                                         .filter(Objects::nonNull)
                                         .toArray(ICommand[]::new);
                    }
                    break;
                default:
                    _Log.warning("event type no handle %s", event.getEventType());
                    break;
            }
            if (Objects.nonNull(commands) && commands.length > 0 && Objects.nonNull(session)) {
                event.produce(WRITE, new Pair<>(commands, session), ZSort.CONSUMER.getTransfer());
            }
            else {
                event.ignore();
            }
        }, new EncryptHandler());
    }

    public void connect()
    {
        try {
            connect(_DeviceConnector, _ChannelGroup);
            _TimeWheel.acquire(3, TimeUnit.SECONDS, _DeviceConnector, new ScheduleHandler<>(false, connector ->
            {
                if (Objects.nonNull(clientSession)) {
                    _Log.info("connect status checked -> success");
                }
                else {
                    _Log.warning("connect status checked -> failed -> retry once");
                    try {
                        connect(connector, _ChannelGroup);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        //terminate connect
                    }
                }
            }));
        }
        catch (IOException e) {
            e.printStackTrace();
            //terminate connect
        }
    }

    @Override
    public void onCreate(ISession<ZContext> session)
    {
        _Log.info("client connect:%s", session);
        clientSession = session;
        updateState(STATE.OFFLINE);
    }

    @Override
    public void onDismiss(ISession<ZContext> session)
    {
        _Log.info("dismiss:%s", session);
        if (clientSession == session) {
            _Log.info("drop client session %s", session);
            clientSession = null;
            updateState(STATE.SHUTDOWN);
        }
    }

    public boolean isOnline()
    {
        return Objects.nonNull(clientSession) && _State.get() >= STATE.ONLINE.ordinal();
    }

    public boolean isOffline()
    {
        return Objects.isNull(clientSession) && _State.get() <= STATE.OFFLINE.ordinal();
    }

    public boolean sendLocal(ICommand... toSends)
    {
        if (isOffline()) { throw new IllegalStateException("client is offline"); }
        return _ClientCore.localSend(clientSession, CONSUMER.getTransfer(), toSends);
    }

    public void close()
    {
        _ClientCore.localClose(clientSession, CONSUMER.getCloseOperator());
    }

    public void handshake()
    {
        sendLocal(new X101_HandShake<ZContext>(_TargetHost,
                                               clientSession.getContext()
                                                            .getSeKey(),
                                               13));
    }

    public void heartbeat(String msg)
    {
        Objects.requireNonNull(msg);
        sendLocal(new X104_Ping(msg.getBytes()));
    }

    public byte[] getToken()
    {
        return currentTokenRef.get();
    }

    public void setToken(String token)
    {
        currentTokenRef.set(IoUtil.hex2bin(token));
    }
}
