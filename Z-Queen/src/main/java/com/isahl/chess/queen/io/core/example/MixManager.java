/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.isahl.chess.queen.io.core.example;

import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.db.model.IStorage;
import com.isahl.chess.queen.events.cluster.IClusterCustom;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.events.server.ILinkCustom;
import com.isahl.chess.queen.events.server.ILogicHandler;
import com.isahl.chess.queen.io.core.features.model.channels.IActivity;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.zls.IEncryptor;
import com.isahl.chess.queen.io.core.net.socket.AioManager;
import com.isahl.chess.queen.io.core.tasks.ServerCore;
import com.isahl.chess.queen.io.core.tasks.features.IBizCore;
import com.isahl.chess.queen.io.core.tasks.features.IClusterCore;
import com.isahl.chess.queen.io.core.tasks.features.ILocalPublisher;
import com.lmax.disruptor.RingBuffer;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * @author william.d.zk
 */
public class MixManager
        extends AioManager
        implements IActivity,
                   IBizCore,
                   IClusterCore,
                   ILocalPublisher
{
    private final ServerCore _ServerCore;

    public MixManager(IAioConfig config, ServerCore serverCore)
    {
        super(config);
        _ServerCore = serverCore;
    }

    @Override
    public void close(ISession session, IOperator.Type type)
    {
        _ServerCore.close(session, type);
    }

    @Override
    public RingBuffer<QEvent> getPublisher(IOperator.Type type)
    {
        return _ServerCore.getPublisher(type);
    }

    @Override
    public RingBuffer<QEvent> getCloser(IOperator.Type type)
    {
        return _ServerCore.getCloser(type);
    }

    @Override
    public ReentrantLock getLock(IOperator.Type type)
    {
        return _ServerCore.getLock(type);
    }

    @Override
    public final boolean send(ISession session, IOperator.Type type, IControl... commands)
    {
        return _ServerCore.send(session, type, commands);
    }

    public <T extends IStorage> void build(ILogicHandler.factory logicFactory,
                                           ILinkCustom linkCustom,
                                           IClusterCustom<T> clusterCustom,
                                           Supplier<IEncryptor> encryptSupplier)
    {
        _ServerCore.build(this, logicFactory, linkCustom, clusterCustom, encryptSupplier);
    }

    @Override
    public AsynchronousChannelGroup getServiceChannelGroup() throws IOException
    {
        return _ServerCore.getServiceChannelGroup();
    }

    @Override
    public AsynchronousChannelGroup getClusterChannelGroup() throws IOException
    {
        return _ServerCore.getClusterChannelGroup();
    }
}
