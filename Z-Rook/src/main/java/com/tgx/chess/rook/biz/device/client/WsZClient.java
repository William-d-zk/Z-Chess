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
import static com.tgx.chess.rook.io.WsZSort.CONSUMER;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.io.ws.bean.WsContext;
import com.tgx.chess.bishop.io.ws.control.X101_HandShake;
import com.tgx.chess.bishop.io.ws.control.X103_Close;
import com.tgx.chess.bishop.io.ws.control.X104_Ping;
import com.tgx.chess.bishop.io.ws.control.X105_Pong;
import com.tgx.chess.bishop.io.zcrypt.EncryptHandler;
import com.tgx.chess.bishop.io.zprotocol.device.X21_SignUpResult;
import com.tgx.chess.bishop.io.zprotocol.device.X22_SignIn;
import com.tgx.chess.bishop.io.zprotocol.device.X23_SignInResult;
import com.tgx.chess.bishop.io.zprotocol.device.X30_EventMsg;
import com.tgx.chess.bishop.io.zprotocol.device.X31_ConfirmMsg;
import com.tgx.chess.bishop.io.zprotocol.ztls.X03_Cipher;
import com.tgx.chess.bishop.io.zprotocol.ztls.X05_EncryptStart;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionOption;
import com.tgx.chess.rook.io.WsZSort;

/**
 * @author william.d.zk
 */
@Component
@PropertySource("classpath:ws.client.properties")
public class WsZClient
        extends
        BaseDeviceClient<WsContext>
{

    public WsZClient(@Value("${ws.client.target.name}") String targetName,
                     @Value("${ws.client.target.host}") String targetHost,
                     @Value("${ws.client.target.port}") int targetPort) throws IOException
    {

        super(targetName, targetHost, targetPort, CONSUMER);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ICommand<WsContext>[] createCommands(ISession<WsContext> session)
    {
        return new ICommand[] { new X101_HandShake<>(_TargetHost,
                                                     session.getContext()
                                                            .getSeKey(),
                                                     13) };
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    private void init()
    {
        _ClientCore.build((QEvent event, long sequence, boolean endOfBatch) ->
        {
            ICommand<WsContext>[] commands = null;
            ISession<WsContext> session = null;
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
                                             _Logger.info("recv:%x ", cmd.getSerial());
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
                                                         _Logger.info("sign in success token invalid @ %s",
                                                                      Instant.ofEpochMilli(x23.getInvalidTime())
                                                                             .atZone(ZoneId.of("GMT+8")));
                                                     }
                                                     else {
                                                         return new X103_Close<>("sign in failed! ws_close".getBytes());
                                                     }
                                                     break;
                                                 case X30_EventMsg.COMMAND:
                                                     X30_EventMsg x30 = (X30_EventMsg) cmd;
                                                     _Logger.info("x30 payload: %s",
                                                                  new String(x30.getPayload(), StandardCharsets.UTF_8));
                                                     X31_ConfirmMsg x31 = new X31_ConfirmMsg<>(x30.getUID());
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
                    _Logger.warning("event type no handle %s", event.getEventType());
                    break;
            }
            if (Objects.nonNull(commands) && commands.length > 0 && Objects.nonNull(session)) {
                event.produce(WRITE, new Pair<>(commands, session), WsZSort.CONSUMER.getTransfer());
            }
            else {
                event.ignore();
            }
        }, new EncryptHandler());
    }

    public void handshake()
    {
        sendLocal(new X101_HandShake<>(_TargetHost,
                                       clientSession.getContext()
                                                    .getSeKey(),
                                       13));
    }

    public void heartbeat(String msg)
    {
        Objects.requireNonNull(msg);
        sendLocal(new X104_Ping(msg.getBytes()));
    }

    @Override
    public WsContext createContext(ISessionOption option, ISort sort)
    {
        return new WsContext(option, sort);
    }
}
