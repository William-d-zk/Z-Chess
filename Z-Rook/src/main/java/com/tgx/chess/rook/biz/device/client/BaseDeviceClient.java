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

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.king.config.Config;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.io.core.async.AioCreator;
import com.tgx.chess.queen.io.core.async.AioSession;
import com.tgx.chess.queen.io.core.async.BaseAioConnector;
import com.tgx.chess.queen.io.core.executor.ClientCore;
import com.tgx.chess.queen.io.core.inf.IAioClient;
import com.tgx.chess.queen.io.core.inf.IAioConnector;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ICommandCreator;
import com.tgx.chess.queen.io.core.inf.IConnectionContext;
import com.tgx.chess.queen.io.core.inf.IContextCreator;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionCreated;
import com.tgx.chess.queen.io.core.inf.ISessionCreator;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.inf.ISessionOption;

/**
 * @author william.d.zk
 * @date 2019-05-12
 */
public abstract class BaseDeviceClient<C extends ZContext>
        implements
        IAioClient,
        ISessionDismiss<C>,
        ISessionCreated<C>,
        ICommandCreator<C>,
        IContextCreator<C>
{
    final Logger _Logger = Logger.getLogger(getClass().getName());

    final String                           _TargetHost;
    private final String                   _TargetName;
    private final int                      _TargetPort;
    private final Config                   _Config;
    private final ISessionCreator<C>       _SessionCreator;
    private final IAioConnector<C>         _DeviceConnector;
    private final AsynchronousChannelGroup _ChannelGroup;
    final ClientCore<C>                    _ClientCore     = new ClientCore<>();
    private final TimeWheel                _TimeWheel      = _ClientCore.getTimeWheel();
    private final AtomicInteger            _State          = new AtomicInteger();
    final AtomicReference<byte[]>          currentTokenRef = new AtomicReference<>();
    private final ISort<C>                 _Sort;
    ISession<C>                            clientSession;

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

    public BaseDeviceClient(String targetName,
                            String targetHost,
                            int targetPort,
                            ISort<C> sort) throws IOException
    {
        _State.set(STATE.STOP.ordinal());
        _TargetName = targetName;
        _TargetHost = targetHost;
        _TargetPort = targetPort;
        _Sort = sort;
        _Config = new Config();
        _ChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(1, _ClientCore.getWorkerThreadFactory());
        _SessionCreator = new AioCreator<C>(_Config)
        {
            @Override
            public ISession<C> createSession(AsynchronousSocketChannel socketChannel, IConnectionContext<C> context)
            {
                try {
                    return new AioSession<>(socketChannel,
                                            context.getConnectActive(),
                                            this,
                                            this,
                                            BaseDeviceClient.this);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public C createContext(ISessionOption option, ISort sort)
            {
                return BaseDeviceClient.this.createContext(option, sort);
            }
        };
        _DeviceConnector = new BaseAioConnector<C>(_TargetHost, _TargetPort, _Sort.getEncoder(), _Sort.getDecoder())
        {

            @Override
            public ISort<C> getSort()
            {
                return _Sort;
            }

            @Override
            public ISessionCreator<C> getSessionCreator()
            {
                return _SessionCreator;
            }

            @Override
            public ISessionCreated<C> getSessionCreated()
            {
                return BaseDeviceClient.this;
            }

            @Override
            public ICommandCreator<C> getCommandCreator()
            {
                return BaseDeviceClient.this;
            }
        };

    }

    public void connect()
    {
        try {
            connect(_DeviceConnector, _ChannelGroup);
            _TimeWheel.acquire(3, TimeUnit.SECONDS, _DeviceConnector, new ScheduleHandler<>(false, connector ->
            {
                if (Objects.nonNull(clientSession)) {
                    _Logger.info("connect status checked -> success");
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
    public void onCreate(ISession<C> session)
    {
        _Logger.info("client connect:%s", session);
        clientSession = session;
        updateState(STATE.OFFLINE);
    }

    @Override
    public void onDismiss(ISession<C> session)
    {
        _Logger.info("dismiss:%s", session);
        if (clientSession == session) {
            _Logger.info("drop client session %s", session);
            clientSession = null;
            updateState(STATE.SHUTDOWN);
        }
    }

    public boolean isOnline()
    {
        return Objects.nonNull(clientSession) && _State.get() >= STATE.ONLINE.ordinal();
    }

    private boolean isOffline()
    {
        return Objects.isNull(clientSession) && _State.get() <= STATE.OFFLINE.ordinal();
    }

    @SafeVarargs
    public final boolean sendLocal(ICommand<C>... toSends)
    {
        if (isOffline()) { throw new IllegalStateException("client is offline"); }
        return _ClientCore.localSend(clientSession, _Sort.getTransfer(), toSends);
    }

    public void close()
    {
        _ClientCore.localClose(clientSession, _Sort.getCloser());
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
