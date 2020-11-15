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
import java.util.concurrent.locks.ReentrantLock;

import com.isahl.chess.king.base.disruptor.MultiBufferBatchEventProcessor;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.config.IMixConfig;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.event.handler.DecodeHandler;
import com.isahl.chess.queen.event.handler.EncodeHandler;
import com.isahl.chess.queen.event.handler.EncodedHandler;
import com.isahl.chess.queen.event.handler.WriteDispatcher;
import com.isahl.chess.queen.event.handler.cluster.IClusterCustom;
import com.isahl.chess.queen.event.handler.mix.ILinkCustom;
import com.isahl.chess.queen.event.handler.mix.ILogicHandler;
import com.isahl.chess.queen.event.handler.mix.MixDecodedDispatcher;
import com.isahl.chess.queen.event.handler.mix.MixIoDispatcher;
import com.isahl.chess.queen.event.handler.mix.MixMappingHandler;
import com.isahl.chess.queen.event.inf.IOperator;
import com.isahl.chess.queen.event.processor.QEvent;
import com.isahl.chess.queen.io.core.async.AioWorker;
import com.isahl.chess.queen.io.core.manager.MixManager;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;

/**
 * @author william.d.zk
 */
public class ServerCore
        extends
        ThreadPoolExecutor
        implements
        IBizCore,
        ILocalPublisher
{
    protected Logger _Logger = Logger.getLogger("io.queen.core." + getClass().getSimpleName());

    private final int _DecoderCount;
    private final int _EncoderCount;
    private final int _LogicCount;
    private final int _BizIoCount;
    private final int _ClusterIoCount;
    private final int _AioQueueSize;
    private final int _ClusterQueueSize;
    private final int _LinkQueueSize;
    private final int _LogicQueueSize;
    private final int _ErrorQueueSize;

    private final RingBuffer<QEvent>[] _AioProducerEvents;
    private final SequenceBarrier[]    _AioProducerBarriers;
    private final RingBuffer<QEvent>   _ClusterLocalCloseEvent;
    private final RingBuffer<QEvent>   _BizLocalCloseEvent;
    private final RingBuffer<QEvent>   _ClusterLocalSendEvent;
    private final RingBuffer<QEvent>   _BizLocalSendEvent;
    private final RingBuffer<QEvent>   _ClusterWriteEvent;
    private final RingBuffer<QEvent>   _LinkWriteEvent;

    /**
     * 用于选举功能的处理pipeline，用于local timer 和 cluster segment log 处理结果向集群中其他节点发送
     * 选举结果都在 cluster processor 中统一由集群处理逻辑执行。
     */
    private final RingBuffer<QEvent> _ConsensusEvent;
    private final RingBuffer<QEvent> _ConsensusApiEvent;

    /**
     * 集群一致性处理结果与Local Logic的桥梁，用于业务一致性数据向集群写入后，集群向业务系统反馈执行结果的
     * cluster -> biz 层投递
     */
    private final RingBuffer<QEvent>                        _ClusterEvent;
    private final RingBuffer<QEvent>                        _NotifyEvent;
    private final ConcurrentLinkedQueue<RingBuffer<QEvent>> _AioCacheConcurrentQueue;
    private final ConcurrentLinkedQueue<RingBuffer<QEvent>> _ClusterCacheConcurrentQueue;

    private final ThreadFactory _WorkerThreadFactory = new ThreadFactory()
    {
        int count;

        @Override
        public Thread newThread(Runnable r)
        {
            return new AioWorker(r,
                                 String.format("AioWorker.server.%d", count++),
                                 _AioCacheConcurrentQueue::offer,
                                 _AioCacheConcurrentQueue.poll());
        }
    };

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

    private final ReentrantLock      _LocalLock        = new ReentrantLock();
    private final ReentrantLock      _ClusterLock      = new ReentrantLock();
    private final ReentrantLock      _ConsensusLock    = new ReentrantLock();
    private final ReentrantLock      _ConsensusApiLock = new ReentrantLock();
    private AsynchronousChannelGroup mServiceChannelGroup;
    private AsynchronousChannelGroup mClusterChannelGroup;

    @SuppressWarnings("unchecked")
    public ServerCore(IMixConfig config)
    {
        super(config.getPoolSize(), config.getPoolSize(), 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        _LogicCount = 1 << config.getLogicCountPower();
        _DecoderCount = 1 << config.getDecoderCountPower();
        _EncoderCount = 1 << config.getEncoderCountPower();
        _BizIoCount = 1 << config.getBizIoCountPower();
        _ClusterIoCount = 1 << config.getClusterIoCountPower();
        _AioQueueSize = 1 << config.getAioQueueSizePower();
        _ClusterQueueSize = 1 << config.getClusterQueueSizePower();
        _LinkQueueSize = 1 << config.getLinkQueueSizePower();
        _LogicQueueSize = 1 << config.getLogicQueueSizePower();
        _ErrorQueueSize = 1 << config.getErrorQueueSizePower();
        final int _CloserQueueSize = 1 << config.getCloserQueueSizePower();
        final int _LocalQueueSize = 1 << config.getLocalQueueSizePower();
        /* Aio event producer */
        _AioProducerEvents = new RingBuffer[_BizIoCount + _ClusterIoCount];
        _AioProducerBarriers = new SequenceBarrier[_AioProducerEvents.length];
        /* Aio worker cache */
        _AioCacheConcurrentQueue = new ConcurrentLinkedQueue<>();
        /* Cluster worker cache */
        _ClusterCacheConcurrentQueue = new ConcurrentLinkedQueue<>();
        Arrays.setAll(_AioProducerEvents, slot ->
        {
            RingBuffer<QEvent> rb = createPipelineYield(_AioQueueSize);
            if (!(slot < _BizIoCount ? _AioCacheConcurrentQueue
                                     : _ClusterCacheConcurrentQueue).offer(rb))
            {
                _Logger.warning(String.format("%s cache queue offer failed :%d",
                                              slot < _BizIoCount ? "biz io"
                                                                 : "cluster io",
                                              slot));
            }
            return rb;
        });
        Arrays.setAll(_AioProducerBarriers, slot -> _AioProducerEvents[slot].newBarrier());

        _ClusterLocalCloseEvent = createPipelineLite(_CloserQueueSize);
        _ClusterLocalSendEvent = createPipelineLite(_ClusterQueueSize);
        _ClusterWriteEvent = createPipelineYield(_ClusterQueueSize);

        _BizLocalCloseEvent = createPipelineLite(_CloserQueueSize);
        _BizLocalSendEvent = createPipelineLite(_LocalQueueSize);
        _LinkWriteEvent = createPipelineYield(_LinkQueueSize << 1);

        _ConsensusEvent = createPipelineYield(_ClusterQueueSize);
        _ConsensusApiEvent = createPipelineLite(_ClusterQueueSize);
        _ClusterEvent = createPipelineYield(_ClusterQueueSize);
        _NotifyEvent = createPipelineYield(_ClusterQueueSize);
    }

    /* @formatter:off
     * ║ barrier, ━> publish event, ━━ pipeline, | event handler
    
     * ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     * ┃                                                                                                                       ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓ ┃
     * ┃                                                                                                                       ┃       ║                 ┏━>_ClusterEvent   ━>┛ ┃
     * ┃                                                                                                                       ┃    ┏>━║_LinkProcessor━━━╋━>_ErrorEvent     ━>━━┛
     * ┃                                                                               Api   ━━> _ConsensusApiEvent ━━━━━━━━━━━╋━━━━┫  ║                 ┗━>_LinkWriteEvent ━━━>━║                 ┏━>_EncodedEvents[0]{_EncoderProcessors[0]}|║
     * ┃  ━> _AioProducerEvents━║                                                      Timer ━━> _ConsensusEvent    ━━━━━━━━━━━┫    ┣━━<━━━━━<━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓  ║                 ┃  _EncodedEvents[1]{_EncoderProcessors[1]}|║
     * ┃  ━> _ClusterLocalClose━║                ┏>_LinkIoEvent    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━┫                ━━━> _BizLocalSendEvent ━╋>━║_WriteDispatcher━┫  _EncodedEvents[2]{_EncoderProcessors[2]}|║_EncodedProcessor┳━━>║[Event Done]
     * ┃  ━> _BizLocalClose    ━║                ┃ _ClusterIoEvent ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫    ┃  ║                 ┏━>_NotifyEvent ━━━━━┛  ║                 ┃  _EncodedEvents[.]{_EncoderProcessors[.]}|║                 ┗━━>_ErrorEvent━┓
     * ┗━━━> _ErrorEvent[0]    ━║_IoDispatcher ━━┫ _ReadEvents[0]{_DecodeProcessors[0]}|━║                   ┏>_ClusterDecoded━┻━━>━╋>━║_ClusterProcessor╋━>_ClusterWriteEvent━>━║                 ┃  _EncodedEvents[M]{_EncoderProcessors[M]}|║                                 ┃
     * ┏━━━> _ErrorEvent[4]    ━║                ┃ _ReadEvents[1]{_DecodeProcessors[1]}|━║_DecodedDispatcher━┫ _LinkDecoded   ━━━━━━┛  ║                 ┗━>_ErrorEvent ━┓       ║                 ┗━>_ErrorEvent━┓                                                              ┃
     * ┃┏━━> _ErrorEvent[3]    ━║                ┃ _ReadEvents[.]{_DecodeProcessors[.]}|━║                   ┃ _Logic         ━━━━━━━━━━━━━━━━━━━━━{_LogicProcessors}|━━━╋━━━━━>━║                                ┃                                                              ┃
     * ┃┃┏━> _ErrorEvent[1]    ━║                ┃ _ReadEvents[N]{_DecodeProcessors[N]}|━║                   ┗>_ErrorEvent    ━━━━━━┓     ━━━> _ClusterLocalSendEvent ━━━╋━━━━━>━║                                ┃                                                              ┃
     * ┃┃┃┏> _ErrorEvent[2]    ━║                ┗>_WroteBuffer  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━━>━║                                ┃                                                              ┃
     * ┃┃┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                    ┃                                        ┃                                                              ┃
     * ┃┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                        ┃                                                              ┃
     * ┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                                              ┃
     * ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    
     * 每一个 Dispatcher 都需要将之前发生的错误信息转换为 session close 事件传递回 IoBarrier，从而将 session close 事件归并到
     * _LinkIoEvent|_ClusterIoEvent 处理器上进行集中处理。
     * (.*)IoEvent 只处理 Connected | Close 事件，所以平行需要一个_ErrorEvent 接受 IoDispatcher 获取的
     * READ_WRITE_DECODE_ENCODE_ENCRYPT_TIMEOUT_INTERRUPT_CANCEL等错误转换为
     * Close operator
     * @formatter:on
     */

    @SuppressWarnings("unchecked")
    public <T extends IStorage> void build(MixManager manager,
                                           ILogicHandler logicHandler,
                                           ILinkCustom linkCustom,
                                           IClusterCustom<T> clusterCustom)
    {
        final RingBuffer<QEvent> _WroteEvent = createPipelineYield(_AioQueueSize << 1);
        final RingBuffer<QEvent> _LinkIoEvent = createPipelineYield(_LinkQueueSize);
        final RingBuffer<QEvent> _ClusterIoEvent = createPipelineYield(_ClusterQueueSize);
        final RingBuffer<QEvent>[] _ErrorEvents = new RingBuffer[5];
        final RingBuffer<QEvent>[] _DispatchIo = new RingBuffer[_BizIoCount
                                                                + _ClusterIoCount
                                                                + _ErrorEvents.length
                                                                + 2];
        final RingBuffer<QEvent>[] _ReadEvents = new RingBuffer[_DecoderCount];
        final RingBuffer<QEvent>[] _LogicEvents = new RingBuffer[_LogicCount];
        final SequenceBarrier[] _ReadBarriers = new SequenceBarrier[_ReadEvents.length];
        final SequenceBarrier[] _LogicBarriers = new SequenceBarrier[_LogicEvents.length];
        final SequenceBarrier[] _DispatchIoBarriers = new SequenceBarrier[_DispatchIo.length];
        final SequenceBarrier[] _ErrorBarriers = new SequenceBarrier[_ErrorEvents.length];
        Arrays.setAll(_ErrorEvents, slot -> createPipelineYield(_ErrorQueueSize));
        /* 所有的_ErrorEvent 都是一次性处理，没有经过任何 processor 进行中间结果转换 */
        Arrays.setAll(_ErrorBarriers, slot -> _ErrorEvents[slot].newBarrier());
        IoUtil.addArray(_AioProducerEvents, _DispatchIo, _ClusterLocalCloseEvent, _BizLocalCloseEvent);
        IoUtil.addArray(_ErrorEvents, _DispatchIo, _BizIoCount + _ClusterIoCount + 2);
        IoUtil.addArray(_AioProducerBarriers,
                        _DispatchIoBarriers,
                        _ClusterLocalCloseEvent.newBarrier(),
                        _BizLocalCloseEvent.newBarrier());
        IoUtil.addArray(_ErrorBarriers, _DispatchIoBarriers, _BizIoCount + _ClusterIoCount + 2);
        /*
         * 负责进行 session 上数据的解码，由于相同的 session 是在同一个read processor 上执行
         * 虽然解决了先后顺序和组包的问题，单 session 巨帧(frame > 64K) 将导致资源利用率不均匀
         * 单帧 处理的数据量限定为 64K，//TODO 分帧传输将被严格受限，后续继续降低单帧容量
         */
        final BatchEventProcessor<QEvent>[] _DecodeProcessors = new BatchEventProcessor[_DecoderCount];
        Arrays.setAll(_ReadEvents, slot -> createPipelineLite(_AioQueueSize));
        Arrays.setAll(_ReadBarriers, slot -> _ReadEvents[slot].newBarrier());
        Arrays.setAll(_DecodeProcessors,
                      slot -> new BatchEventProcessor<>(_ReadEvents[slot], _ReadBarriers[slot], new DecodeHandler()));

        /* 链路处理 */
        /* 所有带有路由规则绑定的数据都需要投递到这个 Pipeline -> _LinkDecoded */
        final RingBuffer<QEvent> _LinkDecoded = createPipelineLite(_LinkQueueSize);
        final RingBuffer<QEvent>[] _LinkEvents = new RingBuffer[]{_LinkIoEvent,
                                                                  _LinkDecoded,
                                                                  _ConsensusApiEvent,
                                                                  _NotifyEvent};
        final SequenceBarrier[] _LinkBarriers = new SequenceBarrier[]{_LinkIoEvent.newBarrier(),
                                                                      _LinkDecoded.newBarrier(),
                                                                      _ConsensusApiEvent.newBarrier(),
                                                                      _NotifyEvent.newBarrier()};
        final MultiBufferBatchEventProcessor<QEvent> _LinkProcessor = new MultiBufferBatchEventProcessor<>(_LinkEvents,
                                                                                                           _LinkBarriers,
                                                                                                           new MixMappingHandler<>("LINK",
                                                                                                                                   manager,
                                                                                                                                   _ErrorEvents[0],
                                                                                                                                   _LinkWriteEvent,
                                                                                                                                   _ClusterEvent,
                                                                                                                                   linkCustom,
                                                                                                                                   clusterCustom));
        _LinkProcessor.setThreadName("LinkProcessor");
        for (int i = 0, size = _LinkEvents.length; i < size; i++) {
            _LinkEvents[i].addGatingSequences(_LinkProcessor.getSequences()[i]);
        }
        /* 集群处理 */
        /* 所有已解码完毕的集群通讯都进入这个 Pipeline -> _ClusterDecoded */
        final RingBuffer<QEvent> _ClusterDecoded = createPipelineLite(_ClusterQueueSize);
        final RingBuffer<QEvent>[] _ClusterEvents = new RingBuffer[]{_ClusterIoEvent,
                                                                     _ClusterDecoded,
                                                                     _ConsensusEvent,
                                                                     _ClusterEvent};
        final SequenceBarrier[] _ClusterBarriers = new SequenceBarrier[]{_ClusterIoEvent.newBarrier(),
                                                                         _ClusterDecoded.newBarrier(),
                                                                         _ConsensusEvent.newBarrier(),
                                                                         _ClusterEvent.newBarrier()};
        final MultiBufferBatchEventProcessor<QEvent> _ClusterProcessor = new MultiBufferBatchEventProcessor<>(_ClusterEvents,
                                                                                                              _ClusterBarriers,
                                                                                                              new MixMappingHandler<>("CONSENSUS",
                                                                                                                                      manager,
                                                                                                                                      _ErrorEvents[1],
                                                                                                                                      _ClusterWriteEvent,
                                                                                                                                      _NotifyEvent,
                                                                                                                                      linkCustom,
                                                                                                                                      clusterCustom));
        _ClusterProcessor.setThreadName("ClusterProcessor");
        for (int i = 0, size = _ClusterEvents.length; i < size; i++) {
            _ClusterEvents[i].addGatingSequences(_ClusterProcessor.getSequences()[i]);
        }
        final MultiBufferBatchEventProcessor<QEvent> _IoDispatcher = new MultiBufferBatchEventProcessor<>(_DispatchIo,
                                                                                                          _DispatchIoBarriers,
                                                                                                          new MixIoDispatcher(_LinkIoEvent,
                                                                                                                              _ClusterIoEvent,
                                                                                                                              _WroteEvent,
                                                                                                                              _ReadEvents));
        _IoDispatcher.setThreadName("IoDispatcher");
        for (int i = 0, size = _DispatchIo.length; i < size; i++) {
            _DispatchIo[i].addGatingSequences(_IoDispatcher.getSequences()[i]);
        }
        /* Decoded dispatch */
        final SequenceBarrier[] _DecodedBarriers = new SequenceBarrier[_DecoderCount];
        Arrays.setAll(_DecodedBarriers, slot -> _ReadEvents[slot].newBarrier(_DecodeProcessors[slot].getSequence()));
        /* 负责最终的业务逻辑的处理 */
        final BatchEventProcessor<QEvent>[] _LogicProcessors = new BatchEventProcessor[_LogicCount];
        Arrays.setAll(_LogicEvents, slot -> createPipelineLite(_LogicQueueSize));
        Arrays.setAll(_LogicBarriers, slot -> _LogicEvents[slot].newBarrier());
        Arrays.setAll(_LogicProcessors,
                      slot -> new BatchEventProcessor<>(_LogicEvents[slot], _LogicBarriers[slot], logicHandler));
        /* Decoded dispatcher 将所有解码完成的结果派发到 _LinkDecoded,_ClusterDecoded,以及_LogicEvents进行逻辑处理 */
        final MultiBufferBatchEventProcessor<QEvent> _DecodedDispatcher = new MultiBufferBatchEventProcessor<>(_ReadEvents,
                                                                                                               _DecodedBarriers,
                                                                                                               new MixDecodedDispatcher(_LinkDecoded,
                                                                                                                                        _ClusterDecoded,
                                                                                                                                        _ErrorEvents[2],
                                                                                                                                        _LogicEvents));
        _DecodedDispatcher.setThreadName("DecodedDispatcher");
        for (int i = 0; i < _DecoderCount; i++) {
            _ReadEvents[i].addGatingSequences(_DecodedDispatcher.getSequences()[i]);
        }
        /* wait to send */
        final RingBuffer<QEvent>[] _SendEvents = new RingBuffer[_LogicCount + 5];
        IoUtil.addArray(_LogicEvents,
                        _SendEvents,
                        _WroteEvent,
                        _ClusterWriteEvent,
                        _ClusterLocalSendEvent,
                        _LinkWriteEvent,
                        _BizLocalSendEvent);

        final SequenceBarrier[] _SendBarriers = new SequenceBarrier[_SendEvents.length];
        Arrays.setAll(_SendBarriers,
                      slot -> slot < _LogicCount ? _SendEvents[slot].newBarrier(_LogicProcessors[slot].getSequence())
                                                 : _SendEvents[slot].newBarrier());
        /* 最终执行 write 操作的 Pipeline */
        final RingBuffer<QEvent>[] _WriteEvents = new RingBuffer[_EncoderCount];
        Arrays.setAll(_WriteEvents, slot -> createPipelineLite(_AioQueueSize));
        /*
         * write dispatch ，将各个上游 pipeline 等待发送的数据均匀的分发到 write-pipeline 中，
         * 需要注意：同一个 session 在 session.size() > 0时将分配到相同的 pipeline 上，从而导致待发消息堆积时线程间负荷不均
         * -------------------------------------------------------------------------------------------------
         * --------
         */
        final MultiBufferBatchEventProcessor<QEvent> _WriteDispatcher = new MultiBufferBatchEventProcessor<>(_SendEvents,
                                                                                                             _SendBarriers,
                                                                                                             new WriteDispatcher(_ErrorEvents[3],
                                                                                                                                 _WriteEvents));
        _WriteDispatcher.setThreadName("WriteDispatcher");
        for (int i = 0, size = _SendEvents.length; i < size; i++) {
            _SendEvents[i].addGatingSequences(_WriteDispatcher.getSequences()[i]);
        }
        /* encode processor */
        final BatchEventProcessor<QEvent>[] _EncodeProcessors = new BatchEventProcessor[_EncoderCount];
        Arrays.setAll(_EncodeProcessors,
                      slot -> new BatchEventProcessor<>(_WriteEvents[slot],
                                                        _WriteEvents[slot].newBarrier(),
                                                        new EncodeHandler()));
        final RingBuffer<QEvent>[] _EncodedEvents = new RingBuffer[_EncoderCount];
        IoUtil.addArray(_WriteEvents, _EncodedEvents);
        final SequenceBarrier[] _EncodedBarriers = new SequenceBarrier[_EncodedEvents.length];
        Arrays.setAll(_EncodedBarriers, slot -> _EncodedEvents[slot].newBarrier(_EncodeProcessors[slot].getSequence()));
        final MultiBufferBatchEventProcessor<QEvent> _EncodedProcessor = new MultiBufferBatchEventProcessor<>(_EncodedEvents,
                                                                                                              _EncodedBarriers,
                                                                                                              new EncodedHandler(_ErrorEvents[4]));
        _EncodedProcessor.setThreadName("EncodedProcessor");
        for (int i = 0; i < _EncoderCount; i++) {
            _EncodedEvents[i].addGatingSequences(_EncodedProcessor.getSequences()[i]);
        }
        /*-------------------------------------------------------------------------------------------------------------------------------------*/
        /* 所有Io事件 都通过这个 Dispatcher 向其他的领域处理器进行分发 */
        submit(_IoDispatcher);
        Arrays.stream(_DecodeProcessors)
              .forEach(this::submit);
        submit(_ClusterProcessor);
        submit(_LinkProcessor);
        Arrays.stream(_LogicProcessors)
              .forEach(this::submit);
        submit(_DecodedDispatcher);
        submit(_WriteDispatcher);
        Arrays.stream(_EncodeProcessors)
              .forEach(this::submit);
        submit(_EncodedProcessor);
        _Logger.info("%s =>>>>>>>>>>> start", getClass().getSimpleName());
    }

    @Override
    public AsynchronousChannelGroup getServiceChannelGroup() throws IOException
    {
        if (mServiceChannelGroup == null) {
            mServiceChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(_BizIoCount, _WorkerThreadFactory);
        }
        return mServiceChannelGroup;

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
    public RingBuffer<QEvent> getCloser(IOperator.Type type)
    {
        return switch (type)
        {
            case BIZ_LOCAL -> _BizLocalCloseEvent;
            case CLUSTER_LOCAL -> _ClusterLocalCloseEvent;
            default -> throw new IllegalArgumentException(String.format("get closer type error:%s ", type.name()));
        };
    }

    @Override
    public ReentrantLock getLock(IOperator.Type type)
    {
        return switch (type)
        {
            case BIZ_LOCAL -> _LocalLock;
            case CLUSTER_LOCAL -> _ClusterLock;
            case CONSENSUS -> _ConsensusApiLock;
            case CLUSTER_TIMER -> _ConsensusLock;
            default -> throw new IllegalArgumentException(String.format("error type:%s", type));
        };
    }

    @Override
    public RingBuffer<QEvent> getPublisher(IOperator.Type type)
    {
        return switch (type)
        {
            case BIZ_LOCAL -> _BizLocalSendEvent;
            case CLUSTER_LOCAL -> _ClusterLocalSendEvent;
            case CONSENSUS -> _ConsensusApiEvent;
            case CLUSTER_TIMER -> _ConsensusEvent;
            default -> throw new IllegalArgumentException(String.format("get publisher type error:%s ", type.name()));
        };
    }

    @Override
    public ReentrantLock getConsensusLock()
    {
        return _ConsensusLock;
    }

    @Override
    public RingBuffer<QEvent> getConsensusEvent()
    {
        return _ConsensusEvent;
    }

    public ReentrantLock getConsensusApiLock()
    {
        return _ConsensusApiLock;
    }
}
