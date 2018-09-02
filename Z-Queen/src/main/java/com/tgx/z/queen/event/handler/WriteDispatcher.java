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

package com.tgx.z.queen.event.handler;

import static com.tgx.z.queen.event.inf.IOperator.Type.WRITE;
import static com.tgx.z.queen.event.inf.IOperator.Type.WROTE;

import com.lmax.disruptor.RingBuffer;
import com.tgx.z.queen.base.log.Logger;
import com.tgx.z.queen.base.util.Pair;
import com.tgx.z.queen.event.inf.IPipeEventHandler;
import com.tgx.z.queen.event.processor.QEvent;
import com.tgx.z.queen.io.core.inf.ICommand;
import com.tgx.z.queen.io.core.inf.ISession;

public class WriteDispatcher
        implements
        IPipeEventHandler<QEvent, QEvent>
{
    private final RingBuffer<QEvent>[] _Encoders;
    private final int                  _Mask;
    private final Logger               _Log = Logger.getLogger(getClass().getName());

    @SafeVarargs
    public WriteDispatcher(RingBuffer<QEvent>... workers) {
        _Encoders = workers;
        _Mask = _Encoders.length - 1;
    }

    private RingBuffer<QEvent> dispatchEncoder(long seq) {
        return _Encoders[(int) (seq & _Mask)];
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception {
        _Log.info(event);
        /*
         Write Dispatcher  不存在错误状态的输入,都已经处理完了
         */
        switch (event.getEventType()) {
            case NULL://在前一个处理器event.reset().
            case IGNORE://没有任何时间需要跨 Barrier 投递向下一层 Pipeline
                break;
            case LOCAL://from biz local
            case WRITE://from LinkIo
            case LOGIC://from read->logic
                Pair<ICommand, ISession> writeContent = event.getContent();
                ISession session = writeContent.second();
                tryPublish(dispatchEncoder(session.isEmpty() ? sequence : session.getIndex()),
                           WRITE,
                           writeContent.first(),
                           session,
                           event.getEventOp());
                break;
            case WROTE://from io-wrote
                Pair<Integer, ISession> wroteContent = event.getContent();
                session = wroteContent.second();
                tryPublish(dispatchEncoder(session.isEmpty() ? sequence : session.getIndex()),
                           WROTE,
                           wroteContent.first(),
                           session,
                           event.getEventOp());
                break;
            default:
                break;
        }
    }
}
