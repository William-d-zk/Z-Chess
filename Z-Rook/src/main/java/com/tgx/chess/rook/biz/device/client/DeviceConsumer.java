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
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.biz.db.dao.ZUID;
import com.tgx.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.tgx.chess.bishop.io.mqtt.control.X112_QttConnack;
import com.tgx.chess.bishop.io.ws.bean.WsContext;
import com.tgx.chess.bishop.io.ws.control.X101_HandShake;
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
import com.tgx.chess.queen.config.IBizIoConfig;
import com.tgx.chess.queen.config.QueenCode;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.async.AioSession;
import com.tgx.chess.queen.io.core.async.AioSessionManager;
import com.tgx.chess.queen.io.core.async.BaseAioConnector;
import com.tgx.chess.queen.io.core.executor.ClientCore;
import com.tgx.chess.queen.io.core.inf.IAioClient;
import com.tgx.chess.queen.io.core.inf.IAioConnector;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.inf.ISessionOption;
import com.tgx.chess.rook.config.ConsumerConfig;
import com.tgx.chess.rook.io.ConsumerZSort;

/**
 * @author william.d.zk
 * @date 2019-05-12
 */
@Component
public class DeviceConsumer
        extends
        AioSessionManager<ZContext>
        implements
        IAioClient<ZContext>,
        ISessionDismiss<ZContext>
{
    private final Logger                   _Logger    = Logger.getLogger(getClass().getSimpleName());
    private final ConsumerConfig           _ConsumerConfig;
    private final AsynchronousChannelGroup _ChannelGroup;
    private final ClientCore<ZContext>     _ClientCore;
    private final TimeWheel                _TimeWheel;
    private final CryptUtil                _CryptUtil = new CryptUtil();
    private final ZUID                     _ZUid      = new ZUID();
    private final Map<Long,
                      ZClient>             _ZClientMap;

    @SuppressWarnings("unchecked")
    @Autowired
    public DeviceConsumer(IBizIoConfig bizIoConfig,
                          ConsumerConfig consumerConfig) throws IOException
    {
        super(bizIoConfig);
        _ZClientMap = new HashMap<>(1 << getConfigPower(getSlot(AioSessionManager.SERVER_SLOT)));
        _ConsumerConfig = consumerConfig;
        int ioCount = _ConsumerConfig.getIoCount();
        _ClientCore = new ClientCore<>(ioCount);
        _TimeWheel = _ClientCore.getTimeWheel();
        _ChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(ioCount, _ClientCore.getWorkerThreadFactory());
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
                                                     byte[] token = x22.getToken();
                                                     ZClient zClient = _ZClientMap.get(session.getIndex());
                                                     if (zClient == null) {
                                                         _Logger.warning("z-client not found");
                                                     }
                                                     else {
                                                         zClient.setToken(IoUtil.bin2Hex(token));
                                                     }
                                                     x22.setToken(token);
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
                                                     _Logger.info("qtt connack %s", cmd);
                                                     break;
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
        _Logger.info("device consumer created");
    }

    @PostConstruct
    private void init()
    {

    }

    @Override
    public void connect(IAioConnector<ZContext> connector) throws IOException
    {
        if (_ChannelGroup.isShutdown()) return;
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(_ChannelGroup);
        socketChannel.connect(connector.getRemoteAddress(), socketChannel, connector);
    }

    @SuppressWarnings("unchecked")
    public void connect(ConsumerZSort sort, ZClient zClient) throws IOException
    {
        final String host;
        final int port;
        switch (sort)
        {
            case WS_CONSUMER:
                host = _ConsumerConfig.getWs()
                                      .getHost();
                port = _ConsumerConfig.getWs()
                                      .getPort();
                break;
            case QTT_SYMMETRY:
                host = _ConsumerConfig.getQtt()
                                      .getHost();
                port = _ConsumerConfig.getQtt()
                                      .getPort();
                break;
            default:
                throw new UnsupportedOperationException();

        }
        BaseAioConnector<ZContext> connector = new BaseAioConnector<ZContext>(host,
                                                                              port,
                                                                              getSocketConfig(getSlot(QueenCode.CU_XID)),
                                                                              _ClientCore)
        {
            @Override
            public ISort<ZContext> getSort()
            {
                return sort;
            }

            @Override
            public void onCreate(ISession<ZContext> session)
            {
                long sessionIndex = _ZUid.getId();
                session.setIndex(sessionIndex);
                DeviceConsumer.this.addSession(session);
                zClient.setSessionIndex(sessionIndex);
                _ZClientMap.put(session.getIndex(), zClient);
                _Logger.info("connected :%d", sessionIndex);
            }

            @Override
            public ISession<ZContext> createSession(AsynchronousSocketChannel socketChannel,
                                                    IConnectActivity<ZContext> activity) throws IOException
            {
                return new AioSession<>(socketChannel, this, this, activity, DeviceConsumer.this);
            }

            @Override
            public ZContext createContext(ISessionOption option, ISort<ZContext> sort)
            {
                return sort.newContext(option, this);
            }

            @Override
            public IControl<ZContext>[] createCommands(ISession<ZContext> session)
            {
                switch (sort)
                {
                    case WS_CONSUMER:
                        WsContext wsContext = (WsContext) session.getContext();
                        X101_HandShake x101 = new X101_HandShake(host, wsContext.getSeKey(), wsContext.getWsVersion());
                        return new IControl[] { x101 };
                    case QTT_SYMMETRY:
                        try {
                            X111_QttConnect x111 = new X111_QttConnect();
                            x111.setClientId(zClient.getClientId());
                            x111.setUserName(zClient.getUsername());
                            x111.setPassword(zClient.getPassword()
                                                    .getBytes(StandardCharsets.UTF_8));
                            x111.setClean();
                            return new IControl[] { x111 };
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

    @Override
    public void onDismiss(ISession<ZContext> session)
    {
        rmSession(session);
        _Logger.warning("device consumer dismiss session %s", session);
    }

    @SafeVarargs
    public final void sendLocal(long sessionIndex, IControl<ZContext>... toSends)
    {
        ISession<ZContext> session = findSessionByIndex(sessionIndex);
        if (Objects.nonNull(session)) {
            _ClientCore.localSend(session,
                                  session.getContext()
                                         .getSort()
                                         .getTransfer(),
                                  toSends);
        }
        else {
            throw new ZException("client-id:%d,is offline;send % failed", sessionIndex, Arrays.toString(toSends));
        }
    }

    public void close(long sessionIndex)
    {
        ISession<ZContext> session = findSessionByIndex(sessionIndex);
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

    public void wsHeartbeat(long sessionIndex, String msg)
    {
        Objects.requireNonNull(msg);
        sendLocal(sessionIndex, new X104_Ping(msg.getBytes()));
    }

    @Override
    public void onFailed(IAioConnector<ZContext> connector)
    {
        _TimeWheel.acquire(connector, new ScheduleHandler<>(connector.getConnectTimeout() * 3, c ->
        {
            try {
                _Logger.info("%s retry connect",
                             Thread.currentThread()
                                   .getName());
                connect(c);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }
}
