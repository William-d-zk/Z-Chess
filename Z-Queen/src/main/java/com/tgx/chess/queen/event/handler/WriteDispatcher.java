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

package com.tgx.chess.queen.event.handler;

import static com.tgx.chess.queen.event.inf.IOperator.Type.WRITE;
import static com.tgx.chess.queen.event.inf.IOperator.Type.WROTE;

import java.util.List;
import java.util.Objects;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.IPipeEventHandler;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class WriteDispatcher<C extends IContext<C>>
        implements
        IPipeEventHandler<QEvent>
{
    private final RingBuffer<QEvent>[] _Encoders;
    private final int                  _Mask;
    private final Logger               _Logger = Logger.getLogger(getClass().getName());

    @SafeVarargs
    public WriteDispatcher(RingBuffer<QEvent>... workers)
    {
        _Encoders = workers;
        _Mask = _Encoders.length - 1;
    }

    private RingBuffer<QEvent> dispatchEncoder(long seq)
    {
        return _Encoders[(int) (seq & _Mask)];
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        /*
         Write Dispatcher  不存在错误状态的输入,都已经处理完了
         */
        switch (event.getEventType())
        {
            case NULL://在前一个处理器event.reset().
            case IGNORE://没有任何时间需要跨 Barrier 投递向下一层 Pipeline
                break;
            case LOCAL://from biz local
            case WRITE://from LinkIo/Cluster
            case LOGIC://from read->logic
                IPair writeContent = event.getContent();
                IControl[] commands = writeContent.first();
                ISession<C> session = writeContent.second();
                if (session.isValid() && Objects.nonNull(commands)) {
                    IOperator<IControl[],
                              ISession,
                              List<ITriple>> transferOperator = event.getEventOp();
                    List<ITriple> triples = transferOperator.handle(commands, session);
                    for (ITriple triple : triples) {
                        tryPublish(dispatchEncoder(session.getHashKey()),
                                   WRITE,
                                   new Pair<>(triple.first(), session),
                                   triple.third());
                    }
                    _Logger.info("write_dispatcher, source %s,transfer:%d", event.getEventType(), commands.length);
                }
                break;
            case WROTE://from io-wrote
                IPair wroteContent = event.getContent();
                int wroteCount = wroteContent.first();
                session = wroteContent.second();
                if (session.isValid()) {
                    tryPublish(dispatchEncoder(session.getHashKey()),
                               WROTE,
                               new Pair<>(wroteCount, session),
                               event.getEventOp());
                }
                break;
            default:
                break;
        }
        event.reset();
    }
}
