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

package com.tgx.z.rook.biz.device.client;

import static com.tgx.z.queen.event.operator.MODE.CONSUMER;
import static com.tgx.z.queen.event.operator.OperatorHolder.CONNECTED_OPERATOR;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.tgx.z.config.Config;
import com.tgx.z.queen.base.log.Logger;
import com.tgx.z.queen.base.schedule.ScheduleHandler;
import com.tgx.z.queen.base.schedule.TimeWheel;
import com.tgx.z.queen.base.util.Pair;
import com.tgx.z.queen.base.util.Triple;
import com.tgx.z.queen.event.inf.IOperator;
import com.tgx.z.queen.event.operator.MODE;
import com.tgx.z.queen.io.core.async.AioCreator;
import com.tgx.z.queen.io.core.async.AioSession;
import com.tgx.z.queen.io.core.executor.ClientCore;
import com.tgx.z.queen.io.core.inf.IAioClient;
import com.tgx.z.queen.io.core.inf.IAioConnector;
import com.tgx.z.queen.io.core.inf.ICommand;
import com.tgx.z.queen.io.core.inf.ICommandCreator;
import com.tgx.z.queen.io.core.inf.IConnectActive;
import com.tgx.z.queen.io.core.inf.IConnectionContext;
import com.tgx.z.queen.io.core.inf.IContext;
import com.tgx.z.queen.io.core.inf.ISession;
import com.tgx.z.queen.io.core.inf.ISessionCreated;
import com.tgx.z.queen.io.core.inf.ISessionCreator;
import com.tgx.z.queen.io.core.inf.ISessionDismiss;
import com.tgx.z.queen.io.core.inf.ISessionOption;
import com.tgx.z.queen.io.external.websokcet.ZContext;
import com.tgx.z.queen.io.external.websokcet.bean.control.X101_HandShake;
import com.tgx.z.queen.io.external.websokcet.bean.control.X104_Ping;
import com.tgx.z.queen.io.external.websokcet.bean.control.X105_Pong;
import com.tgx.z.rook.biz.device.dto.DeviceEntry;

@Component
@PropertySource("classpath:client.properties")
public class DeviceClient
        implements
        IAioClient,
        ISessionDismiss,
        ISessionCreated
{
    private final Logger                   _Log        = Logger.getLogger(getClass().getName());

    private final String                   _TargetName;
    private final String                   _TargetHost;
    private final int                      _TargetPort;
    private final Config                   _Config;
    private final ISessionCreator          _SessionCreator;
    private final ICommandCreator          _CommandCreator;
    private final IAioConnector            _DeviceConnector;
    private final AsynchronousChannelGroup _ChannelGroup;
    private final ClientCore<DeviceEntry>  _ClientCore = new ClientCore<>();
    private final TimeWheel                _TimeWheel  = _ClientCore.getTimeWheel();

    private ISession                       clientSession;

    public DeviceClient(@Value("${client.target.name}") String targetName,
                        @Value("${client.target.host}") String targetHost,
                        @Value("${client.target.port}") int targetPort)
            throws IOException {
        _TargetName = targetName;
        _TargetHost = targetHost;
        _TargetPort = targetPort;
        _Config = new Config("client");
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
            switch (event.getEventType()) {
                case LOGIC:
                    Pair<ICommand[], ISession> logicContent = event.getContent();
                    ICommand[] commands = logicContent.first();
                    ISession session = logicContent.second();
                    if (Objects.nonNull(commands)) for (ICommand cmd : commands)

                        switch (cmd.getSerial()) {
                            case X101_HandShake.COMMAND:
                                _Log.info("handshake ok");
                                break;
                            case X105_Pong.COMMAND:
                                _Log.info("heartbeat ok");
                                break;
                        }
                    break;
                default:
                    _Log.warning("event type no handle %s", event.getEventType());
                    break;
            }
            event.ignore();
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
}
