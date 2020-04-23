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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.queen.io.core.executor;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.tgx.chess.king.base.disruptor.MultiBufferBatchEventProcessor;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.config.IClusterConfig;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.event.handler.DecodeHandler;
import com.tgx.chess.queen.event.handler.EncodeHandler;
import com.tgx.chess.queen.event.handler.EncodedHandler;
import com.tgx.chess.queen.event.handler.IClusterCustom;
import com.tgx.chess.queen.event.handler.WriteDispatcher;
import com.tgx.chess.queen.event.handler.cluster.DecodedDispatcher;
import com.tgx.chess.queen.event.handler.cluster.INotifyCustom;
import com.tgx.chess.queen.event.handler.cluster.IoDispatcher;
import com.tgx.chess.queen.event.handler.cluster.MappingHandler;
import com.tgx.chess.queen.event.handler.cluster.NotifyHandler;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.async.socket.AioWorker;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IEncryptHandler;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.manager.ClusterManager;

public class ClusterCore<C extends IContext<C>>
        extends
        ThreadPoolExecutor
        implements
        IPipeCore,
        ILocalPublisher<C>
{
    private final int                  _DecoderCount;
    private final int                  _EncoderCount;
    private final int                  _LogicCount;
    private final int                  _ClusterIoCount;
    private final int                  _ClusterPower;
    private final int                  _AioQueuePower;
    private final int                  _ErrorPower;
    private final RingBuffer<QEvent>[] _AioProducerEvents;
    private final SequenceBarrier[]    _AioProducerBarriers;
    private final RingBuffer<QEvent>   _ClusterLocalCloseEvent;
    private final RingBuffer<QEvent>   _ClusterWriteEvent;
    private final RingBuffer<QEvent>   _ConsensusEvent;
    private final RingBuffer<QEvent>   _ConsensusApiEvent;

    private final ReentrantLock _ClusterLock      = new ReentrantLock();
    private final ReentrantLock _ConsensusLock    = new ReentrantLock();
    private final ReentrantLock _ConsensusApiLock = new ReentrantLock();

    private final ConcurrentLinkedQueue<RingBuffer<QEvent>> _ClusterCacheConcurrentQueue;
    private final Logger                                    _Logger = Logger.getLogger(getClass().getSimpleName());

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

    /**
     * 
     */
    private AsynchronousChannelGroup mClusterChannelGroup;

    @SuppressWarnings("unchecked")
    public ClusterCore(IClusterConfig config)
    {
        super(config.getPoolSize(), config.getPoolSize(), 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        _ClusterCacheConcurrentQueue = new ConcurrentLinkedQueue<>();
        _ClusterIoCount = config.getClusterIoCount();
        _DecoderCount = config.getDecoderCount();
        _EncoderCount = config.getEncoderCount();
        _LogicCount = config.getLogicCount();
        _ClusterPower = config.getClusterPower();
        _AioQueuePower = config.getAioQueuePower();
        _ErrorPower = config.getErrorPower();
        _AioProducerEvents = new RingBuffer[_ClusterIoCount];
        _AioProducerBarriers = new SequenceBarrier[_AioProducerEvents.length];
        Arrays.setAll(_AioProducerEvents, slot ->
        {
            RingBuffer<QEvent> rb = createPipelineYield(_AioQueuePower);
            if (!_ClusterCacheConcurrentQueue.offer(rb)) {
                _Logger.warning(String.format("cluster io cache queue offer failed :%d", slot));
            }
            return rb;
        });
        Arrays.setAll(_AioProducerBarriers, slot -> _AioProducerEvents[slot].newBarrier());

        _ClusterLocalCloseEvent = createPipelineLite(config.getCloserPower());
        _ClusterWriteEvent = createPipelineYield(_ClusterPower);
        _ConsensusEvent = createPipelineYield(_ClusterPower);
        _ConsensusApiEvent = createPipelineLite(_ClusterPower);
    }

    /*  ║ barrier, ━> publish event, ━━ pipeline, | event handler
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                   Api   ━> _ConsensusApiEvent ━━━━━━━━━━━┓                                                                                                                             ┃
     ┃  ━> _AioProducerEvents ║                                          Timer ━> _ConsensusEvent  ━━━━━━━━━━━━━┫                      ┏━> _ClusterNotifiers[0] | [CallBack]                                                                  ┃
     ┃  ━> _ClusterLocalClose ║                  ┏> _ClusterIoEvent ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫                      ┃   _ClusterNotifiers[.] | [CallBack]                                                                  ┃
     ┗━━━> _ErrorEvent[0]     ║ _IoDispatcher ━━━┫  _ReadEvents[0]|━║                                           ┃                      ┃   _ClusterNotifiers[N] | [CallBack]         ┏>_EncodedEvents[0]|║                                    ┃
     ┏━━━> _ErrorEvent[3]     ║                  ┃  _ReadEvents[.]|━║ _DecodedDispatcher ━┳━━> _ClusterDecoded ━┻━> ║_ClusterProcessor ╋━> _ClusterWriteEvent ━━>║ _WriteDispatcher ━┫ _EncodedEvents[.]|║ _EncodedProcessor┳━━> _ErrorEvent ━┛
     ┃┏━━> _ErrorEvent[2]     ║                  ┃  _ReadEvents[N]|━║                     ┗━━> _ErrorEvent ━━━━━┓                      ┗━> _ErrorEvent ━━┓       ║                   ┃ _EncodedEvents[M]|║                  ┗━━>║ [Event Done]
     ┃┃┏━> _ErrorEvent[1]     ║                  ┗> _WroteBuffer  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━━━━║                   ┗>_ErrorEvent ━┓
     ┃┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                        ┃                                          ┃
     ┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                          ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                                                                                                                                                                                          ┃
    */
    @SuppressWarnings("unchecked")
    public <T extends IStorage> void build(ClusterManager<C> manager,
                                           IEncryptHandler encryptHandler,
                                           INotifyCustom notifyCustom,
                                           IClusterCustom<C,
                                                          T> clusterCustom)
    {
        final RingBuffer<QEvent> _WroteEvent = createPipelineYield(_AioQueuePower + 1);
        final RingBuffer<QEvent> _ClusterIoEvent = createPipelineYield(_ClusterPower);
        final RingBuffer<QEvent>[] _ErrorEvents = new RingBuffer[4];
        final RingBuffer<QEvent>[] _DispatchIo = new RingBuffer[_ClusterIoCount + _ErrorEvents.length + 1];
        final RingBuffer<QEvent>[] _ReadEvents = new RingBuffer[_DecoderCount];
        final SequenceBarrier[] _ReadBarriers = new SequenceBarrier[_ReadEvents.length];
        final SequenceBarrier[] _DispatchIoBarriers = new SequenceBarrier[_DispatchIo.length];
        final SequenceBarrier[] _ErrorBarriers = new SequenceBarrier[_ErrorEvents.length];
        Arrays.setAll(_ErrorEvents, slot -> createPipelineYield(_ErrorPower));
        Arrays.setAll(_ErrorBarriers, slot -> _ErrorEvents[slot].newBarrier());
        IoUtil.addArray(_AioProducerEvents, _DispatchIo, _ClusterLocalCloseEvent);
        IoUtil.addArray(_ErrorEvents, _DispatchIo, _ClusterIoCount + 1);
        IoUtil.addArray(_AioProducerBarriers, _DispatchIoBarriers, _ClusterLocalCloseEvent.newBarrier());
        IoUtil.addArray(_ErrorBarriers, _DispatchIoBarriers, _ClusterIoCount + 1);
        final BatchEventProcessor<QEvent>[] _DecodeProcessors = new BatchEventProcessor[_DecoderCount];
        Arrays.setAll(_ReadEvents, slot -> createPipelineLite(_AioQueuePower));
        Arrays.setAll(_ReadBarriers, slot -> _ReadEvents[slot].newBarrier());
        Arrays.setAll(_DecodeProcessors,
                      slot -> new BatchEventProcessor<>(_ReadEvents[slot],
                                                        _ReadBarriers[slot],
                                                        new DecodeHandler<>(encryptHandler)));
        final RingBuffer<QEvent>[] _ClusterNotifiers = new RingBuffer[_LogicCount + 1];
        final SequenceBarrier[] _ClusterNotifyBarriers = new SequenceBarrier[_ClusterNotifiers.length];
        final BatchEventProcessor<QEvent>[] _ClusterNotifyProcessors = new BatchEventProcessor[_ClusterNotifiers.length];
        Arrays.setAll(_ClusterNotifiers, slot -> createPipelineLite(_ClusterPower));
        Arrays.setAll(_ClusterNotifyBarriers, slot -> _ClusterNotifiers[slot].newBarrier());
        Arrays.setAll(_ClusterNotifyProcessors,
                      slot -> new BatchEventProcessor<>(_ClusterNotifiers[slot],
                                                        _ClusterNotifyBarriers[slot],
                                                        new NotifyHandler()));
        for (int i = 0, size = _ClusterNotifiers.length; i < size; i++) {
            _ClusterNotifiers[i].addGatingSequences(_ClusterNotifyProcessors[i].getSequence());
        }
        final RingBuffer<QEvent> _ClusterDecoded = createPipelineLite(_ClusterPower);
        final RingBuffer<QEvent>[] _ClusterEvents = new RingBuffer[] { _ClusterIoEvent,
                                                                       _ClusterDecoded,
                                                                       _ConsensusApiEvent,
                                                                       _ConsensusEvent };
        final SequenceBarrier[] _ClusterBarriers = new SequenceBarrier[] { _ClusterIoEvent.newBarrier(),
                                                                           _ClusterDecoded.newBarrier(),
                                                                           _ConsensusApiEvent.newBarrier(),
                                                                           _ConsensusEvent.newBarrier() };
        final MultiBufferBatchEventProcessor<QEvent> _ClusterProcessor = new MultiBufferBatchEventProcessor<>(_ClusterEvents,
                                                                                                              _ClusterBarriers,
                                                                                                              new MappingHandler<>("CONSENSUS",
                                                                                                                                   manager,
                                                                                                                                   _ErrorEvents[0],
                                                                                                                                   _ClusterWriteEvent,
                                                                                                                                   _ClusterNotifiers,
                                                                                                                                   notifyCustom,
                                                                                                                                   clusterCustom));
        _ClusterProcessor.setThreadName("ClusterProcessor");
        for (int i = 0, size = _ClusterEvents.length; i < size; i++) {
            _ClusterEvents[i].addGatingSequences(_ClusterProcessor.getSequences()[i]);
        }
        final MultiBufferBatchEventProcessor<QEvent> _IoDispatcher = new MultiBufferBatchEventProcessor<>(_DispatchIo,
                                                                                                          _DispatchIoBarriers,
                                                                                                          new IoDispatcher<>(_ClusterIoEvent,
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
                                                                                                               new DecodedDispatcher<>(_ClusterDecoded,
                                                                                                                                       _ErrorEvents[1]));
        _DecodedDispatcher.setThreadName("DecodedDispatcher");
        for (int i = 0; i < _DecoderCount; i++) {
            _ReadEvents[i].addGatingSequences(_DecodedDispatcher.getSequences()[i]);
        }
        /* wait to send */
        final RingBuffer<QEvent>[] _SendEvents = new RingBuffer[] { _ClusterWriteEvent,
                                                                    _WroteEvent };
        final SequenceBarrier[] _SendBarriers = new SequenceBarrier[_SendEvents.length];
        Arrays.setAll(_SendBarriers, slot -> _SendEvents[slot].newBarrier());
        final RingBuffer<QEvent>[] _WriteEvents = new RingBuffer[_EncoderCount];
        Arrays.setAll(_WriteEvents, slot -> createPipelineLite(_AioQueuePower));
        final MultiBufferBatchEventProcessor<QEvent> _WriteDispatcher = new MultiBufferBatchEventProcessor<>(_SendEvents,
                                                                                                             _SendBarriers,
                                                                                                             new WriteDispatcher<>(_ErrorEvents[2],
                                                                                                                                   _WriteEvents));
        _WriteDispatcher.setThreadName("WriteDispatcher");
        for (int i = 0, size = _SendEvents.length; i < size; i++) {
            _SendEvents[i].addGatingSequences(_WriteDispatcher.getSequences()[i]);
        }
        final BatchEventProcessor<QEvent>[] _EncodeProcessors = new BatchEventProcessor[_EncoderCount];
        Arrays.setAll(_EncodeProcessors,
                      slot -> new BatchEventProcessor<>(_WriteEvents[slot],
                                                        _WriteEvents[slot].newBarrier(),
                                                        new EncodeHandler<>()));
        final RingBuffer<QEvent>[] _EncodedEvents = new RingBuffer[_EncoderCount];
        IoUtil.addArray(_WriteEvents, _EncodedEvents);
        final SequenceBarrier[] _EncodedBarriers = new SequenceBarrier[_EncodedEvents.length];
        Arrays.setAll(_EncodedBarriers, slot -> _EncodedEvents[slot].newBarrier(_EncodeProcessors[slot].getSequence()));
        final MultiBufferBatchEventProcessor<QEvent> _EncodedProcessor = new MultiBufferBatchEventProcessor<>(_EncodedEvents,
                                                                                                              _EncodedBarriers,
                                                                                                              new EncodedHandler<>(_ErrorEvents[3]));
        _EncodedProcessor.setThreadName("EncodedProcessor");
        for (int i = 0; i < _EncoderCount; i++) {
            _EncodedEvents[i].addGatingSequences(_EncodedProcessor.getSequences()[i]);
        }
        /*-------------------------------------------------------------------------------------------------------------------------------------*/
        submit(_IoDispatcher);
        Arrays.stream(_DecodeProcessors)
              .forEach(this::submit);
        submit(_ClusterProcessor);
        Arrays.stream(_ClusterNotifyProcessors)
              .forEach(this::submit);
        submit(_DecodedDispatcher);
        submit(_WriteDispatcher);
        Arrays.stream(_EncodeProcessors)
              .forEach(this::submit);
        submit(_EncodedProcessor);
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
    public ReentrantLock getLock(ISession<C> session, IOperator.Type type)
    {
        switch (type)
        {
            case CLUSTER_LOCAL:
                return _ClusterLock;
            case CONSENSUS:
                return _ConsensusLock;
            default:
                throw new IllegalArgumentException(String.format("error type:%s", type));
        }
    }

    @Override
    public RingBuffer<QEvent> getPublisher(ISession<C> session, IOperator.Type type)
    {
        switch (type)
        {
            case CONSENSUS:
                return _ConsensusApiEvent;
            case EXTERNAL:
                return _ConsensusEvent;
            default:
                throw new IllegalArgumentException(String.format("get publisher type error:%s ", type.name()));
        }
    }

    @Override
    public RingBuffer<QEvent> getCloser(ISession<C> session, IOperator.Type type)
    {
        switch (type)
        {
            case CLUSTER_LOCAL:
            case CONSENSUS:
                return _ClusterLocalCloseEvent;
            default:
                throw new IllegalArgumentException(String.format("get closer type error:%s ", type.name()));
        }
    }
}
