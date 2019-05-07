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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.bishop.biz.db.dao.DeviceEntry;
import com.tgx.chess.bishop.io.zcrypt.EncryptHandler;
import com.tgx.chess.bishop.io.zhandler.ZLinkedHandler;
import com.tgx.chess.bishop.io.zhandler.ZLogicHandler;
import com.tgx.chess.bishop.io.WsZSort;
import com.tgx.chess.bishop.io.zprotocol.ZContext;
import com.tgx.chess.bishop.io.ws.control.X101_HandShake;
import com.tgx.chess.bishop.io.ws.control.X103_Close;
import com.tgx.chess.bishop.io.ws.control.X104_Ping;
import com.tgx.chess.bishop.io.ws.control.X105_Pong;
import com.tgx.chess.bishop.io.zprotocol.device.X20_SignUp;
import com.tgx.chess.bishop.io.zprotocol.device.X21_SignUpResult;
import com.tgx.chess.bishop.io.zprotocol.device.X22_SignIn;
import com.tgx.chess.bishop.io.zprotocol.device.X23_SignInResult;
import com.tgx.chess.bishop.io.zprotocol.device.X30_EventMsg;
import com.tgx.chess.bishop.io.zprotocol.device.X31_ConfirmMsg;
import com.tgx.chess.bishop.io.zprotocol.device.X32_MsgStatus;
import com.tgx.chess.bishop.io.zprotocol.device.X50_DeviceMsg;
import com.tgx.chess.bishop.io.zprotocol.device.X51_DeviceMsgAck;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.king.config.Config;
import com.tgx.chess.queen.config.QueenCode;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.async.AioCreator;
import com.tgx.chess.queen.io.core.async.AioSession;
import com.tgx.chess.queen.io.core.async.BaseAioServer;
import com.tgx.chess.queen.io.core.executor.ServerCore;
import com.tgx.chess.queen.io.core.inf.IAioServer;
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
import com.tgx.chess.queen.io.core.manager.QueenManager;

/**
 * @author william.d.zk
 */
