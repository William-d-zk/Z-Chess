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

import static com.tgx.chess.bishop.io.zoperator.ZOperators.SERVER_TRANSFER;
import static com.tgx.chess.queen.event.inf.IOperator.Type.WRITE;
import static com.tgx.chess.queen.io.core.inf.IoHandler.CONNECTED_OPERATOR;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.bishop.biz.db.dao.DeviceEntry;
import com.tgx.chess.bishop.io.zcrypt.EncryptHandler;
import com.tgx.chess.bishop.io.zhandler.ZLinkedHandler;
import com.tgx.chess.bishop.io.zoperator.ZOperators;
import com.tgx.chess.bishop.io.zprotocol.ZContext;
import com.tgx.chess.bishop.io.zprotocol.control.X101_HandShake;
import com.tgx.chess.bishop.io.zprotocol.control.X103_Close;
import com.tgx.chess.bishop.io.zprotocol.control.X104_Ping;
import com.tgx.chess.bishop.io.zprotocol.control.X105_Pong;
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
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.king.config.Config;
import com.tgx.chess.queen.config.QueenCode;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.async.AioCreator;
import com.tgx.chess.queen.io.core.async.AioSession;
import com.tgx.chess.queen.io.core.executor.ServerCore;
import com.tgx.chess.queen.io.core.inf.IAioServer;
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
import com.tgx.chess.queen.io.core.manager.QueenManager;

