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
import com.isahl.chess.king.base.disruptor.features.functions.OperateType;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IMessage;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.lmax.disruptor.RingBuffer;

import java.util.List;

import static com.isahl.chess.king.base.disruptor.features.functions.OperateType.CLUSTER;

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
    public IHealth _Health()
    {
        return _Health;
    }

    @Override
    public void onEvent(QEvent event, long sequence) throws Exception
    {
        if(event.hasError()) {// 错误处理
            IPair dispatchError = event.getComponent();
            ISession session = dispatchError.getSecond();
            if(!session.isClosed()) {
                error(_Error, event.getErrorType(), dispatchError, event.getEventBinaryOp());
            }
        }
        else {
            switch(event.getEventType()) {
                case SINGLE -> {
                    IPair content = event.getComponent();
                    if(content != null) {
                        IProtocol decoded = content.getFirst();
                        ISession session = content.getSecond();
                        ISort.Mode mode = session.getMode();
                        IPair nextPipe = getNextPipe(mode, decoded);
                        publish(nextPipe.getFirst(), nextPipe.getSecond(), content, event.getEventBinaryOp());
                    }
                }
                case BATCH -> {
                    //triple.fst 「IProtocol」snd「ISession」trd「IPipeTransfer」
                    List<ITriple> contents = event.getResultList();
                    if(contents != null) {
                        contents.forEach(triple->{
                            IProtocol decoded = triple.getFirst();
                            ISession session = triple.getSecond();
                            ISort.Mode mode = session.getMode();
                            IPair nextPipe = getNextPipe(mode, decoded);
                            publish(nextPipe.getFirst(),
                                    nextPipe.getSecond(),
                                    Pair.of(decoded, session),
                                    triple.getThird());
                        });
                    }
                }
                case SERVICE -> {
                    IoSerial content = event.getComponent()
                                            .getFirst();
                    publish(dispatchWorker(content.hashCode()),
                            OperateType.SERVICE,
                            new Pair<>(content, null),
                            null);
                }
                case IGNORE -> _Logger.warning("decode ignore, need more data");
                default -> _Logger.warning("decoded dispatcher event type no handle: %s", event.getEventType());
            }
        }
    }

    protected IPair getNextPipe(ISort.Mode mode, IoSerial input)
    {
        if(input instanceof IMessage msg) {
            if(mode == ISort.Mode.CLUSTER && msg.isMapping()) {
                return Pair.of(_Cluster, CLUSTER);
            }
            else if(msg instanceof IProtocol content) {
                return Pair.of(dispatchWorker(content.session()
                                                     .hashCode()), OperateType.LOGIC);
            }
        }
        return Pair.of(dispatchWorker(input.hashCode()), OperateType.SERVICE);
    }

    protected RingBuffer<QEvent> dispatchWorker(int hashCode)
    {
        return _LogicWorkers[hashCode & _WorkerMask];
    }

    @Override
    public Logger _Logger()
    {
        return _Logger;
    }
}
