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

package com.isahl.chess.king.base.disruptor.components;

import com.isahl.chess.king.base.disruptor.features.event.IEvent;
import com.isahl.chess.king.base.disruptor.features.flow.IBatchHandler;
import com.isahl.chess.king.base.log.Logger;
import com.lmax.disruptor.*;

import java.util.concurrent.atomic.AtomicInteger;

public class Z1Processor<T extends IEvent>
        implements EventProcessor
{

    private static final int IDLE    = 0;
    private static final int HALTED  = IDLE + 1;
    private static final int RUNNING = HALTED + 1;

    private final Logger                _Logger;
    private final AtomicInteger         _Running  = new AtomicInteger(IDLE);
    private final DataProvider<T>       _DataProvider;
    private final SequenceBarrier  _SequenceBarrier;
    private final IBatchHandler<T> _BatchEventHandler;
    private final Sequence         _Sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);

    private ExceptionHandler<T> exceptionHandler;

    /**
     * Construct a {@link EventProcessor} that will automatically track the progress by updating its sequence when
     * the {@link EventHandler#onEvent(Object, long, boolean)} method returns.
     *
     * @param dataProvider    to which events are published.
     * @param sequenceBarrier on which it is waiting.
     * @param eventHandler    is the delegate to which events are dispatched.
     */
    public Z1Processor(final DataProvider<T> dataProvider,
                       final SequenceBarrier sequenceBarrier,
                       final IBatchHandler<T> eventHandler)
    {
        _DataProvider = dataProvider;
        _SequenceBarrier = sequenceBarrier;
        _BatchEventHandler = eventHandler;
        _Logger = Logger.getLogger(String.format("base.king.%s.%s",
                                                 getClass().getSimpleName(),
                                                 eventHandler.getClass()
                                                             .getSimpleName()));
    }

    @Override
    public Sequence getSequence()
    {
        return _Sequence;
    }

    @Override
    public void halt()
    {
        _Running.set(HALTED);
        _SequenceBarrier.alert();
    }

    @Override
    public boolean isRunning()
    {
        return _Running.get() != IDLE;
    }

    /**
     * Set a new {@link ExceptionHandler} for handling exceptions propagated out of the {@link BatchEventProcessor}
     *
     * @param exceptionHandler to replace the existing exceptionHandler.
     */
    public void setExceptionHandler(final ExceptionHandler<T> exceptionHandler)
    {
        if(null == exceptionHandler) {
            throw new NullPointerException();
        }
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * It is ok to have another thread rerun this method after a halt().
     *
     * @throws IllegalStateException if this object instance is already running in a thread
     */
    @Override
    public void run()
    {
        if(_Running.compareAndSet(IDLE, RUNNING)) {
            _SequenceBarrier.clearAlert();

            try {
                if(_Running.get() == RUNNING) {
                    processEvents();
                }
            }
            finally {
                _Running.set(IDLE);
            }
        }
        else {
            // This is a little bit of guess work.  The running state could of changed to HALTED by
            // this point.  However, Java does not have compareAndExchange which is the only way
            // to get it exactly correct.
            if(_Running.get() == RUNNING) {
                throw new IllegalStateException("Thread is already running");
            }
            else {
                halt();
            }
        }
    }

    private void processEvents()
    {
        T event = null;
        long nextSequence = _Sequence.get() + 1L;

        while(true) {
            try {
                final long availableSequence = _SequenceBarrier.waitFor(nextSequence);
                _BatchEventHandler.onBatchStart(nextSequence - 1);
                while(nextSequence <= availableSequence) {
                    event = _DataProvider.get(nextSequence);
                    _BatchEventHandler.onEvent(event, nextSequence);
                    nextSequence++;
                }
                _Sequence.set(availableSequence);
            }
            catch(final TimeoutException e) {
                notifyTimeout(_Sequence.get());
            }
            catch(final AlertException ex) {
                if(_Running.get() != RUNNING) {
                    break;
                }
            }
            catch(final Throwable ex) {
                handleEventException(ex, nextSequence, event);
                _Sequence.set(nextSequence);
                nextSequence++;
            }
            finally {
                _BatchEventHandler.onBatchComplete(_Sequence.get());
            }
        }
    }

    private void notifyTimeout(final long availableSequence)
    {
        try {
            _BatchEventHandler.onTimeout(availableSequence);
        }
        catch(Throwable e) {
            handleEventException(e, availableSequence, null);
        }
    }

    /**
     * Delegate to {@link ExceptionHandler#handleEventException(Throwable, long, Object)} on the delegate or
     * the default {@link ExceptionHandler} if one has not been configured.
     */
    private void handleEventException(final Throwable ex, final long sequence, final T event)
    {
        _Logger.warning(ex);
        getExceptionHandler().handleEventException(ex, sequence, event);
    }

    private ExceptionHandler<? super T> getExceptionHandler()
    {
        ExceptionHandler<? super T> handler = exceptionHandler;
        if(handler == null) {
            return ExceptionHandlers.defaultHandler();
        }
        return handler;
    }
}
