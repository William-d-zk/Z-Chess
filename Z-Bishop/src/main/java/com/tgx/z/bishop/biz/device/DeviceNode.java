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

package com.tgx.z.bishop.biz.device;

import static com.tgx.z.queen.event.inf.IOperator.Type.WRITE;
import static com.tgx.z.queen.event.operator.OperatorHolder.CONNECTED_OPERATOR;
import static com.tgx.z.queen.event.operator.OperatorHolder.SERVER_ACCEPTOR;
import static com.tgx.z.queen.event.operator.OperatorHolder.SERVER_ENCODER;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import com.tgx.z.bishop.biz.db.dto.DeviceEntry;
import com.tgx.z.config.Config;
import com.tgx.z.config.QueenCode;
import com.tgx.z.queen.base.log.Logger;
import com.tgx.z.queen.base.util.Pair;
import com.tgx.z.queen.base.util.Triple;
import com.tgx.z.queen.event.inf.IOperator;
import com.tgx.z.queen.event.operator.MODE;
import com.tgx.z.queen.io.core.async.AioCreator;
import com.tgx.z.queen.io.core.async.AioSession;
import com.tgx.z.queen.io.core.executor.ServerCore;
import com.tgx.z.queen.io.core.inf.IAioServer;
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
import com.tgx.z.queen.io.core.manager.QueenManager;
import com.tgx.z.queen.io.external.websokcet.ZContext;
import com.tgx.z.queen.io.external.websokcet.bean.control.X101_HandShake;

@Service
@PropertySource({ "classpath:device.properties",
                  "classpath:db.properties" })
public class DeviceNode
        extends
        QueenManager
        implements
        ISessionDismiss,
        ISessionCreated
{
    private Logger                _Log = Logger.getLogger(getClass().getName());
    private final String          _ServerHost;
    private final int             _ServerPort;
    private final IAioServer      _DeviceServer;
    private final ISessionCreator _SessionCreator;
    private final ICommandCreator _CommandCreator;

    @Override
    public void onDismiss(ISession session) {
        rmSession(session);
    }

    @Override
    public void onCreate(ISession session) {
        /* 进入这里的都是 _DeviceServer 建立的链接*/
        session.setIndex(QueenCode.CM_XID);
        addSession(session);
    }

    public DeviceNode(@Value("${device.server.host}") String host, @Value("${device.server.port}") int port) {
        super(new Config("device"));
        _ServerHost = host;
        _ServerPort = port;
        _SessionCreator = new AioCreator(getConfig())
        {
            @Override
            public ISession createSession(AsynchronousSocketChannel socketChannel, IConnectActive active) {
                try {
                    return new AioSession(socketChannel,
                                          active,
                                          this,
                                          this,
                                          DeviceNode.this,
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
        _CommandCreator = () -> null;
        _DeviceServer = new IAioServer()
        {
            private final InetSocketAddress _LocalBind = new InetSocketAddress(_ServerHost, _ServerPort);

            @Override
            public InetSocketAddress getRemoteAddress() {
                throw new UnsupportedOperationException(" server hasn't remote address");
            }

            @Override
            public InetSocketAddress getLocalAddress() {
                return _LocalBind;
            }

            @Override
            public IOperator<Throwable, IAioServer> getErrorOperator() {
                return new IOperator<Throwable, IAioServer>()
                {
                    @Override
                    @SuppressWarnings("unchecked")
                    public Triple<Throwable, IAioServer, IOperator<Throwable, IAioServer>> handle(Throwable throwable, IAioServer active) {
                        _Log.warning(String.format("accept  failed%s", active.toString()), throwable);
                        return new Triple<>(throwable, active, this);
                    }
                };
            }

            @Override
            public IOperator<IConnectionContext, AsynchronousSocketChannel> getConnectedOperator() {
                return CONNECTED_OPERATOR();
            }

            private AsynchronousServerSocketChannel mServerChannel;

            @Override
            public void bindAddress(InetSocketAddress address, AsynchronousChannelGroup channelGroup) throws IOException {
                mServerChannel = AsynchronousServerSocketChannel.open(channelGroup);
                mServerChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                mServerChannel.bind(address, 1 << 6);
            }

            @Override
            public void pendingAccept() {
                if (mServerChannel.isOpen()) mServerChannel.accept(this, SERVER_ACCEPTOR());
            }

            @Override
            public MODE getMode() {
                return MODE.SERVER;
            }

            @Override
            public ISessionCreator getSessionCreator() {
                return _SessionCreator;
            }

            @Override
            public ISessionCreated getSessionCreated() {
                return DeviceNode.this;
            }

            @Override
            public ICommandCreator getCommandCreator() {
                return _CommandCreator;
            }

        };
        _Log.info("Device Node Bean Load");
    }

    @PostConstruct
    private void start() throws IOException {
        ServerCore<DeviceEntry> core = new ServerCore<>();
        core.build(queenManager -> (event, sequence, endOfBatch) -> {
            switch (event.getEventType()) {
                case LOGIC:
                    Pair<ICommand, ISession> logicContent = event.getContent();
                    ICommand cmd = logicContent.first();
                    ISession session = logicContent.second();
                    if (Objects.isNull(cmd)) {
                        _Log.warning("cmd null");
                    }
                    else {
                        _Log.info(cmd);
                        switch (cmd.getSerial()) {
                            case X101_HandShake.COMMAND:
                            default:
                                break;
                        }
                        event.produce(WRITE, cmd, session, SERVER_ENCODER());
                    }
                    break;
            }

        }, this);
        _DeviceServer.bindAddress(new InetSocketAddress(_ServerHost, _ServerPort),
                                  AsynchronousChannelGroup.withFixedThreadPool(core.getServerCount(), core.getWorkerThreadFactory()));
        _DeviceServer.pendingAccept();
        _Log.info(String.format("device node start %s:%d", _ServerHost, _ServerPort));
    }

}
