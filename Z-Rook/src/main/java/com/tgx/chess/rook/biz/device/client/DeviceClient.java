/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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
import static com.tgx.chess.queen.event.operator.MODE.CONSUMER;
import static com.tgx.chess.queen.event.operator.OperatorHolder.CONNECTED_OPERATOR;
import static com.tgx.chess.queen.event.operator.OperatorHolder.CONSUMER_TRANSFER;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.king.config.Config;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.operator.MODE;
import com.tgx.chess.queen.io.core.async.AioCreator;
import com.tgx.chess.queen.io.core.async.AioSession;
import com.tgx.chess.queen.io.core.executor.ClientCore;
import com.tgx.chess.queen.io.core.inf.IAioClient;
import com.tgx.chess.queen.io.core.inf.IAioConnector;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ICommandCreator;
import com.tgx.chess.queen.io.core.inf.IConnectActive;
import com.tgx.chess.queen.io.core.inf.IConnectionContext;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionCreated;
import com.tgx.chess.queen.io.core.inf.ISessionCreator;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.inf.ISessionOption;
import com.tgx.chess.queen.io.external.websokcet.ZContext;
import com.tgx.chess.queen.io.external.websokcet.bean.control.X101_HandShake;
import com.tgx.chess.queen.io.external.websokcet.bean.control.X103_Close;
import com.tgx.chess.queen.io.external.websokcet.bean.control.X104_Ping;
import com.tgx.chess.queen.io.external.websokcet.bean.control.X105_Pong;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X21_SignUpResult;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X22_SignIn;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X23_SignInResult;
import com.tgx.chess.queen.io.external.websokcet.bean.ztls.X03_Cipher;
import com.tgx.chess.queen.io.external.websokcet.bean.ztls.X05_EncryptStart;
import com.tgx.chess.rook.biz.device.dto.DeviceEntry;

@Component
@PropertySource("classpath:client.properties")
public class DeviceClient
        implements
        IAioClient,
        ISessionDismiss,
        ISessionCreated
{
    private final Logger                   _Log            = Logger.getLogger(getClass().getName());

    private final String                   _TargetName;
    private final String                   _TargetHost;
    private final int                      _TargetPort;
    private final Config                   _Config;
    private final ISessionCreator          _SessionCreator;
    private final ICommandCreator          _CommandCreator;
    private final IAioConnector            _DeviceConnector;
    private final AsynchronousChannelGroup _ChannelGroup;
    private final ClientCore<DeviceEntry>  _ClientCore     = new ClientCore<>();
    private final TimeWheel                _TimeWheel      = _ClientCore.getTimeWheel();

    private ISession                       clientSession;
    private final AtomicReference<byte[]>  currentTokenRef = new AtomicReference<>();

    public DeviceClient(@Value("${client.target.name}") String targetName,
                        @Value("${client.target.host}") String targetHost,
                        @Value("${client.target.port}") int targetPort)
            throws IOException {
        _TargetName = targetName;
        _TargetHost = targetHost;
        _TargetPort = targetPort;
        _Config = new Config();
        _ChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(1, _ClientCore.getWorkerThreadFactory());
        _SessionCreator = new AioCreator(_Config)
        {
            @Override
            public ISession createSession(AsynchronousSocketChannel socketChannel, IConnectActive active) {
                try {
                    return new AioSession(socketChannel,
                                          active,
                                          this,
                                          this,
                                          DeviceClient.this,
                                          active.getMode()
                                                .getInOperator());
                }
                catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public IContext createContext(ISessionOption option, MODE mode) {
                return new ZContext(option, mode);
            }
        };
        _CommandCreator = (session) -> new ICommand[] { new X101_HandShake(_TargetHost, ((ZContext) session.getContext()).getSeKey(), 13) };
        _DeviceConnector = new IAioConnector()
        {
            private final InetSocketAddress remote = new InetSocketAddress(_TargetHost, _TargetPort);
            private InetSocketAddress       localBind;

            @Override
            public InetSocketAddress getRemoteAddress() {
                return remote;
            }

            @Override
            public InetSocketAddress getLocalAddress() {
                return localBind;
            }

            @Override
            public void setLocalAddress(InetSocketAddress address) {
                localBind = address;
            }

            @Override
            public IOperator<Throwable, IAioConnector> getErrorOperator() {
                return new IOperator<Throwable, IAioConnector>()
                {
                    @Override
                    @SuppressWarnings("unchecked")
                    public Triple<Throwable, IAioConnector, IOperator<Throwable, IAioConnector>> handle(Throwable throwable,
                                                                                                        IAioConnector connector) {
                        _Log.warning("connect active failed %s", throwable, connector.toString());
                        return new Triple<>(throwable, connector, this);
                    }
                };
            }

            @Override
            public MODE getMode() {
                return CONSUMER;
            }

            @Override
            public IOperator<IConnectionContext, AsynchronousSocketChannel> getConnectedOperator() {
                return CONNECTED_OPERATOR();
            }

            @Override
            public ISessionCreator getSessionCreator() {
                return _SessionCreator;
            }

            @Override
            public ISessionCreated getSessionCreated() {
                return DeviceClient.this;
            }

            @Override
            public ICommandCreator getCommandCreator() {
                return _CommandCreator;
            }

        };

    }

    @PostConstruct
    private void init() {
        _ClientCore.build((event, sequence, endOfBatch) -> {
            ICommand[] commands = null;
            ISession session = null;
            switch (event.getEventType()) {
                case LOGIC:
                    //与 Server Node 处理过程存在较大的差异，中间去掉一个decoded dispatcher 所以此处入参为 ICommand[]
                    Pair<ICommand[], ISession> logicContent = event.getContent();
                    commands = logicContent.first();
                    session = logicContent.second();
                    if (Objects.nonNull(commands)) {
                        commands = Stream.of(commands)
                                         .map(cmd -> {
                                             switch (cmd.getSerial()) {
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
                                                         return new X103_Close("sign in failed! close".getBytes());
                                                     }
                                                     break;
                                                 case X101_HandShake.COMMAND:
                                                     _Log.info("handshake ok");
                                                     break;
                                                 case X105_Pong.COMMAND:
                                                     _Log.info("heartbeat ok");
                                                     break;
                                                 case X103_Close.COMMAND:
                                                     return new X103_Close("client response close".getBytes());
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
                event.produce(WRITE, commands, session, CONSUMER_TRANSFER());
            }
            else event.ignore();
        });
    }

    public void connect() {
        try {
            connect(_DeviceConnector, _ChannelGroup);
            _TimeWheel.acquire(3, TimeUnit.SECONDS, _DeviceConnector, new ScheduleHandler<>(false, connector -> {
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
    public void onCreate(ISession session) {
        _Log.info("client connect:%s", session);
        clientSession = session;
    }

    @Override
    public void onDismiss(ISession session) {
        _Log.info("dismiss:%s", session);
        if (clientSession == session) {
            _Log.info("drop client session %s", session);
            clientSession = null;
            connect();
        }
    }

    public boolean sendLocal(ICommand... toSends) {
        return _ClientCore.localSend(clientSession, toSends);
    }

    public void close() {
        _ClientCore.close(clientSession);
    }

    public void handshake() {
        sendLocal(new X101_HandShake(_TargetHost, ((ZContext) clientSession.getContext()).getSeKey(), 13));
    }

    public void heartbeat(String msg) {
        Objects.requireNonNull(msg);
        sendLocal(new X104_Ping(msg.getBytes()));
    }

    public byte[] getToken() {
        return currentTokenRef.get();
    }
}
