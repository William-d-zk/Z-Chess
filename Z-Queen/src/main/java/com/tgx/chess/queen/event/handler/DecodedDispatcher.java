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
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IError;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class DecodedDispatcher<C extends IContext<C>>
        extends
        BaseDispatcher<C>
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
        if (IError.Type.NO_ERROR.equals(event.getErrorType())) {
            switch (event.getEventType())
            {
                case TRANSFER:
                case LOGIC:
                case DISPATCH:
                    IPair dispatchContent = event.getContent();
                    ISession<C> session = dispatchContent.getSecond();
                    IControl<C>[] commands = dispatchContent.getFirst();
                    if (Objects.nonNull(commands)) {
                        for (IControl<C> cmd : commands) {
                            //dispatch 到对应的 处理器里
                            dispatch(session.getContext()
                                            .getSort(),
                                     cmd,
                                     session,
                                     event.getEventOp());
                        }
                    }
                default:
                    break;
            }
        }
        else {//错误处理
            IPair dispatchError = event.getContent();
            Throwable throwable = dispatchError.getFirst();
            ISession<C> session = dispatchError.getSecond();
            error(_Error, event.getErrorType(), new Pair<>(throwable, session), event.getEventOp());
        }
        event.reset();
    }

    private void dispatch(ISort<C> sorter,
                          IControl<C> cmd,
                          ISession<C> session,
                          IOperator<IControl<C>,
                                    ISession<C>,
                                    ITriple> op)
    {
        cmd.setSession(session);
        switch (sorter.getMode())
        {
            case CLUSTER:
                publish(_Cluster, LOGIC, new Pair<>(cmd, session), op);
                break;
            case LINK:
                if (cmd.isMapping()) {
                    publish(_Link, LOGIC, new Pair<>(cmd, session), op);
                }
                else {
                    publish(dispatchWorker(session.getHashKey()), LOGIC, new Pair<>(cmd, session), op);
                }
                break;
            default:
                // ignore consumer event
                break;
        }
    }
}
