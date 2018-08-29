/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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

package com.tgx.z.queen.io.core.executor;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.tgx.z.config.Config;
import com.tgx.z.queen.base.db.inf.IStorage;
import com.tgx.z.queen.base.log.Logger;
import com.tgx.z.queen.base.util.IoUtil;
import com.tgx.z.queen.event.handler.DecodeHandler;
import com.tgx.z.queen.event.handler.EncodeHandler;
import com.tgx.z.queen.event.handler.EncodedHandler;
import com.tgx.z.queen.event.handler.client.ClientIoDispatcher;
import com.tgx.z.queen.event.handler.client.ClientLinkHandler;
import com.tgx.z.queen.event.handler.client.ClientWriteDispatcher;
import com.tgx.z.queen.event.inf.IOperator.Type;
import com.tgx.z.queen.event.operator.OperatorHolder;
import com.tgx.z.queen.event.processor.MultiBufferBatchEventProcessor;
import com.tgx.z.queen.event.processor.QEvent;
import com.tgx.z.queen.io.core.async.socket.AioWorker;
import com.tgx.z.queen.io.core.inf.ICommand;
import com.tgx.z.queen.io.core.inf.ISession;

public class ClientCore<E extends IStorage>
        extends
        ThreadPoolExecutor

{
    private static Logger            _Log                 = Logger.getLogger(ClientCore.class.getName());
    private static Config            _Config              = new Config().load(getConfigName());
    private final RingBuffer<QEvent> _AioProducerEvent;
    private final RingBuffer<QEvent> _BizLocalCloseEvent;
    private final RingBuffer<QEvent> _BizLocalSendEvent;
    private final ThreadFactory      _WorkerThreadFactory = new ThreadFactory()
                                                          {
                                                              int count;

                                                              @Override
                                                              public Thread newThread(Runnable r) {
                                                                  return new AioWorker(r,
                                                                                       String.format("AioWorker.client.%d", count++),
                                                                                       _AioProducerEvent);
                                                              }
                                                          };
    private final ReentrantLock      _LocalLock           = new ReentrantLock();

    public ClientCore() {
        super(poolSize(), poolSize(), 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        _AioProducerEvent = createPipelineYield(6);
        _BizLocalCloseEvent = createPipelineLite(5);
        _BizLocalSendEvent = createPipelineLite(5);
    }

    /*  ║ barrier, ━> publish event, ━━ pipeline, | handle event
    
                                          ━━> _LocalSend    ║
     ━━> _AioProducerEvent ║               ┏> _LinkIoEvent| ║
         _BizLocalClose    ║_IoDispatcher━━┫  _ReadEvent  ||║_WriteDispatcher┏>_EncodedEvent|║━┓
    ┏━━> _ErrorEvent[2]    ║               ┃  _WroteBuffer  ║                ┗>_ErrorEvent━┓   ┃
    ┃┏━> _ErrorEvent[1]    ║               ┗> _ErrorEvent━┓                                ┃   ┃
    ┃┃┏> _ErrorEvent[0]    ║                              ┃                                ┃   ┃
    ┃┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                ┃   ┃
    ┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛   ┃
    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    */
    @SuppressWarnings("unchecked")
    public void build(final EventHandler<QEvent> _LogicHandler) {
        final RingBuffer<QEvent> _WroteEvent = createPipelineYield(7);
        final RingBuffer<QEvent> _LinkIoEvent = createPipelineLite(2);
        final RingBuffer<QEvent>[] _ErrorEvents = new RingBuffer[3];
        final RingBuffer<QEvent>[] _DispatchIo = new RingBuffer[5];
        final RingBuffer<QEvent> _ReadAndLogicEvent = createPipelineLite(6);
        final RingBuffer<QEvent> _EncodeEvent = createPipelineLite(7);
        final SequenceBarrier[] _DispatchIoBarriers = new SequenceBarrier[_DispatchIo.length];
        final SequenceBarrier[] _ErrorBarriers = new SequenceBarrier[_ErrorEvents.length];
        Arrays.setAll(_ErrorEvents, slot -> createPipelineLite(5));
        Arrays.setAll(_ErrorBarriers, slot -> _ErrorEvents[slot].newBarrier());
        IoUtil.addArray(_ErrorEvents, _DispatchIo, _AioProducerEvent, _BizLocalCloseEvent);
        IoUtil.addArray(_ErrorBarriers, _DispatchIoBarriers, _AioProducerEvent.newBarrier(), _BizLocalCloseEvent.newBarrier());
        final MultiBufferBatchEventProcessor<QEvent> _IoDispatcher = new MultiBufferBatchEventProcessor<>(_DispatchIo,
                                                                                                          _DispatchIoBarriers,
                                                                                                          new ClientIoDispatcher(_LinkIoEvent,
                                                                                                                                 _ReadAndLogicEvent,
                                                                                                                                 _WroteEvent,
                                                                                                                                 _ErrorEvents[0]));
        _IoDispatcher.setThreadName("IoDispatcher");
        for (int i = 0, size = _DispatchIo.length; i < size; i++)
            _DispatchIo[i].addGatingSequences(_IoDispatcher.getSequences()[i]);
        final BatchEventProcessor<QEvent> _DecodeProcessor = new BatchEventProcessor<>(_ReadAndLogicEvent,
                                                                                       _ReadAndLogicEvent.newBarrier(),
                                                                                       new DecodeHandler());
        /* 相对 server core 做了精简，decode 错误将在 logic processor 中按照 Ignore 进行处理
           最终被
        */
        final BatchEventProcessor<QEvent> _LogicProcessor = new BatchEventProcessor<>(_ReadAndLogicEvent,
                                                                                      _ReadAndLogicEvent.newBarrier(_DecodeProcessor.getSequence()),
                                                                                      _LogicHandler);
        final BatchEventProcessor<QEvent> _LinkProcessor = new BatchEventProcessor<>(_LinkIoEvent,
                                                                                     _LinkIoEvent.newBarrier(),
                                                                                     new ClientLinkHandler());
        final RingBuffer<QEvent>[] _SendEvents = new RingBuffer[] { _BizLocalSendEvent,
                                                                    _LinkIoEvent,
                                                                    _ReadAndLogicEvent,
                                                                    _WroteEvent };
        final SequenceBarrier[] _SendBarriers = new SequenceBarrier[] { _BizLocalSendEvent.newBarrier(),
                                                                        _LinkIoEvent.newBarrier(_LinkProcessor.getSequence()),
                                                                        _ReadAndLogicEvent.newBarrier(_LogicProcessor.getSequence()),
                                                                        _WroteEvent.newBarrier() };
        final MultiBufferBatchEventProcessor<QEvent> _WriteDispatcher = new MultiBufferBatchEventProcessor<>(_SendEvents,
                                                                                                             _SendBarriers,
                                                                                                             new ClientWriteDispatcher(_ErrorEvents[1],
                                                                                                                                       _EncodeEvent));
        _WriteDispatcher.setThreadName("WriteDispatcher");
        for (int i = 0, size = _SendEvents.length; i < size; i++)
            _SendEvents[i].addGatingSequences(_WriteDispatcher.getSequences()[i]);
        final BatchEventProcessor<QEvent> _EncodeProcessor = new BatchEventProcessor<>(_EncodeEvent,
                                                                                       _EncodeEvent.newBarrier(),
                                                                                       new EncodeHandler());
        final BatchEventProcessor<QEvent> _EncodedProcessor = new BatchEventProcessor<>(_EncodeEvent,
                                                                                        _EncodeEvent.newBarrier(_EncodeProcessor.getSequence()),
                                                                                        new EncodedHandler(_ErrorEvents[2]));

        _EncodeEvent.addGatingSequences(_EncodedProcessor.getSequence());
        /* ---------------------------------------------------------------------------------------------------------------- */
        submit(_IoDispatcher);
        submit(_DecodeProcessor);
        submit(_LinkProcessor);
        submit(_LogicProcessor);
        submit(_WriteDispatcher);
        submit(_EncodeProcessor);
        submit(_EncodedProcessor);
    }

    private RingBuffer<QEvent> createPipeline(int power, WaitStrategy waitStrategy) {
        return RingBuffer.createSingleProducer(QEvent.EVENT_FACTORY, 1 << power, waitStrategy);
    }

    private RingBuffer<QEvent> createPipelineYield(int power) {
        return createPipeline(power, new YieldingWaitStrategy());
    }

    private RingBuffer<QEvent> createPipelineLite(int power) {
        return createPipeline(power, new LiteBlockingWaitStrategy());
    }

    private static int poolSize() {
        return 1// aioDispatch
               + 1// link
               + 1// read-decode
               + 1// logic-
               + 1// write-dispatcher
               + 1// write-encode
               + 1// write-end
        ;
    }

    private static String getConfigName() {
        return "ClientPipeline";
    }

    private static String getConfigGroup() {
        return "pipeline";
    }

    public ThreadFactory getWorkerThreadFactory() {
        return _WorkerThreadFactory;
    }

    public boolean localSend(ICommand toSend, ISession session) {
        Objects.requireNonNull(toSend);
        Objects.requireNonNull(session);
        if (_LocalLock.tryLock()) try {
            long sequence = _BizLocalSendEvent.next();
            try {
                QEvent event = _BizLocalSendEvent.get(sequence);
                event.produce(Type.LOCAL,
                              toSend,
                              session,
                              session.getMode()
                                     .getOutOperator());
                return true;
            }
            finally {
                _BizLocalSendEvent.publish(sequence);
            }
        }
        finally {
            _LocalLock.unlock();
        }
        return false;
    }

    public void close(ISession session) {
        Objects.requireNonNull(session);
        _LocalLock.lock();
        try {
            long sequence = _BizLocalCloseEvent.next();
            try {
                QEvent event = _BizLocalCloseEvent.get(sequence);
                event.produce(Type.CLOSE, null, session, OperatorHolder.CLOSE_OPERATOR());
            }
            finally {
                _BizLocalCloseEvent.publish(sequence);
            }
        }
        finally {
            _LocalLock.unlock();
        }
    }

}
