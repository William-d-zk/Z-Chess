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

package com.isahl.chess.queen.io.core.executor;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import com.isahl.chess.king.base.disruptor.MultiBufferBatchEventProcessor;
import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.config.IClusterConfig;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.event.handler.DecodeHandler;
import com.isahl.chess.queen.event.handler.EncodeHandler;
import com.isahl.chess.queen.event.handler.EncodedHandler;
import com.isahl.chess.queen.event.handler.WriteDispatcher;
import com.isahl.chess.queen.event.handler.cluster.ClusterMappingHandler;
import com.isahl.chess.queen.event.handler.cluster.DecodedDispatcher;
import com.isahl.chess.queen.event.handler.cluster.IClusterCustom;
import com.isahl.chess.queen.event.handler.cluster.IConsistentCustom;
import com.isahl.chess.queen.event.handler.cluster.IoDispatcher;
import com.isahl.chess.queen.event.handler.cluster.NotifyHandler;
import com.isahl.chess.queen.event.handler.mix.ILogicHandler;
import com.isahl.chess.queen.event.QEvent;
import com.isahl.chess.queen.io.core.async.AioWorker;
import com.isahl.chess.queen.io.core.inf.IEncryptHandler;
import com.isahl.chess.queen.io.core.manager.ClusterManager;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;

