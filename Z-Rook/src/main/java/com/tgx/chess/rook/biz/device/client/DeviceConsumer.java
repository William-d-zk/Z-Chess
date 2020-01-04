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

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.tgx.chess.bishop.io.mqtt.control.X112_QttConnack;
import com.tgx.chess.bishop.io.ws.bean.WsContext;
import com.tgx.chess.bishop.io.ws.control.X101_HandShake;
import com.tgx.chess.bishop.io.ws.control.X102_SslHandShake;
import com.tgx.chess.bishop.io.ws.control.X103_Close;
import com.tgx.chess.bishop.io.ws.control.X104_Ping;
import com.tgx.chess.bishop.io.ws.control.X105_Pong;
import com.tgx.chess.bishop.io.zcrypt.EncryptHandler;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.device.X21_SignUpResult;
import com.tgx.chess.bishop.io.zprotocol.device.X22_SignIn;
import com.tgx.chess.bishop.io.zprotocol.device.X23_SignInResult;
import com.tgx.chess.bishop.io.zprotocol.device.X30_EventMsg;
import com.tgx.chess.bishop.io.zprotocol.device.X31_ConfirmMsg;
import com.tgx.chess.bishop.io.zprotocol.ztls.X03_Cipher;
import com.tgx.chess.bishop.io.zprotocol.ztls.X05_EncryptStart;
import com.tgx.chess.king.base.exception.ZException;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.base.util.CryptUtil;
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
import com.tgx.chess.queen.io.core.inf.ICommandCreator;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionCreated;
import com.tgx.chess.queen.io.core.inf.ISessionCreator;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.inf.ISessionOption;
import com.tgx.chess.rook.io.ConsumerZSort;

/**
 * @author william.d.zk
 * @date 2019-05-12
 */
