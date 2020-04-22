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

package com.tgx.chess.queen.event.handler.cluster;

import static com.tgx.chess.queen.event.inf.IOperator.Type.CLUSTER;
import static com.tgx.chess.queen.event.inf.IOperator.Type.DISPATCH;

import java.util.Objects;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IError;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.IPipeEventHandler;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class DecodedDispatcher<C extends IContext<C>>
        implements
        IPipeEventHandler<QEvent>
{
    private final Logger             _Logger = Logger.getLogger(getClass().getSimpleName());
    private final RingBuffer<QEvent> _Cluster;
    private final RingBuffer<QEvent> _Error;

    public DecodedDispatcher(RingBuffer<QEvent> cluster,
                             RingBuffer<QEvent> error)
    {
        _Cluster = cluster;
        _Error = error;
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        if (event.getErrorType() == IError.Type.NO_ERROR) {
            if (event.getEventType() == DISPATCH) {
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
            }
            else {
                _Logger.warning("decoded dispatcher event type error: %s",
                                event.getEventType()
                                     .name());
            }
        }
        else {//错误处理
            IPair dispatchError = event.getContent();
            ISession<C> session = dispatchError.getSecond();
            if (!session.isClosed()) {
                error(_Error, event.getErrorType(), dispatchError, event.getEventOp());
            }
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
        if (sorter.getMode() == ISort.Mode.CLUSTER) {
            publish(_Cluster, CLUSTER, new Pair<>(cmd, session), op);
        }
    }
}