public class ClusterCore
        extends
        ThreadPoolExecutor
        implements
        IPipeCore,
        ILocalPublisher
{

    private final int                  _DecoderCount;
    private final int                  _EncoderCount;
    private final int                  _LogicCount;
    private final int                  _ClusterIoCount;
    private final int                  _ClusterQueueSize;
    private final int                  _AioQueueSize;
    private final int                  _ErrorQueueSize;
    private final RingBuffer<QEvent>[] _AioProducerEvents;
    private final SequenceBarrier[]    _AioProducerBarriers;
    private final RingBuffer<QEvent>   _ClusterLocalCloseEvent;
    private final RingBuffer<QEvent>   _ClusterLocalSendEvent;
    private final RingBuffer<QEvent>   _ClusterWriteEvent;
    private final RingBuffer<QEvent>   _ConsensusEvent;
    private final RingBuffer<QEvent>   _ConsensusApiEvent;
    private final RingBuffer<QEvent>   _LogicEvent;

    private final ReentrantLock _ClusterLock      = new ReentrantLock();
    private final ReentrantLock _ConsensusLock    = new ReentrantLock();
    private final ReentrantLock _ConsensusApiLock = new ReentrantLock();

    private final ConcurrentLinkedQueue<RingBuffer<QEvent>> _ClusterCacheConcurrentQueue;

    private final Logger _Logger = Logger.getLogger("io.queen.core." + getClass().getSimpleName());

    private final ThreadFactory _ClusterThreadFactory = new ThreadFactory()
    {
        int count;

        @Override
        public Thread newThread(Runnable r)
        {
            return new AioWorker(r,
                                 String.format("AioWorker.cluster.%d", count++),
                                 _ClusterCacheConcurrentQueue::offer,
                                 _ClusterCacheConcurrentQueue.poll());
        }
    };

    private static final ThreadFactory _SelfThreadFactory = new ThreadFactory()
    {
        private final AtomicInteger   _ThreadNumber    = new AtomicInteger(1);
        private final SecurityManager _SecurityManager = System.getSecurityManager();
        private final ThreadGroup     _ThreadGroup     = (_SecurityManager != null) ? _SecurityManager.getThreadGroup()
                                                                                    : Thread.currentThread()
                                                                                            .getThreadGroup();;

        @Override
        public Thread newThread(Runnable r)
        {
            return new Thread(_ThreadGroup, r, "ClusterCore." + _ThreadNumber.getAndIncrement());
        }
    };

    /**
     * 
     */
    private AsynchronousChannelGroup mClusterChannelGroup;

    @SuppressWarnings("unchecked")
    public ClusterCore(IClusterConfig config)
    {
        super(config.getPoolSize(),
              config.getPoolSize(),
              100,
              TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<>(),
              _SelfThreadFactory);
        _ClusterCacheConcurrentQueue = new ConcurrentLinkedQueue<>();
        _ClusterIoCount = 1 << config.getClusterIoCountPower();
        _DecoderCount = 1 << config.getDecoderCountPower();
        _EncoderCount = 1 << config.getEncoderCountPower();
        _LogicCount = 1 << config.getLogicCountPower();
        _ClusterQueueSize = 1 << config.getClusterQueueSizePower();
        _AioQueueSize = 1 << config.getAioQueueSizePower();
        _ErrorQueueSize = 1 << config.getErrorQueueSizePower();
        final int _CloserQueueSize = 1 << config.getCloserQueueSizePower();
        final int _LogicQueueSize = 1 << config.getLogicQueueSizePower();
        _AioProducerEvents = new RingBuffer[_ClusterIoCount];
        _AioProducerBarriers = new SequenceBarrier[_AioProducerEvents.length];
        Arrays.setAll(_AioProducerEvents, slot ->
        {
            RingBuffer<QEvent> rb = createPipelineYield(_AioQueueSize);
            if (!_ClusterCacheConcurrentQueue.offer(rb)) {
                _Logger.warning(String.format("cluster io cache queue offer failed :%d", slot));
            }
            return rb;
        });
        Arrays.setAll(_AioProducerBarriers, slot -> _AioProducerEvents[slot].newBarrier());

        _ClusterLocalCloseEvent = createPipelineLite(_CloserQueueSize);
        _ClusterLocalSendEvent = createPipelineLite(_CloserQueueSize);
        _ClusterWriteEvent = createPipelineYield(_ClusterQueueSize << 2);
        _ConsensusEvent = createPipelineYield(_ClusterQueueSize);
        _ConsensusApiEvent = createPipelineLite(_ClusterQueueSize);
        _LogicEvent = createPipelineLite(_LogicQueueSize);
    }

    /* @formatter:off
     * ║ barrier, ━> publish event, ━━ pipeline, | event handler

     * ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     * ┃                                                                                      Api   ━> _ConsensusApiEvent ━━━━━━━━━━━━━━━║                  ┏━> _ClusterNotifiers[0] {CallBack}|                                                                                        ┃
     * ┃  ━> _AioProducerEvents ║                                                             Timer ━> _ConsensusEvent  ━━━━━━━━━━━━━━━━━║                  ┃   _ClusterNotifiers[.] {CallBack}|                                                                                        ┃
     * ┃  ━> _ClusterLocalClose ║                  ┏> _ClusterIoEvent ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━║                  ┃   _ClusterNotifiers[N] {CallBack}|                                                                                        ┃
     * ┗━━━> _ErrorEvent[0]     ║ _IoDispatcher ━━━┫  _ReadEvents[0]{_DecodeProcessors}|━║                     ┏━━> _ClusterDecoded ━━━━━║_ClusterProcessor ╋━> _ClusterWriteEvent ━━━║                   ┏>_EncodeEvents[0]━{_EncodeProcessors}|━║                                     ┃
     * ┏━━━> _ErrorEvent[1]     ║                  ┃  _ReadEvents[.]{_DecodeProcessors}|━║ _DecodedDispatcher ━╋━━> _LogicEvent     ━━━━━{_LogicProcessor}|━╋━━━━━━━━━━━━━━━━━━━━━━━━━║ _WriteDispatcher ━┫ _EncodeEvents[.]━{_EncodeProcessors}|━║ _EncodedProcessor ┳━━> _ErrorEvent ━┛
     * ┃┏━━> _ErrorEvent[2]     ║                  ┃  _ReadEvents[N]{_DecodeProcessors}|━║                     ┗━━> _ErrorEvent ━━━━━┓                      ┗━> _ErrorEvent ━━┓       ║                   ┃ _EncodeEvents[M]━{_EncodeProcessors}|━║                   ┗━━║ [Event Done]
     * ┃┃┏━> _ErrorEvent[3]     ║                  ┗> _WroteBuffer  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━━━━║                   ┗>_ErrorEvent ━┓
     * ┃┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛           ━> _ClusterLocalSendEvent ━━━╋━━━━━━━║                                  ┃
     * ┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                          ┃
     * ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                                                                                                                                                                                          ┃

     * @formatter:on
     */
    @SuppressWarnings("unchecked")
    public <T extends IStorage> void build(ClusterManager manager,
                                           IClusterCustom<T> clusterCustom,
                                           IConsistentCustom consistentCustom,
                                           ILogicHandler logicHandler,
                                           Supplier<IEncryptHandler> encryptSupplier)
    {
        final RingBuffer<QEvent> _WroteEvent = createPipelineYield(_AioQueueSize << 1);
        final RingBuffer<QEvent> _ClusterIoEvent = createPipelineYield(_ClusterQueueSize);
        final RingBuffer<QEvent>[] _ErrorEvents = new RingBuffer[4];
        final RingBuffer<QEvent>[] _DispatchIo = new RingBuffer[_ClusterIoCount + _ErrorEvents.length + 1];
        final RingBuffer<QEvent>[] _ReadEvents = new RingBuffer[_DecoderCount];
        final SequenceBarrier[] _ReadBarriers = new SequenceBarrier[_ReadEvents.length];
        final SequenceBarrier[] _DispatchIoBarriers = new SequenceBarrier[_DispatchIo.length];
        final SequenceBarrier[] _ErrorBarriers = new SequenceBarrier[_ErrorEvents.length];
        Arrays.setAll(_ErrorEvents, slot -> createPipelineYield(_ErrorQueueSize));
        Arrays.setAll(_ErrorBarriers, slot -> _ErrorEvents[slot].newBarrier());
        IoUtil.addArray(_AioProducerEvents, _DispatchIo, _ClusterLocalCloseEvent);
        IoUtil.addArray(_ErrorEvents, _DispatchIo, _ClusterIoCount + 1);
        IoUtil.addArray(_AioProducerBarriers, _DispatchIoBarriers, _ClusterLocalCloseEvent.newBarrier());
        IoUtil.addArray(_ErrorBarriers, _DispatchIoBarriers, _ClusterIoCount + 1);
        final BatchEventProcessor<QEvent>[] _DecodeProcessors = new BatchEventProcessor[_DecoderCount];
        Arrays.setAll(_ReadEvents, slot -> createPipelineLite(_AioQueueSize));
        Arrays.setAll(_ReadBarriers, slot -> _ReadEvents[slot].newBarrier());
        Arrays.setAll(_DecodeProcessors,
                      slot -> new BatchEventProcessor<>(_ReadEvents[slot],
                                                        _ReadBarriers[slot],
                                                        new DecodeHandler(encryptSupplier.get())));
        final RingBuffer<QEvent>[] _ClusterNotifiers = new RingBuffer[_LogicCount];
        final SequenceBarrier[] _ClusterNotifyBarriers = new SequenceBarrier[_ClusterNotifiers.length];
        final BatchEventProcessor<QEvent>[] _ClusterNotifyProcessors = new BatchEventProcessor[_ClusterNotifiers.length];
        Arrays.setAll(_ClusterNotifiers, slot -> createPipelineLite(_ClusterQueueSize));
        Arrays.setAll(_ClusterNotifyBarriers, slot -> _ClusterNotifiers[slot].newBarrier());
        Arrays.setAll(_ClusterNotifyProcessors,
                      slot -> new BatchEventProcessor<>(_ClusterNotifiers[slot],
                                                        _ClusterNotifyBarriers[slot],
                                                        new NotifyHandler()));
        for (int i = 0, size = _ClusterNotifiers.length; i < size; i++) {
            _ClusterNotifiers[i].addGatingSequences(_ClusterNotifyProcessors[i].getSequence());
        }
        final RingBuffer<QEvent> _ClusterDecoded = createPipelineLite(_ClusterQueueSize << 1);
        final RingBuffer<QEvent>[] _ClusterEvents = new RingBuffer[]{_ClusterIoEvent,
                                                                     _ClusterDecoded,
                                                                     _ConsensusApiEvent,
                                                                     _ConsensusEvent};
        final SequenceBarrier[] _ClusterBarriers = new SequenceBarrier[]{_ClusterIoEvent.newBarrier(),
                                                                         _ClusterDecoded.newBarrier(),
                                                                         _ConsensusApiEvent.newBarrier(),
                                                                         _ConsensusEvent.newBarrier()};
        final MultiBufferBatchEventProcessor<QEvent> _ClusterProcessor = new MultiBufferBatchEventProcessor<>(_ClusterEvents,
                                                                                                              _ClusterBarriers,
                                                                                                              new ClusterMappingHandler<>("CONSENSUS",
                                                                                                                                          manager,
                                                                                                                                          _ErrorEvents[2],
                                                                                                                                          _ClusterWriteEvent,
                                                                                                                                          _ClusterNotifiers,
                                                                                                                                          clusterCustom,
                                                                                                                                          consistentCustom));
        _ClusterProcessor.setThreadName("ClusterProcessor");
        for (int i = 0, size = _ClusterEvents.length; i < size; i++) {
            _ClusterEvents[i].addGatingSequences(_ClusterProcessor.getSequences()[i]);
        }
        final MultiBufferBatchEventProcessor<QEvent> _IoDispatcher = new MultiBufferBatchEventProcessor<>(_DispatchIo,
                                                                                                          _DispatchIoBarriers,
                                                                                                          new IoDispatcher(_ClusterIoEvent,
                                                                                                                           _WroteEvent,
                                                                                                                           _ReadEvents));
        _IoDispatcher.setThreadName("IoDispatcher");
        for (int i = 0, size = _DispatchIo.length; i < size; i++) {
            _DispatchIo[i].addGatingSequences(_IoDispatcher.getSequences()[i]);
        }
        /* Decoded dispatch */
        final SequenceBarrier[] _DecodedBarriers = new SequenceBarrier[_DecoderCount];
        Arrays.setAll(_DecodedBarriers, slot -> _ReadEvents[slot].newBarrier(_DecodeProcessors[slot].getSequence()));
        final MultiBufferBatchEventProcessor<QEvent> _DecodedDispatcher = new MultiBufferBatchEventProcessor<>(_ReadEvents,
                                                                                                               _DecodedBarriers,
                                                                                                               new DecodedDispatcher(_ClusterDecoded,
                                                                                                                                     _ErrorEvents[3],
                                                                                                                                     _LogicEvent));
        _DecodedDispatcher.setThreadName("DecodedDispatcher");
        for (int i = 0; i < _DecoderCount; i++) {
            _ReadEvents[i].addGatingSequences(_DecodedDispatcher.getSequences()[i]);
        }
        final BatchEventProcessor<QEvent> _LogicProcessor = new BatchEventProcessor<>(_LogicEvent,
                                                                                      _LogicEvent.newBarrier(),
                                                                                      logicHandler);

        /* wait to send */
        final RingBuffer<QEvent>[] _SendEvents = new RingBuffer[]{_LogicEvent,
                                                                  _ClusterWriteEvent,
                                                                  _ClusterLocalSendEvent,
                                                                  _WroteEvent};
        final SequenceBarrier[] _SendBarriers = new SequenceBarrier[_SendEvents.length];
        Arrays.setAll(_SendBarriers,
                      slot -> slot == 0 ? _SendEvents[slot].newBarrier(_LogicProcessor.getSequence())
                                        : _SendEvents[slot].newBarrier());
        final RingBuffer<QEvent>[] _EncodeEvents = new RingBuffer[_EncoderCount];
        Arrays.setAll(_EncodeEvents, slot -> createPipelineLite(_AioQueueSize));
        final MultiBufferBatchEventProcessor<QEvent> _WriteDispatcher = new MultiBufferBatchEventProcessor<>(_SendEvents,
                                                                                                             _SendBarriers,
                                                                                                             new WriteDispatcher(_ErrorEvents[1],
                                                                                                                                 _EncodeEvents));
        _WriteDispatcher.setThreadName("WriteDispatcher");
        for (int i = 0, size = _SendEvents.length; i < size; i++) {
            _SendEvents[i].addGatingSequences(_WriteDispatcher.getSequences()[i]);
        }
        final BatchEventProcessor<QEvent>[] _EncodeProcessors = new BatchEventProcessor[_EncodeEvents.length];
        Arrays.setAll(_EncodeProcessors,
                      slot -> new BatchEventProcessor<>(_EncodeEvents[slot],
                                                        _EncodeEvents[slot].newBarrier(),
                                                        new EncodeHandler(encryptSupplier.get())));
        final SequenceBarrier[] _EncodedBarriers = new SequenceBarrier[_EncodeEvents.length];
        Arrays.setAll(_EncodedBarriers, slot -> _EncodeEvents[slot].newBarrier(_EncodeProcessors[slot].getSequence()));
        final MultiBufferBatchEventProcessor<QEvent> _EncodedProcessor = new MultiBufferBatchEventProcessor<>(_EncodeEvents,
                                                                                                              _EncodedBarriers,
                                                                                                              new EncodedHandler(_ErrorEvents[0]));
        _EncodedProcessor.setThreadName("EncodedProcessor");
        for (int i = 0; i < _EncoderCount; i++) {
            _EncodeEvents[i].addGatingSequences(_EncodedProcessor.getSequences()[i]);
        }
        /*-------------------------------------------------------------------------------------------------------------------------------------*/
        submit(_IoDispatcher);
        Arrays.stream(_DecodeProcessors)
              .forEach(this::submit);
        submit(_DecodedDispatcher);
        submit(_ClusterProcessor);
        Arrays.stream(_ClusterNotifyProcessors)
              .forEach(this::submit);
        submit(_LogicProcessor);
        submit(_WriteDispatcher);
        Arrays.stream(_EncodeProcessors)
              .forEach(this::submit);
        submit(_EncodedProcessor);
        _Logger.info("%s =>>>>>>>>>>start", getClass().getSimpleName());
    }

    @Override
    public AsynchronousChannelGroup getClusterChannelGroup() throws IOException
    {
        if (mClusterChannelGroup == null) {
            mClusterChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(_ClusterIoCount, _ClusterThreadFactory);
        }
        return mClusterChannelGroup;
    }

    @Override
    public ReentrantLock getLock(OperatorType type)
    {
        return switch (type)
        {
            case CLUSTER_LOCAL -> _ClusterLock;
            case CONSENSUS -> _ConsensusApiLock;
            case CLUSTER_TIMER -> _ConsensusLock;
            default -> throw new IllegalArgumentException(String.format("error type:%s", type));
        };
    }

    @Override
    public RingBuffer<QEvent> getPublisher(OperatorType type)
    {
        return switch (type)
        {
            case CLUSTER_LOCAL -> _ClusterLocalSendEvent;
            case CONSENSUS -> _ConsensusApiEvent;
            case CLUSTER_TIMER -> _ConsensusEvent;
            default -> throw new IllegalArgumentException(String.format("get publisher type error:%s ", type.name()));
        };
    }

    @Override
    public RingBuffer<QEvent> getCloser(OperatorType type)
    {
        if (type == OperatorType.CLUSTER_LOCAL) { return _ClusterLocalCloseEvent; }
        throw new IllegalArgumentException(String.format("get closer type error:%s ", type.name()));
    }

    @Override
    public RingBuffer<QEvent> getConsensusEvent()
    {
        return _ConsensusEvent;
    }

    @Override
    public ReentrantLock getConsensusLock()
    {
        return _ConsensusLock;
    }

}
