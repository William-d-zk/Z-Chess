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

package com.isahl.chess.queen.events.cluster;

import com.isahl.chess.king.base.disruptor.components.Health;
import com.isahl.chess.king.base.disruptor.features.debug.IHealth;
import com.isahl.chess.king.base.disruptor.features.flow.IPipeHandler;
import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.lmax.disruptor.RingBuffer;

import java.util.Objects;

/**
 * @author william.d.zk
 */
public class DecodedDispatcher
        implements IPipeHandler<QEvent>
{
    private final Logger               _Logger = Logger.getLogger("io.queen.dispatcher." + getClass().getSimpleName());
    private final RingBuffer<QEvent>   _Cluster;
    private final RingBuffer<QEvent>   _Error;
    private final RingBuffer<QEvent>[] _LogicWorkers;
    private final int                  _WorkerMask;
    private final IHealth              _Health = new Health(-1);

    @SafeVarargs
    public DecodedDispatcher(RingBuffer<QEvent> cluster, RingBuffer<QEvent> error, RingBuffer<QEvent>... workers)
    {
        _Cluster = cluster;
        _Error = error;
        _LogicWorkers = workers;
        _WorkerMask = _LogicWorkers.length - 1;
    }

    @Override
    public IHealth getHealth()
    {
        return _Health;
    }

    @Override
    public void onEvent(QEvent event, long sequence) throws Exception
    {
        if(event.hasError()) {// 错误处理
            IPair dispatchError = event.getContent();
            ISession session = dispatchError.getSecond();
            if(!session.isClosed()) {
                error(_Error, event.getErrorType(), dispatchError, event.getEventOp());
            }
        }
        else {
            if(event.getEventType() == IOperator.Type.DISPATCH) {
                IPair dispatchContent = event.getContent();
                ISession session = dispatchContent.getSecond();
                IControl[] commands = dispatchContent.getFirst();
                if(Objects.nonNull(commands)) {
                    for(IControl cmd : commands) {
                        // dispatch 到对应的 处理器里
                        dispatch(cmd, session, event.getEventOp());
                    }
                }
            }
            else if(event.getEventType() != IOperator.Type.NULL) {
                _Logger.warning("decoded dispatcher event type error: %s",
                                event.getEventType()
                                     .name());
            }
        }
        event.reset();
    }

    private void dispatch(IControl cmd, ISession session, IOperator<IControl, ISession, ITriple> op)
    {
        cmd.putSession(session);
        IPair nextPipe = getNextPipe(session.getMode(), cmd);
        publish(nextPipe.getFirst(), nextPipe.getSecond(), new Pair<>(cmd, session), op);
    }

    protected IPair getNextPipe(ISort.Mode mode, IControl cmd)
    {
        return mode == ISort.Mode.CLUSTER && cmd.isMapping() ? new Pair<>(_Cluster, IOperator.Type.CLUSTER)
                                                             : new Pair<>(dispatchWorker(cmd), IOperator.Type.LOGIC);
    }

    protected RingBuffer<QEvent> dispatchWorker(IControl cmd)
    {
        return _LogicWorkers[cmd.session()
                                .hashCode() & _WorkerMask];
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
