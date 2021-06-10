/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.queen.io.core.manager;

import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.event.QEvent;
import com.isahl.chess.queen.event.handler.cluster.IClusterCustom;
import com.isahl.chess.queen.event.handler.cluster.IConsistentCustom;
import com.isahl.chess.queen.event.handler.mix.ILogicHandler;
import com.isahl.chess.queen.io.core.async.AioManager;
import com.isahl.chess.queen.io.core.executor.ClusterCore;
import com.isahl.chess.queen.io.core.executor.IClusterCore;
import com.isahl.chess.queen.io.core.executor.ILocalPublisher;
import com.isahl.chess.queen.io.core.inf.IActivity;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.IEncryptHandler;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.lmax.disruptor.RingBuffer;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class ClusterManager
        extends
        AioManager
        implements
        IActivity,
        IClusterCore,
        ILocalPublisher
{
    private final ClusterCore _ClusterCore;

    public ClusterManager(IAioConfig config,
                          ClusterCore clusterCore)
    {
        super(config);
        _ClusterCore = clusterCore;
    }

    @Override
    public void close(ISession session, OperatorType type)
    {
        _ClusterCore.close(session, type);
    }

    @Override
    public RingBuffer<QEvent> getPublisher(OperatorType type)
    {
        return _ClusterCore.getPublisher(type);
    }

    @Override
    public RingBuffer<QEvent> getCloser(OperatorType type)
    {
        return _ClusterCore.getCloser(type);
    }

    @Override
    public ReentrantLock getLock(OperatorType type)
    {
        return _ClusterCore.getLock(type);
    }

    @Override
    public final boolean send(ISession session, OperatorType type, IControl... commands)
    {
        return _ClusterCore.send(session, type, commands);
    }

    public <T extends IStorage> void build(IClusterCustom<T> clusterCustom,
                                           IConsistentCustom consistentCustom,
                                           ILogicHandler logicHandler,
                                           Supplier<IEncryptHandler> encryptSupplier)
    {
        _ClusterCore.build(this, clusterCustom, consistentCustom, logicHandler, encryptSupplier);
    }

    @Override
    public AsynchronousChannelGroup getClusterChannelGroup() throws IOException
    {
        return _ClusterCore.getClusterChannelGroup();
    }
}
