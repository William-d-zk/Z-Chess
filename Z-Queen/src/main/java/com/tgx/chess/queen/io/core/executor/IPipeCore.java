package com.tgx.chess.queen.io.core.executor;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;

import com.lmax.disruptor.LiteBlockingWaitStrategy;
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

    default RingBuffer<QEvent> createPipeline(int power, WaitStrategy waitStrategy)
    {
        return RingBuffer.createSingleProducer(QEvent.EVENT_FACTORY, 1 << power, waitStrategy);
    }

    default RingBuffer<QEvent> createPipelineYield(int power)
    {
        return createPipeline(power, new YieldingWaitStrategy());
    }

    default RingBuffer<QEvent> createPipelineLite(int power)
    {
        return createPipeline(power, new LiteBlockingWaitStrategy());
    }
}
