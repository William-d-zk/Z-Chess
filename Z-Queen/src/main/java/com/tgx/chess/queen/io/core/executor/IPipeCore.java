/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.queen.io.core.executor;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.LiteTimeoutBlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.tgx.chess.queen.event.processor.QEvent;

/**
 * @author william.d.zk
 * @date 2020/4/21
 */
public interface IPipeCore
{
    AsynchronousChannelGroup getClusterChannelGroup() throws IOException;

    default RingBuffer<QEvent> createPipeline(int size, WaitStrategy waitStrategy)
    {
        return RingBuffer.createSingleProducer(QEvent.EVENT_FACTORY, size, waitStrategy);
    }

    default RingBuffer<QEvent> createPipelineYield(int size)
    {
        return createPipeline(size, new YieldingWaitStrategy());
    }

    default RingBuffer<QEvent> createPipelineLite(int size)
    {
        return createPipeline(size, new LiteBlockingWaitStrategy());
    }

    default RingBuffer<QEvent> createPipelineTimeoutLite(int size)
    {
        return createPipeline(size, new LiteTimeoutBlockingWaitStrategy(5, TimeUnit.SECONDS));
    }

    RingBuffer<QEvent> getConsensusEvent();

    ReentrantLock getConsensusLock();
}
