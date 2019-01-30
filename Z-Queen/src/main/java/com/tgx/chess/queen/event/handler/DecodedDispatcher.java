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

import static com.tgx.chess.queen.event.inf.IOperator.Type.LOGIC;

import java.util.Objects;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ISession;

public class DecodedDispatcher
        extends
        BaseDispatcher
{

    public DecodedDispatcher(RingBuffer<QEvent> link,
                             RingBuffer<QEvent> cluster,
                             RingBuffer<QEvent> error,
                             RingBuffer<QEvent>[] logic)
    {
        super(link, cluster, error, logic);
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        switch (event.getErrorType())
        {
            case NO_ERROR:
                switch (event.getEventType())
                {
                    case TRANSFER:
                    case LOGIC:
                    case DISPATCH:
                        Pair<ICommand[],
                             ISession> dispatchContent = event.getContent();
                        ISession session = dispatchContent.second();
                        ICommand[] commands = dispatchContent.first();
                        if (Objects.nonNull(commands)) {
                            for (ICommand cmd : commands) {
                                //dispatch 到对应的 处理器里
                                dispatch(session.getHandler(), cmd, session);
                            }
                        }
                }
                break;
            default:
                //错误处理
                Pair<Throwable,
                     ISession> dispatchError = event.getContent();
                ISession session = dispatchError.second();
                error(_Error, event.getErrorType(), dispatchError.first(), session, event.getEventOp());
                break;

        }
        event.reset();
    }

    private void dispatch(ISort sorter, ICommand cmd, ISession session)
    {
        switch (sorter.getMode())
        {
            case CLUSTER:
                publish(_Cluster, LOGIC, cmd, session, null);
                break;
            case LINK:
                if (cmd.isMappingCommand()) {
                    publish(_Link, LOGIC, cmd, session, null);
                }
                else {
                    publish(dispatchWorker(session.getHashKey()), LOGIC, cmd, session, null);
                }
                break;
            default:
                // ignore consumer event
                break;
        }
    }
}
