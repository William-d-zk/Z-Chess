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

package com.isahl.chess.queen.io.core.executor;

import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.disruptor.processor.Z1Processor;
import com.isahl.chess.king.base.disruptor.processor.Z2Processor;
import com.isahl.chess.king.base.schedule.TimeWheel;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.event.QEvent;
import com.isahl.chess.queen.event.handler.EncodeHandler;
import com.isahl.chess.queen.event.handler.EncodedHandler;
import com.isahl.chess.queen.event.handler.client.ClientDecodeHandler;
import com.isahl.chess.queen.event.handler.client.ClientIoDispatcher;
import com.isahl.chess.queen.event.handler.client.ClientLinkHandler;
import com.isahl.chess.queen.event.handler.client.ClientWriteDispatcher;
import com.isahl.chess.queen.event.handler.mix.ILogicHandler;
import com.isahl.chess.queen.io.core.async.AioWorker;
import com.isahl.chess.queen.io.core.inf.IEncryptHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * @author william.d.zk
 */
public class ClientCore
        extends ThreadPoolExecutor
        implements IBizCore,
                   ILocalPublisher

{
    private final RingBuffer<QEvent>[]                      _AioProducerEvents;
    private final RingBuffer<QEvent>                        _BizLocalCloseEvent;
    private final RingBuffer<QEvent>                        _BizLocalSendEvent;
    private final ConcurrentLinkedQueue<RingBuffer<QEvent>> _AioCacheConcurrentQueue;
    private final int                                       _IoCount;
    private final ThreadFactory                             _WorkerThreadFactory = new ThreadFactory()
    {
        int count;

        @Override
        public Thread newThread(Runnable r)
        {
            return new AioWorker(r,
                                 String.format("AioWorker.client.%d", count++),
                                 _AioCacheConcurrentQueue::offer,
                                 _AioCacheConcurrentQueue.poll());
        }
    };
    private final ReentrantLock                             _LocalLock           = new ReentrantLock();
    private final TimeWheel                                 _TimeWheel           = new TimeWheel();

    @SuppressWarnings("unchecked")
    public ClientCore(int ioCount)
    {
        super(poolSize(), poolSize(), 15, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        _AioProducerEvents = new RingBuffer[_IoCount = ioCount];
        Arrays.setAll(_AioProducerEvents, slot->createPipelineYield(1 << 6));
        _AioCacheConcurrentQueue = new ConcurrentLinkedQueue<>(Arrays.asList(_AioProducerEvents));
        _BizLocalCloseEvent = createPipelineLite(1 << 5);
        _BizLocalSendEvent = createPipelineLite(1 << 5);
        // _TimeWheel.acquire("client event", new ScheduleHandler<>(45, true));
    }

    /*@formatter:off
     * ║ barrier, ━> publish event, ━━ pipeline, | handle event

     *                                        ━━> _LocalSend   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━║
     *  ━━> _AioProducerEvents ║               ┏> _LinkIoEvent ━━━━{_LinkProcessor}|━━━━━━━━━━━━━━━━━━━━━━━━║
     *      _BizLocalClose     ║_IoDispatcher━━┫  _ReadEvent   ━━━━{_DecodeProcessor}|━━{_LogicProcessor}|━━║_WriteDispatcher┏>_EncodedEvent━━{_EncodedProcessor}|━┳━║[Event Done]
     * ┏━━> _ErrorEvent[2]     ║               ┃  _WroteBuffer ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━║                ┗>_ErrorEvent━┓                       ┗━>_ErrorEvent━┓
     * ┃┏━> _ErrorEvent[1]     ║               ┗> _ErrorEvent━┓                                                                            ┃                                      ┃
     * ┃┃┏> _ErrorEvent[0]     ║                              ┃                                                                            ┃                                      ┃
     * ┃┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                                                            ┃                                      ┃
     * ┃┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                      ┃
     * ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

     * @formatter:on
     */
    @SuppressWarnings("unchecked")
    public void build(final ILogicHandler.factory factory, Supplier<IEncryptHandler> encryptSupplier)
    {
        final RingBuffer<QEvent> _WroteEvent = createPipelineYield(1 << 7);
        final RingBuffer<QEvent> _LinkIoEvent = createPipelineLite(1 << 2);
        final RingBuffer<QEvent>[] _ErrorEvents = new RingBuffer[3];
        final RingBuffer<QEvent>[] _DispatchIo = new RingBuffer[4 + _IoCount];
        final RingBuffer<QEvent> _ReadAndLogicEvent = createPipelineLite(1 << 6);
        final RingBuffer<QEvent> _EncodeEvent = createPipelineLite(1 << 7);
        final SequenceBarrier[] _DispatchIoBarriers = new SequenceBarrier[_DispatchIo.length];
        final SequenceBarrier[] _ErrorBarriers = new SequenceBarrier[_ErrorEvents.length];
        final SequenceBarrier[] _AioProducerBarriers = new SequenceBarrier[_IoCount];
        Arrays.setAll(_ErrorEvents, slot->createPipelineLite(1 << 5));
        Arrays.setAll(_ErrorBarriers, slot->_ErrorEvents[slot].newBarrier());
        Arrays.setAll(_AioProducerBarriers, slot->_AioProducerEvents[slot].newBarrier());
        IoUtil.addArray(_AioProducerEvents, _DispatchIo, _BizLocalCloseEvent);
        IoUtil.addArray(_AioProducerBarriers, _DispatchIoBarriers, _BizLocalCloseEvent.newBarrier());
        IoUtil.addArray(_ErrorEvents, _DispatchIo, _IoCount + 1);
        IoUtil.addArray(_ErrorBarriers, _DispatchIoBarriers, _IoCount + 1);
        final Z2Processor<QEvent> _IoDispatcher = new Z2Processor<>(_DispatchIo,
                                                                    _DispatchIoBarriers,
                                                                    new ClientIoDispatcher(_LinkIoEvent,
                                                                                           _ReadAndLogicEvent,
                                                                                           _WroteEvent,
                                                                                           _ErrorEvents[0]));
        _IoDispatcher.setThreadName("IoDispatcher");
        for(int i = 0, size = _DispatchIo.length; i < size; i++) {
            _DispatchIo[i].addGatingSequences(_IoDispatcher.getSequences()[i]);
        }
        final Z1Processor<QEvent> _DecodeProcessor = new Z1Processor<>(_ReadAndLogicEvent,
                                                                       _ReadAndLogicEvent.newBarrier(),
                                                                       new ClientDecodeHandler(encryptSupplier.get(),
                                                                                               0));
        /*
         * 相对 server core 做了精简，decode 错误将在 logic processor 中按照 Ignore 进行处理
         * 最终被
         */
        final Z1Processor<QEvent> _LogicProcessor = new Z1Processor<>(_ReadAndLogicEvent,
                                                                      _ReadAndLogicEvent.newBarrier(_DecodeProcessor.getSequence()),
                                                                      factory.create(0));
        final Z1Processor<QEvent> _LinkProcessor = new Z1Processor<>(_LinkIoEvent,
                                                                     _LinkIoEvent.newBarrier(),
                                                                     new ClientLinkHandler());
        final RingBuffer<QEvent>[] _SendEvents = new RingBuffer[]{ _BizLocalSendEvent,
                                                                   _LinkIoEvent,
                                                                   _ReadAndLogicEvent,
                                                                   _WroteEvent
        };
        final SequenceBarrier[] _SendBarriers = new SequenceBarrier[]{ _BizLocalSendEvent.newBarrier(),
                                                                       _LinkIoEvent.newBarrier(_LinkProcessor.getSequence()),
                                                                       _ReadAndLogicEvent.newBarrier(_LogicProcessor.getSequence()),
                                                                       _WroteEvent.newBarrier()
        };
        final Z2Processor<QEvent> _WriteDispatcher = new Z2Processor<>(_SendEvents,
                                                                       _SendBarriers,
                                                                       new ClientWriteDispatcher(_ErrorEvents[1],
                                                                                                 _EncodeEvent));
        _WriteDispatcher.setThreadName("WriteDispatcher");
        for(int i = 0, size = _SendEvents.length; i < size; i++) {
            _SendEvents[i].addGatingSequences(_WriteDispatcher.getSequences()[i]);
        }
        final Z1Processor<QEvent> _EncodeProcessor = new Z1Processor<>(_EncodeEvent,
                                                                       _EncodeEvent.newBarrier(),
                                                                       new EncodeHandler(encryptSupplier.get(), 0));
        final Z1Processor<QEvent> _EncodedProcessor = new Z1Processor<>(_EncodeEvent,
                                                                        _EncodeEvent.newBarrier(_EncodeProcessor.getSequence()),
                                                                        new EncodedHandler(_ErrorEvents[2]));

        _EncodeEvent.addGatingSequences(_EncodedProcessor.getSequence());
        /* -------------------------------------------------------------------------------------------------*/

        submit(_IoDispatcher);
        submit(_DecodeProcessor);
        submit(_LinkProcessor);
        submit(_LogicProcessor);
        submit(_WriteDispatcher);
        submit(_EncodeProcessor);
        submit(_EncodedProcessor);
    }

    private static int poolSize()
    {
        return 1// aioDispatch
               + 1// link
               + 1// read-decode
               + 1// logic-
               + 1// write-dispatcher
               + 1// write-encode
               + 1// write-end
                ;
    }

    public ThreadFactory getWorkerThreadFactory()
    {
        return _WorkerThreadFactory;
    }

    public TimeWheel getTimeWheel()
    {
        return _TimeWheel;
    }

    @Override
    public RingBuffer<QEvent> getPublisher(OperatorType type)
    {
        return _BizLocalSendEvent;
    }

    @Override
    public RingBuffer<QEvent> getCloser(OperatorType type)
    {
        return _BizLocalCloseEvent;
    }

    @Override
    public ReentrantLock getLock(OperatorType type)
    {
        return _LocalLock;
    }

    @Override
    public AsynchronousChannelGroup getServiceChannelGroup() throws IOException
    {
        return null;
    }
}