public class DeviceNode
        extends
        QueenManager
        implements
        ISessionDismiss,
        ISessionCreated
{
    private final Logger                   _Log = Logger.getLogger(getClass().getName());
    private final String                   _ServerHost;
    private final int                      _ServerPort;
    private final IAioServer               _DeviceServer;
    private final ISessionCreator          _SessionCreator;
    private final ICommandCreator          _CommandCreator;
    private final IRepository<DeviceEntry> _Repository;

    @Override
    public void onDismiss(ISession session)
    {
        rmSession(session);
    }

    @Override
    public void onCreate(ISession session)
    {
        /* 进入这里的都是 _DeviceServer 建立的链接*/
        session.setIndex(QueenCode.CM_XID);
        addSession(session);
    }

    public DeviceNode(String host,
                      int port,
                      IRepository<DeviceEntry> respository)
    {
        super(new Config("device"), new ServerCore()
        {

            @Override
            public RingBuffer<QEvent> getLocalPublisher(ISession session)
            {
                switch (getSlot(session))
                {
                    case QueenCode.CM_XID_LOW:
                    case QueenCode.RM_XID_LOW:
                        return getClusterLocalSendEvent();
                    default:
                        return getBizLocalSendEvent();
                }
            }

            @Override
            public RingBuffer<QEvent> getLocalCloser(ISession session)
            {
                switch (getSlot(session))
                {
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
        _SessionCreator = new AioCreator(getConfig())
        {
            @Override
            public ISession createSession(AsynchronousSocketChannel socketChannel, IConnectActive active)
            {
                try {
                    return new AioSession(socketChannel,
                                          active,
                                          this,
                                          this,
                                          DeviceNode.this,
                                          active.getHandler()
                                                .getInOperator());
                }
                catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public IContext createContext(ISessionOption option, ISort ioHandler)
            {
                return new ZContext(option, ioHandler);

            }
        };
        _CommandCreator = (session) -> null;
        _DeviceServer = new IAioServer()
        {
            private final InetSocketAddress _LocalBind = new InetSocketAddress(_ServerHost, _ServerPort);

            @Override
            public InetSocketAddress getRemoteAddress()
            {
                throw new UnsupportedOperationException(" server hasn't remote address");
            }

            @Override
            public InetSocketAddress getLocalAddress()
            {
                return _LocalBind;
            }

            @Override
            public IOperator<Throwable,
                             IAioServer> getErrorOperator()
            {
                return new IOperator<Throwable,
                                     IAioServer>()
                {
                    @Override
                    @SuppressWarnings("unchecked")
                    public Triple<Throwable,
                                  IAioServer,
                                  IOperator<Throwable,
                                            IAioServer>> handle(Throwable throwable, IAioServer active)
                    {
                        _Log.warning(String.format("accept  failed%s", active.toString()), throwable);
                        return new Triple<>(throwable, active, this);
                    }
                };
            }

            @Override
            public IOperator<IConnectionContext,
                             AsynchronousSocketChannel> getConnectedOperator()
            {
                return CONNECTED_OPERATOR();
            }

            private AsynchronousServerSocketChannel mServerChannel;

            @Override
            public void bindAddress(InetSocketAddress address, AsynchronousChannelGroup channelGroup) throws IOException
            {
                mServerChannel = AsynchronousServerSocketChannel.open(channelGroup);
                mServerChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                mServerChannel.bind(address, 1 << 6);
            }

            @Override
            public void pendingAccept()
            {
                if (mServerChannel.isOpen()) mServerChannel.accept(this, this);
            }

            @Override
            public ZOperators getHandler()
            {
                return ZOperators.SERVER;
            }

            @Override
            public ISessionCreator getSessionCreator()
            {
                return _SessionCreator;
            }

            @Override
            public ISessionCreated getSessionCreated()
            {
                return DeviceNode.this;
            }

            @Override
            public ICommandCreator getCommandCreator()
            {
                return _CommandCreator;
            }

        };
        _Repository = respository;
        _Log.info("Device Node Bean Load");
    }

    public void start() throws IOException
    {
        _ServerCore.build(queenManager -> (event, sequence, endOfBatch) -> {
            //前置的 dispatcher 将 ICommands 拆分了
            if (IOperator.Type.LOGIC.equals(event.getEventType())) {
                Pair<ICommand,
                     ISession> logicContent = event.getContent();
                ICommand cmd = logicContent.first();
                ISession session = logicContent.second();
                if (Objects.isNull(cmd)) {
                    _Log.warning("cmd null");
                }
                else {
                    _Log.info("device node logic handle %s", cmd);
                    switch (cmd.getSerial())
                    {
                        case X30_EventMsg.COMMAND:
                            cmd = new X31_ConfirmMsg(cmd.getUID());
                            break;
                        case X31_ConfirmMsg.COMMAND:
                            cmd = new X32_MsgStatus(cmd.getUID());
                            break;
                        case X50_DeviceMsg.COMMAND:
                            cmd = new X51_DeviceMsgAck(cmd.getUID());
                            break;
                        case X51_DeviceMsgAck.COMMAND:
                            break;
                        case X101_HandShake.COMMAND:
                            break;
                        case X103_Close.COMMAND:
                            cmd = null;
                            localClose(session);
                            break;
                        case X104_Ping.COMMAND:
                            cmd = new X105_Pong("Server pong".getBytes());
                            break;
                        case X105_Pong.COMMAND:
                            cmd = null;
                            break;
                        default:
                            break;
                    }
                    if (Objects.nonNull(cmd)) {
                        event.produce(WRITE, new ICommand[] { cmd }, session, SERVER_TRANSFER());
                        return;
                    }
                }
            }
            event.ignore();
        }, this, new ZLinkedHandler(), new EncryptHandler());
        _DeviceServer.bindAddress(new InetSocketAddress(_ServerHost, _ServerPort),
                                  AsynchronousChannelGroup.withFixedThreadPool(_ServerCore.getServerCount(), _ServerCore.getWorkerThreadFactory()));
        _DeviceServer.pendingAccept();
        _Log.info(String.format("device node start %s:%d", _ServerHost, _ServerPort));
    }

    @Override
    public ICommand save(ICommand tar, ISession session)
    {
        DeviceEntry deviceEntry = _Repository.save(tar);
        switch (tar.getSerial())
        {
            case X20_SignUp.COMMAND:
                X21_SignUpResult x21 = new X21_SignUpResult();
                if (Objects.nonNull(deviceEntry)) {
                    x21.setSuccess();
                    x21.setToken(IoUtil.hex2bin(deviceEntry.getToken()));
                    x21.setPasswordId(deviceEntry.getPasswordId());
                    mapSession(deviceEntry.getDeviceUID(), session);
                }
                else x21.setFailed();
                return x21;
            default:
                return null;
        }

    }

    @Override
    public ICommand find(ICommand key, ISession session)
    {
        DeviceEntry deviceEntry = _Repository.find(key);
        switch (key.getSerial())
        {
            case X22_SignIn.COMMAND:
                X23_SignInResult x23 = new X23_SignInResult();
                if (Objects.nonNull(deviceEntry)) {
                    x23.setSuccess();
                    x23.setInvalidTime(deviceEntry.getInvalidTime());
                    mapSession(deviceEntry.getDeviceUID(), session);
                }
                else x23.setFailed();
                return x23;
            default:
                return null;
        }
    }

    public void localBizSend(long deviceId, ICommand... toSends)
    {
        localSend(findSessionByIndex(deviceId), toSends);
    }

    public void localBizClose(long deviceId)
    {
        localClose(findSessionByIndex(deviceId));
    }
}