@Component
public class DeviceConsumer
        implements
        IAioClient<ZContext>,
        ISessionDismiss<ZContext>,
        ISessionCreated<ZContext>
{
    final Logger _Logger = Logger.getLogger(getClass().getName());

    private final Config                        _Config;
    private final AsynchronousChannelGroup      _ChannelGroup;
    private final ClientCore<ZContext>          _ClientCore     = new ClientCore<>();
    private final TimeWheel                     _TimeWheel      = _ClientCore.getTimeWheel();
    private final AtomicInteger                 _State          = new AtomicInteger();
    private final AtomicReference<byte[]>       currentTokenRef = new AtomicReference<>();
    private final Map<Long,
                      ISession<ZContext>>       _ClientSessions = new HashMap<>(2);
    private final CryptUtil                     _CryptUtil      = new CryptUtil();

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
            _Logger.warning("update state failed! retry: %d", retry);
        }
    }

    @SuppressWarnings("unchecked")
    public DeviceConsumer() throws IOException
    {
        _State.set(STATE.STOP.ordinal());
        _Config = new Config();
        _ChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(1, _ClientCore.getWorkerThreadFactory());
        _ClientCore.build((QEvent event, long sequence, boolean endOfBatch) ->
        {
            IControl<ZContext>[] commands;
            final ISession<ZContext> session;
            switch (event.getEventType())
            {
                case LOGIC:
                    //与 Server Node 处理过程存在较大的差异，中间去掉一个decoded dispatcher 所以此处入参为 IControl[]
                    IPair logicContent = event.getContent();
                    commands = logicContent.first();
                    session = logicContent.second();
                    if (Objects.nonNull(commands)) {
                        commands = Stream.of(commands)
                                         .map(cmd ->
                                         {
                                             _Logger.info("recv:{ %s }", cmd);
                                             switch (cmd.serial())
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
                                                         _Logger.info("sign in success token invalid @ %s",
                                                                      Instant.ofEpochMilli(x23.getInvalidTime())
                                                                             .atZone(ZoneId.of("GMT+8")));
                                                     }
                                                     else {
                                                         return new X103_Close("sign in failed! ws_close".getBytes());
                                                     }
                                                     break;
                                                 case X30_EventMsg.COMMAND:
                                                     X30_EventMsg x30 = (X30_EventMsg) cmd;
                                                     _Logger.info("x30 payload: %s",
                                                                  new String(x30.getPayload(), StandardCharsets.UTF_8));
                                                     X31_ConfirmMsg x31 = new X31_ConfirmMsg(x30.getMsgId());
                                                     x31.setStatus(X31_ConfirmMsg.STATUS_RECEIVED);
                                                     x31.setToken(x30.getToken());
                                                     return x31;
                                                 case X101_HandShake.COMMAND:
                                                     _Logger.info("ws_handshake ok");
                                                     break;
                                                 case X105_Pong.COMMAND:
                                                     _Logger.info("ws_heartbeat ok");
                                                     break;
                                                 case X103_Close.COMMAND:
                                                     close(session.getIndex());
                                                     break;
                                                 case X112_QttConnack.COMMAND:
                                                 default:
                                                     break;
                                             }
                                             return null;
                                         })
                                         .filter(Objects::nonNull)
                                         .toArray(IControl[]::new);
                    }
                    break;
                default:
                    _Logger.warning("event type no mappingHandle %s", event.getEventType());
                    commands = null;
                    session = null;
                    break;
            }
            if (Objects.nonNull(commands) && commands.length > 0 && Objects.nonNull(session)) {
                event.produce(WRITE,
                              new Pair<>(commands, session),
                              session.getContext()
                                     .getSort()
                                     .getTransfer());
            }
            else {
                event.ignore();
            }
        }, new EncryptHandler());
    }

    @PostConstruct
    private void init()
    {

    }

    @SuppressWarnings("unchecked")
    public void connect(final String _TargetHost,
                        final int _TargetPort,
                        final ISort<ZContext> _Sort,
                        final long _ClientId)
    {
        final ICommandCreator<ZContext> _CommandCreator;
        switch ((ConsumerZSort) _Sort)
        {
            case WS_CONSUMER:
                _CommandCreator = session ->
                {
                    WsContext wsContext = (WsContext) session.getContext();
                    X101_HandShake x101 = new X101_HandShake(_TargetHost,
                                                             wsContext.getSeKey(),
                                                             wsContext.getWsVersion());
                    return new IControl[] { x101 };
                };
                break;
            case WS_CONSUMER_SSL:
                _CommandCreator = session ->
                {
                    WsContext wsContext = (WsContext) session.getContext();
                    X102_SslHandShake x102 = new X102_SslHandShake(_TargetHost,
                                                                   wsContext.getSeKey(),
                                                                   wsContext.getWsVersion());
                    return new IControl[] { x102 };
                };
                break;
            case QTT_SYMMETRY:
                _CommandCreator = session ->
                {
                    X111_QttConnect x111 = new X111_QttConnect();
                    x111.setClientId(_CryptUtil.sha256("test-mqtt-smallbeex-0001"));
                    x111.setUserName("A06FF74D68D32FD5FE9DEB00F636BEC9C24FC400F23E6B91F2CA3AA9A3E52B7F");
                    x111.setPassword("SNju/kfjXtgAAe-`cN".getBytes(StandardCharsets.UTF_8));
                    x111.setClean();
                    return new IControl[] { x111 };
                };
                break;
            default:
                _CommandCreator = null;
                break;
        }
        final ISessionCreator<ZContext> _SessionCreator = new AioCreator<ZContext>(_Config)
        {
            @Override
            public ZContext createContext(ISessionOption option, ISort<ZContext> sort)
            {
                return sort.newContext(this, _CommandCreator);
            }

            @Override
            public ISession<ZContext> createSession(AsynchronousSocketChannel socketChannel,
                                                    IConnectActivity<ZContext> activity) throws IOException
            {
                ISession<ZContext> session = new AioSession<>(socketChannel, this, this, activity, DeviceConsumer.this);
                session.setIndex(_ClientId);
                return session;
            }
        };
        final BaseAioConnector<ZContext> _Connector = new BaseAioConnector<ZContext>(_TargetHost, _TargetPort)
        {

            @Override
            public ISort<ZContext> getSort()
            {
                return _Sort;
            }

            @Override
            public ISessionCreator<ZContext> getSessionCreator()
            {
                return _SessionCreator;
            }

            @Override
            public ISessionCreated<ZContext> getSessionCreated()
            {
                return DeviceConsumer.this;
            }

            @Override
            public ICommandCreator<ZContext> getCommandCreator()
            {
                return _CommandCreator;
            }
        };
        try {
            connect(_Connector, _ChannelGroup);
            _TimeWheel.acquire(_Connector, new ScheduleHandler<>(3, connector ->
            {
                if (Objects.nonNull(_ClientSessions.get(_ClientId))) {
                    _Logger.info("connect -> success; %s", _ClientSessions.get(_ClientId));
                }
                else {
                    _Logger.warning("connect status checked -> failed -> retry once");
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
        _Logger.info("client connect:%s", session);
        long sessionIndex = session.getIndex();
        if (_ClientSessions.containsKey(sessionIndex)) {
            _Logger.warning("clientId %d already connected", sessionIndex);
        }
        else {
            _ClientSessions.put(session.getIndex(), session);
            updateState(STATE.OFFLINE);
        }
    }

    @Override
    public void onDismiss(ISession<ZContext> session)
    {
        _Logger.info("dismiss:%s", session);
        if (_ClientSessions.remove(session.getIndex()) == session) {
            _Logger.info("drop client session %s", session);
        }
        if (_ClientSessions.isEmpty()) {
            updateState(STATE.SHUTDOWN);
        }
    }

    @SafeVarargs
    public final void sendLocal(long clientId, IControl<ZContext>... toSends)
    {
        ISession<ZContext> session = _ClientSessions.get(clientId);
        if (Objects.nonNull(session)) {
            _ClientCore.localSend(session,
                                  session.getContext()
                                         .getSort()
                                         .getTransfer(),
                                  toSends);
        }
        else {
            throw new ZException("client-id:%d,is offline;send % failed", clientId, Arrays.toString(toSends));
        }
    }

    public void close(long clientId)
    {
        ISession<ZContext> session = _ClientSessions.get(clientId);
        if (Objects.nonNull(session)) {
            _ClientCore.localClose(session,
                                   session.getContext()
                                          .getSort()
                                          .getCloser());
        }
        else {
            throw new ZException("client session is not exist");
        }
    }

    public byte[] getToken()
    {
        return currentTokenRef.get();
    }

    public void setToken(String token)
    {
        currentTokenRef.set(IoUtil.hex2bin(token));
    }

    public void wsHeartbeat(long clientId, String msg)
    {
        Objects.requireNonNull(msg);
        sendLocal(clientId, new X104_Ping(msg.getBytes()));
    }
}
