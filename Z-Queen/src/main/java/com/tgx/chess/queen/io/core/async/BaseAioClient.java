/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2020 Z-Chess                                                
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

package com.tgx.chess.queen.io.core.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.io.core.inf.IAioClient;
import com.tgx.chess.queen.io.core.inf.IAioConnector;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.ISession;

public class BaseAioClient<C extends IContext<C>>
        implements
        IAioClient<C>
{
    private final Logger                                                  _Logger          = Logger.getLogger("AioClient");
    private final TimeWheel                                               _TimeWheel;
    private final AsynchronousChannelGroup                                _ChannelGroup;
    private final Map<InetSocketAddress,
                      Pair<Integer,
                           IAioConnector<C>>>                             _TargetManageMap = new HashMap<>();

    public BaseAioClient(TimeWheel timeWheel,
                         AsynchronousChannelGroup channelGroup)
    {
        Objects.requireNonNull(timeWheel);
        Objects.requireNonNull(channelGroup);
        _TimeWheel = timeWheel;
        _ChannelGroup = channelGroup;
    }

    private final ReentrantLock _Lock = new ReentrantLock();

    @Override
    public void connect(IAioConnector<C> connector) throws IOException
    {
        _Lock.lock();
        try {
            AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(_ChannelGroup);
            InetSocketAddress remoteAddress = connector.getRemoteAddress();
            Pair<Integer,
                 IAioConnector<C>> pair = _TargetManageMap.putIfAbsent(remoteAddress, new Pair<>(0, connector));
            socketChannel.connect(remoteAddress, socketChannel, connector);
            int retryCount = 0;
            if (pair != null) {
                retryCount = pair.first();
                pair.setFirst(++retryCount);
            }
            _Logger.info("%s,%d", remoteAddress, retryCount);
        }
        finally {
            _Lock.unlock();
        }
    }

    @Override
    public void onFailed(IAioConnector<C> connector)
    {
        _Logger.info("connect failed: retry");
        delayConnect(connector);
    }

    @Override
    public void onDismiss(ISession<C> session)
    {
        InetSocketAddress remoteAddress = session.getRemoteAddress();
        IAioConnector<C> connector = _TargetManageMap.get(remoteAddress)
                                                     .second();
        _Logger.info("on dismiss: [shutdown: %s ]", connector.isShutdown());
        delayConnect(connector);
    }

    private void delayConnect(IAioConnector<C> connector)
    {
        if (connector.isShutdown()) { return; }
        _TimeWheel.acquire(connector,
                           new ScheduleHandler<>(connector.getConnectTimeout()
                                                          .multipliedBy(3),
                                                 c ->
                                                 {
                                                     try {
                                                         _Logger.info("%s connect",
                                                                      Thread.currentThread()
                                                                            .getName());
                                                         connect(c);
                                                     }
                                                     catch (IOException e) {
                                                         e.printStackTrace();
                                                     }
                                                 }));
    }

    @Override
    public void shutdown(ISession<C> session)
    {
        InetSocketAddress remoteAddress = session.getRemoteAddress();
        if (_TargetManageMap.containsKey(remoteAddress)) {
            IAioConnector<C> connector = _TargetManageMap.get(remoteAddress)
                                                         .second();
            connector.shutdown();
        }
    }
}
