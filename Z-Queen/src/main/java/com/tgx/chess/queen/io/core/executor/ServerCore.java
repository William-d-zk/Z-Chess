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

package com.tgx.chess.queen.io.core.executor;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.tgx.chess.king.base.disruptor.MultiBufferBatchEventProcessor;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.config.IServerConfig;
import com.tgx.chess.queen.event.handler.*;
import com.tgx.chess.queen.event.inf.ICustomLogic;
import com.tgx.chess.queen.event.inf.ILogicHandler;
import com.tgx.chess.queen.event.inf.IOperator.Type;
import com.tgx.chess.queen.event.inf.IPipeEventHandler;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.async.socket.AioWorker;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IEncryptHandler;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.manager.QueenManager;

/**
 * @author william.d.zk
 */
public abstract class ServerCore<C extends IContext<C>>
        extends
        ThreadPoolExecutor
        implements
        ILocalPublisher<C>,
        IPeerCore
{
    private Logger    _Logger = Logger.getLogger(getClass().getName());
    private final int _DecoderCount;
    private final int _EncoderCount;
    private final int _LogicCount;
    private final int _BizIoCount;
    private final int _ClusterIoCount;
    private final int _AioQueuePower;
    private final int _ClusterPower;
    private final int _LocalPower;
    private final int _LinkPower;
    private final int _LogicPower;
    private final int _CloserPower;
    private final int _ErrorPower;

    private final RingBuffer<QEvent>[]                      _AioProducerEvents;
    private final SequenceBarrier[]                         _AioProducerBarriers;
    private final RingBuffer<QEvent>                        _ClusterLocalCloseEvent;
    private final RingBuffer<QEvent>                        _BizLocalCloseEvent;
    private final RingBuffer<QEvent>                        _ClusterLocalSendEvent;
    private final RingBuffer<QEvent>                        _BizLocalSendEvent;
    private final RingBuffer<QEvent>                        _ClusterWriteEvent;
    private final RingBuffer<QEvent>                        _LinkWriteEvent;
    /**
     * 用于选举功能的处理pipeline，用于local timer 和 cluster segment log 处理结果向集群中其他节点发送
     * 选举结果都在 cluster processor 中统一由集群处理逻辑执行。
     */
    private final RingBuffer<QEvent>                        _ConsistentElectEvent;
    /**
     * 集群一致性处理结果与Local Logic的桥梁，用于业务一致性数据向集群写入后，集群向业务系统反馈执行结果的
     * cluster -> biz 层投递
     */
    private final RingBuffer<QEvent>                        _ConsistentTransEvent;
    private final ConcurrentLinkedQueue<RingBuffer<QEvent>> _AioCacheConcurrentQueue;
    private final ConcurrentLinkedQueue<RingBuffer<QEvent>> _ClusterCacheConcurrentQueue;
    private final List<RingBuffer<QEvent>>                  _ClusterIoList        = new ArrayList<>();
    private final ThreadFactory                             _WorkerThreadFactory  = new ThreadFactory()
                                                                                  {
                                                                                      int count;

                                                                                      @Override
                                                                                      public Thread newThread(Runnable r)
                                                                                      {
                                                                                          return new AioWorker(r,
                                                                                                               String.format("AioWorker.server.%d",
                                                                                                                             count++),
                                                                                                               _AioCacheConcurrentQueue::offer,
                                                                                                               _AioCacheConcurrentQueue.poll());
                                                                                      }
                                                                                  };
    private final ThreadFactory                             _ClusterThreadFactory = new ThreadFactory()
                                                                                  {
                                                                                      int count;

                                                                                      @Override
                                                                                      public Thread newThread(Runnable r)
                                                                                      {
                                                                                          return new AioWorker(r,
                                                                                                               String.format("AioWorker.cluster.%d",
                                                                                                                             count++),
                                                                                                               _ClusterCacheConcurrentQueue::offer,
                                                                                                               _ClusterCacheConcurrentQueue.poll());
                                                                                      }
                                                                                  };
    private final TimeWheel                                 _TimeWheel            = new TimeWheel();
    private final ReentrantLock                             _LocalLock            = new ReentrantLock();
    private final Random                                    _Random               = new Random();
    private AsynchronousChannelGroup                        mServiceChannelGroup;
    private AsynchronousChannelGroup                        mClusterChannelGroup;

    @SuppressWarnings("unchecked")
    protected ServerCore(IServerConfig config)
    {
        super(config.getPoolSize(), config.getPoolSize(), 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        _LogicCount = config.getLogicCount();
        _DecoderCount = config.getDecoderCount();
        _EncoderCount = config.getEncoderCount();
        _BizIoCount = config.getBizIoCount();
        _ClusterIoCount = config.getClusterIoCount();
        _AioQueuePower = config.getAioQueuePower();
        _ClusterPower = config.getClusterPower();
        _LinkPower = config.getLinkPower();
        _LocalPower = config.getLocalPower();
        _LogicPower = config.getLogicPower();
        _ErrorPower = config.getErrorPower();
        _CloserPower = config.getCloserPower();
        /* Aio event producer  */
        _AioProducerEvents = new RingBuffer[_BizIoCount + _ClusterIoCount];
        _AioProducerBarriers = new SequenceBarrier[_AioProducerEvents.length];
        /* Aio worker cache */
        _AioCacheConcurrentQueue = new ConcurrentLinkedQueue<>();
        /* Cluster worker cache */
        _ClusterCacheConcurrentQueue = new ConcurrentLinkedQueue<>();
        Arrays.setAll(_AioProducerEvents, slot ->
        {
            RingBuffer<QEvent> rb = createPipelineYield(_AioQueuePower);
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
        _ClusterIoList.addAll(_ClusterCacheConcurrentQueue);
        Arrays.setAll(_AioProducerBarriers, slot -> _AioProducerEvents[slot].newBarrier());

        _ClusterLocalCloseEvent = createPipelineLite(_CloserPower);
        _ClusterLocalSendEvent = createPipelineLite(_ClusterPower);
        _ClusterWriteEvent = createPipelineYield(_ClusterPower);

        _BizLocalCloseEvent = createPipelineLite(_CloserPower);
        _BizLocalSendEvent = createPipelineLite(_LocalPower);
        _LinkWriteEvent = createPipelineYield(_LinkPower);

        _ConsistentElectEvent = createPipelineYield(_ClusterPower);
        _ConsistentTransEvent = createPipelineYield(_ClusterPower);

        //        _TimeWheel.acquire("server timer", new ScheduleHandler<>(10, true));
    }

    /*  ║ barrier, ━> publish event, ━━ pipeline,| mappingHandle event
    ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
    ┃                                                                                                        ║                 ┏━>_ErrorEvent━┛
    ┃┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓                                             ┏━║_LinkProcessor━━━┻━>_LinkWriteEvent  ━━━━║                  
    ┃┃ ━> _AioProducerEvents                                 ┃  ━>_ConsistentTransEvent  ━━━━━━━━━━━━━━━━━━┫ ║              ━━>_BizLocalSendEvent  ━━━━║                ┏>_EncodedEvents[0]|║
    ┃┃ ━> _ClusterLocalClose║                 ┏>_ErrorEvent ━┛  ━>_ConsistentElectEvent  ━━━━━━━━━━━━━┓    ┃                                           ║                ┃ _EncodedEvents[1]|║
    ┃┃ ━> _BizLocalClose    ║                 ┃ _LinkIoEvent    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━┫                                           ║_WriteDispatcher┫ _EncodedEvents[2]|║_EncodedProcessor┳━║[Event Done]
    ┃┗━━> _ErrorEvent[3]    ║                 ┃ _ClusterIoEvent ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫    ┃ ║              ━>_ClusterLocalSendEvent ━━║                ┃ _EncodedEvents[3]|║                 ┗━>_ErrorEvent━┓
    ┗━━━> _ErrorEvent[0]    ║_IoDispatcher━━> ┫ _ReadEvents[0]|━║                  ┏>_ClusterDecoded ━┻━━━━╋━║_ClusterProcessor┳━>_ClusterWriteEvent ━━║                ┃ _EncodedEvents[M]|║                                ┃
    ┏━━━> _ErrorEvent[5]    ║                 ┃ _ReadEvents[1]|━║_DecodedDispatcher┫ _LinkDecoded    ━━━━━━┛ ║                 ┗━>_ErrorEvent━┓        ║                ┗>_ErrorEvent━┓                                      ┃
    ┃┏━━> _ErrorEvent[4]    ║                 ┃ _ReadEvents[2]|━║                  ┃ _Logic          ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━━━━━║                              ┃                                      ┃
    ┃┃┏━> _ErrorEvent[2]    ║                 ┃ _ReadEvents[N]|━║                  ┗>_ErrorEvent     ━━━━━━┓                                  ┃        ║                              ┃                                      ┃
    ┃┃┃┏> _ErrorEvent[2]    ║                 ┗>_WroteBuffer  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━━━━━║                              ┃                                      ┃
    ┃┃┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                  ┃                                       ┃                                      ┃
    ┃┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                       ┃                                      ┃
    ┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                      ┃
    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    
     每一个 Dispatcher 都需要将之前发生的错误信息转换为 session close 事件传递回 IoBarrier，从而将 session close 事件归并到 _LinkIoEvent|_ClusterIoEvent  处理器上进行集中处理。
     (.*)IoEvent 只处理 Connected | Close 事件，所以平行需要一个_ErrorEvent 接受 IoDispatcher 获取的 READ_WRITE_DECODE_ENCODE_ENCRYPT_TIMEOUT_INTERRUPT_CANCEL等错误转换为
     Close operator
     */

    @SuppressWarnings("unchecked")
    public void build(QueenManager<C> manager,
                      IEncryptHandler encryptHandler,
                      ILogicHandler<C> logicHandler,
                      ICustomLogic<C> linkCustom,
                      ICustomLogic<C> clusterCustom)
    {
        final RingBuffer<QEvent> _WroteEvent = createPipelineYield(_AioQueuePower + 1);
        final RingBuffer<QEvent> _LinkIoEvent = createPipelineYield(_LinkPower);
        final RingBuffer<QEvent> _ClusterIoEvent = createPipelineYield(_ClusterPower);
        final RingBuffer<QEvent>[] _ErrorEvents = new RingBuffer[6];
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
        Arrays.setAll(_ErrorEvents, slot -> createPipelineYield(_ErrorPower));
        /* 所有的_ErrorEvent 都是一次性处理，没有经过任何 processor 进行中间结果转换 */
        Arrays.setAll(_ErrorBarriers, slot -> _ErrorEvents[slot].newBarrier());
        IoUtil.addArray(_AioProducerEvents, _DispatchIo, _ClusterLocalCloseEvent, _BizLocalCloseEvent);
        IoUtil.addArray(_ErrorEvents, _DispatchIo, _BizIoCount + _ClusterIoCount + 2);
        IoUtil.addArray(_AioProducerBarriers,
                        _DispatchIoBarriers,
                        _ClusterLocalCloseEvent.newBarrier(),
                        _BizLocalCloseEvent.newBarrier());
        IoUtil.addArray(_ErrorBarriers, _DispatchIoBarriers, _BizIoCount + _ClusterIoCount + 2);
        /* 负责进行 session 上数据的解码，由于相同的 session 是在同一个read processor 上执行
          虽然解决了先后顺序和组包的问题，单 session 巨帧(frame > 64K) 将导致资源利用率不均匀
          单帧 处理的数据量限定为 64K，//TODO 分帧传输将被严格受限，后续继续降低单帧容量
        */
        final BatchEventProcessor<QEvent>[] _DecodeProcessors = new BatchEventProcessor[_DecoderCount];
        Arrays.setAll(_ReadEvents, slot -> createPipelineLite(_AioQueuePower));
        Arrays.setAll(_ReadBarriers, slot -> _ReadEvents[slot].newBarrier());
        Arrays.setAll(_DecodeProcessors,
                      slot -> new BatchEventProcessor<>(_ReadEvents[slot],
                                                        _ReadBarriers[slot],
                                                        new DecodeHandler<>(encryptHandler)));

        /* 链路处理 */
        /* 所有带有路由规则绑定的数据都需要投递到这个 Pipeline -> _LinkDecoded */
        final RingBuffer<QEvent> _LinkDecoded = createPipelineLite(_LinkPower);
        final RingBuffer<QEvent>[] _LinkEvents = new RingBuffer[] { _LinkIoEvent,
                                                                    _LinkDecoded,
                                                                    _ConsistentTransEvent };
        final SequenceBarrier[] _LinkBarriers = new SequenceBarrier[] { _LinkIoEvent.newBarrier(),
                                                                        _LinkDecoded.newBarrier(),
                                                                        _ConsistentTransEvent.newBarrier() };
        final MultiBufferBatchEventProcessor<QEvent> _LinkProcessor = new MultiBufferBatchEventProcessor<>(_LinkEvents,
                                                                                                           _LinkBarriers,
                                                                                                           new MappingHandler<>("LINK",
                                                                                                                                manager,
                                                                                                                                _ErrorEvents[0],
                                                                                                                                _LinkWriteEvent,
                                                                                                                                linkCustom));
        _LinkProcessor.setThreadName("LinkProcessor");
        for (int i = 0, size = _LinkEvents.length; i < size; i++) {
            _LinkEvents[i].addGatingSequences(_LinkProcessor.getSequences()[i]);
        }
        /* 集群处理 */
        /* 所有已解码完毕的集群通讯都进入这个 Pipeline -> _ClusterDecoded */
        final RingBuffer<QEvent> _ClusterDecoded = createPipelineLite(_ClusterPower);
        final RingBuffer<QEvent>[] _ClusterEvents = new RingBuffer[] { _ClusterIoEvent,
                                                                       _ClusterDecoded,
                                                                       _ConsistentElectEvent };
        final SequenceBarrier[] _ClusterBarriers = new SequenceBarrier[] { _ClusterIoEvent.newBarrier(),
                                                                           _ClusterDecoded.newBarrier(),
                                                                           _ConsistentElectEvent.newBarrier() };
        final MultiBufferBatchEventProcessor<QEvent> _ClusterProcessor = new MultiBufferBatchEventProcessor<>(_ClusterEvents,
                                                                                                              _ClusterBarriers,
                                                                                                              new MappingHandler<>("CLUSTER",
                                                                                                                                   manager,
                                                                                                                                   _ErrorEvents[1],
                                                                                                                                   _ClusterWriteEvent,
                                                                                                                                   clusterCustom));
        _ClusterProcessor.setThreadName("ClusterProcessor");
        for (int i = 0, size = _ClusterEvents.length; i < size; i++) {
            _ClusterEvents[i].addGatingSequences(_ClusterProcessor.getSequences()[i]);
        }
        final MultiBufferBatchEventProcessor<QEvent> _IoDispatcher = new MultiBufferBatchEventProcessor<>(_DispatchIo,
                                                                                                          _DispatchIoBarriers,
                                                                                                          new IoDispatcher<>(_LinkIoEvent,
                                                                                                                             _ClusterIoEvent,
                                                                                                                             _WroteEvent,
                                                                                                                             _ErrorEvents[2],
                                                                                                                             _ReadEvents));
        _IoDispatcher.setThreadName("IoDispatcher");
        for (int i = 0, size = _DispatchIo.length; i < size; i++) {
            _DispatchIo[i].addGatingSequences(_IoDispatcher.getSequences()[i]);
        }
        /* Decoded dispatch */
        final SequenceBarrier[] _DecodedBarriers = new SequenceBarrier[_DecoderCount];
        Arrays.setAll(_DecodedBarriers, slot -> _ReadEvents[slot].newBarrier(_DecodeProcessors[slot].getSequence()));
        /* 负责最终的业务逻辑的处理*/
        final BatchEventProcessor<QEvent>[] _LogicProcessors = new BatchEventProcessor[_LogicCount];
        Arrays.setAll(_LogicEvents, slot -> createPipelineLite(_LogicPower));
        Arrays.setAll(_LogicBarriers, slot -> _LogicEvents[slot].newBarrier());
        Arrays.setAll(_LogicProcessors,
                      slot -> new BatchEventProcessor<>(_LogicEvents[slot], _LogicBarriers[slot], logicHandler));
        /* Decoded dispatcher 将所有解码完成的结果派发到 _LinkDecoded,_ClusterDecoded,以及_LogicEvents进行逻辑处理*/
        final MultiBufferBatchEventProcessor<QEvent> _DecodedDispatcher = new MultiBufferBatchEventProcessor<>(_ReadEvents,
                                                                                                               _DecodedBarriers,
                                                                                                               new DecodedDispatcher<>(_LinkDecoded,
                                                                                                                                       _ClusterDecoded,
                                                                                                                                       _ErrorEvents[3],
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
        Arrays.setAll(_WriteEvents, slot -> createPipelineLite(_AioQueuePower));
        /* write dispatch ，将各个上游 pipeline 等待发送的数据均匀的分发到 write-pipeline 中，
           需要注意：同一个 session 在 session.size() > 0时将分配到相同的 pipeline 上，从而导致待发消息堆积时线程间负荷不均
        ---------------------------------------------------------------------------------------------------------         
        */
        final MultiBufferBatchEventProcessor<QEvent> _WriteDispatcher = new MultiBufferBatchEventProcessor<>(_SendEvents,
                                                                                                             _SendBarriers,
                                                                                                             new WriteDispatcher<>(_ErrorEvents[4],
                                                                                                                                   _WriteEvents));
        _WriteDispatcher.setThreadName("WriteDispatcher");
        for (int i = 0, size = _SendEvents.length; i < size; i++) {
            _SendEvents[i].addGatingSequences(_WriteDispatcher.getSequences()[i]);
        }
        /* encode processor*/
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
                                                                                                              new EncodedHandler<>(_ErrorEvents[5]));
        _EncodedProcessor.setThreadName("EncodedProcessor");
        for (int i = 0; i < _EncoderCount; i++) {
            _EncodedEvents[i].addGatingSequences(_EncodedProcessor.getSequences()[i]);
        }
        /*-------------------------------------------------------------------------------------------------------------------------------------*/
        /* 所有Io事件 都通过这个 Dispatcher 向其他的领域处理器进行分发*/
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
    }

    private RingBuffer<QEvent> createPipeline(int power, WaitStrategy waitStrategy)
    {
        return RingBuffer.createSingleProducer(QEvent.EVENT_FACTORY, 1 << power, waitStrategy);
    }

    private RingBuffer<QEvent> createPipelineYield(int power)
    {
        return createPipeline(power, new YieldingWaitStrategy());
    }

    private RingBuffer<QEvent> createPipelineLite(int power)
    {
        return createPipeline(power, new LiteBlockingWaitStrategy());
    }

    public int getServerCount()
    {
        return _BizIoCount;
    }

    public int getClusterCount()
    {
        return _ClusterIoCount;
    }

    public ThreadFactory getWorkerThreadFactory()
    {
        return _WorkerThreadFactory;
    }

    public ThreadFactory getClusterThreadFactory()
    {
        return _ClusterThreadFactory;
    }

    public TimeWheel getTimeWheel()
    {
        return _TimeWheel;
    }

    /*close 一定发生在 linker 处理器中，单线程处理*/
    public void localClose(ISession<C> session)
    {
        long sequence = _BizLocalCloseEvent.next();
        QEvent event = _BizLocalCloseEvent.get(sequence);
        event.produce(Type.CLOSE,
                      new Pair<>(null, session),
                      session.getContext()
                             .getSort()
                             .getCloser());
        _BizLocalCloseEvent.publish(sequence);
    }

    @Override
    public ReentrantLock getLocalLock()
    {
        return _LocalLock;
    }

    protected RingBuffer<QEvent> getBizLocalCloseEvent()
    {
        return _BizLocalCloseEvent;
    }

    protected RingBuffer<QEvent> getClusterLocalCloseEvent()
    {
        return _ClusterLocalCloseEvent;
    }

    public RingBuffer<QEvent> getConsistentResultEvent()
    {
        return _ConsistentTransEvent;
    }

    public RingBuffer<QEvent> getConsistentLocalSendEvent()
    {
        return _ConsistentElectEvent;
    }

    protected RingBuffer<QEvent> getBizLocalSendEvent()
    {
        return _BizLocalSendEvent;
    }

    protected RingBuffer<QEvent> getClusterLocalSendEvent()
    {
        return _ClusterLocalSendEvent;
    }

    public AsynchronousChannelGroup getServiceChannelGroup() throws IOException
    {
        return Objects.nonNull(mServiceChannelGroup) ? mServiceChannelGroup
                                                     : (mServiceChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(getServerCount(),
                                                                                                                            getWorkerThreadFactory()));

    }

    public AsynchronousChannelGroup getClusterChannelGroup() throws IOException
    {
        return Objects.nonNull(mClusterChannelGroup) ? mClusterChannelGroup
                                                     : (mClusterChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(getClusterCount(),
                                                                                                                            getClusterThreadFactory()));
    }

    @Override
    public RingBuffer<QEvent> getConnectPublisher()
    {
        return _ClusterIoList.get(_Random.nextInt(_ClusterIoList.size()));
    }
}