public class DeviceNode
        extends
        QueenManager<ZContext>
        implements
        ISessionDismiss<ZContext>,
        ISessionCreated<ZContext>
{
    private final Logger                    _Log     = Logger.getLogger(getClass().getName());
    private final String                    _ServerHost;
    private final int                       _ServerPort;
    private final IAioServer<ZContext>      _DeviceServer;
    private final ISessionCreator<ZContext> _SessionCreator;
    private final ICommandCreator<ZContext> _CommandCreator;
    private final IRepository<DeviceEntry>  _Repository;
    private final IPipeEncoder<ZContext>    _Encoder = WsZSort.SERVER.getEncoder();
    private final IPipeDecoder<ZContext>    _Decoder = WsZSort.SERVER.getDecoder();

    @Override
    public void onDismiss(ISession<ZContext> session) {
        rmSession(session);
    }

    @Override
    public void onCreate(ISession<ZContext> session) {
        /* 进入这里的都是 _DeviceServer 建立的链接*/
        session.setIndex(QueenCode.CM_XID);
        addSession(session);
    }

    public DeviceNode(String host, int port, IRepository<DeviceEntry> respository) {
        super(new Config("device"), new ServerCore<ZContext>()
        {

            @Override
            public RingBuffer<QEvent> getLocalPublisher(ISession<ZContext> session) {
                switch (getSlot(session)) {
                    case QueenCode.CM_XID_LOW:
                    case QueenCode.RM_XID_LOW:
                        return getClusterLocalSendEvent();
                    default:
                        return getBizLocalSendEvent();
                }
            }

            @Override
            public RingBuffer<QEvent> getLocalCloser(ISession<ZContext> session) {
                switch (getSlot(session)) {
                    case QueenCode.CM_XID_LOW:
                    case QueenCode.RM_XID_LOW:
                        return getClusterLocalCloseEvent();
                    default:
                        return getBizLocalCloseEvent();
                }
            }
        });
        _ServerHost = host;
        _ServerPort = port;
        _SessionCreator = new AioCreator<ZContext>(getConfig())
        {

            @Override
            public ISession<ZContext> createSession(AsynchronousSocketChannel socketChannel, IConnectionContext<ZContext> context) {
                try {
                    return new AioSession<>(socketChannel, context.getConnectActive(), this, this, DeviceNode.this);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public ZContext createContext(ISessionOption option, ISort sort) {
                return new ZContext(option, sort);

            }
        };
        _CommandCreator = (session) -> null;

        _DeviceServer = new BaseAioServer<ZContext>(_ServerHost, _ServerPort, _Encoder, _Decoder)
        {

            @Override
            public ISort getSort() {
                return WsZSort.SERVER;
            }

            @Override
            public ISessionCreator<ZContext> getSessionCreator() {
                return _SessionCreator;
            }

            @Override
            public ISessionCreated<ZContext> getSessionCreated() {
                return DeviceNode.this;
            }

            @Override
            public ICommandCreator<ZContext> getCommandCreator() {
                return _CommandCreator;
            }

        };
        _Repository = respository;
        _Log.info("Device Node Bean Load");
    }

    public void start() throws IOException {
        _ServerCore.build(new ZLogicHandler(_Encoder, (command, session, handler) -> {
            //前置的 dispatcher 将 ICommands 拆分了

            _Log.info("device node logic handle %s", command);
            switch (command.getSerial()) {
                case X30_EventMsg.COMMAND:
                    return new X31_ConfirmMsg(command.getUID());
                case X31_ConfirmMsg.COMMAND:
                    return new X32_MsgStatus(command.getUID());
                case X50_DeviceMsg.COMMAND:
                    return new X51_DeviceMsgAck(command.getUID());
                case X103_Close.COMMAND:
                    localClose(session, handler.getCloseOperator());
                    break;
                case X104_Ping.COMMAND:
                    return new X105_Pong("Server pong".getBytes());
                case X101_HandShake.COMMAND:
                    return command;
                case X51_DeviceMsgAck.COMMAND:
                case X105_Pong.COMMAND:
                default:
                    break;
            }
            return null;
        }), this, _Encoder, new ZLinkedHandler(), new EncryptHandler());
        _DeviceServer.bindAddress(new InetSocketAddress(_ServerHost, _ServerPort),
                                  AsynchronousChannelGroup.withFixedThreadPool(_ServerCore.getServerCount(),
                                                                               _ServerCore.getWorkerThreadFactory()));
        _DeviceServer.pendingAccept();
        _Log.info(String.format("device node start %s:%d", _ServerHost, _ServerPort));
    }

    @Override
    public ICommand save(ICommand tar, ISession<ZContext> session) {
        DeviceEntry deviceEntry = _Repository.save(tar);
        switch (tar.getSerial()) {
            case X20_SignUp.COMMAND:
                X21_SignUpResult x21 = new X21_SignUpResult();
                if (Objects.nonNull(deviceEntry)) {
                    x21.setSuccess();
                    x21.setToken(IoUtil.hex2bin(deviceEntry.getToken()));
                    x21.setPasswordId(deviceEntry.getPasswordId());
                    mapSession(deviceEntry.getDeviceUID(), session);
                }
                else {
                    x21.setFailed();
                }
                return x21;
            default:
                return null;
        }

    }

    @Override
    public ICommand find(ICommand key, ISession<ZContext> session) {
        DeviceEntry deviceEntry = _Repository.find(key);
        switch (key.getSerial()) {
            case X22_SignIn.COMMAND:
                X23_SignInResult x23 = new X23_SignInResult();
                if (Objects.nonNull(deviceEntry)) {
                    x23.setSuccess();
                    x23.setInvalidTime(deviceEntry.getInvalidTime());
                    mapSession(deviceEntry.getDeviceUID(), session);
                }
                else {
                    x23.setFailed();
                }
                return x23;
            default:
                return null;
        }
    }

    public void localBizSend(long deviceId, ICommand... toSends) {
        localSend(findSessionByIndex(deviceId), WsZSort.SERVER.getTransfer(), toSends);
    }

    public void localBizClose(long deviceId) {
        localClose(findSessionByIndex(deviceId), WsZSort.SERVER.getCloseOperator());
    }
}
