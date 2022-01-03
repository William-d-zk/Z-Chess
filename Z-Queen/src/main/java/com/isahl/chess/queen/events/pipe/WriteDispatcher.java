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

package com.isahl.chess.queen.events.pipe;

import com.isahl.chess.king.base.disruptor.components.Health;
import com.isahl.chess.king.base.disruptor.features.debug.IHealth;
import com.isahl.chess.king.base.disruptor.features.flow.IPipeHandler;
import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.lmax.disruptor.RingBuffer;

import java.util.List;

import static com.isahl.chess.king.base.features.IError.Type.HANDLE_DATA;
import static com.isahl.chess.king.base.features.IError.Type.INITIATIVE_CLOSE;

/**
 * @author william.d.zk
 */
public class WriteDispatcher
        implements IPipeHandler<QEvent>
{
    private final Logger               _Logger = Logger.getLogger("io.queen.dispatcher." + getClass().getSimpleName());
    private final RingBuffer<QEvent>[] _Encoders;
    private final RingBuffer<QEvent>   _Error;
    private final int                  _Mask;
    private final IHealth              _Health = new Health(-1);

    @SafeVarargs
    public WriteDispatcher(RingBuffer<QEvent> error, RingBuffer<QEvent>... workers)
    {
        _Error = error;
        _Encoders = workers;
        _Mask = _Encoders.length - 1;
    }

    @Override
    public IHealth getHealth()
    {
        return _Health;
    }

    private RingBuffer<QEvent> dispatchEncoder(int code)
    {
        return _Encoders[code & _Mask];
    }

    @Override
    public void onEvent(QEvent event, long sequence) throws Exception
    {
        if(event.hasError()) {
            if(event.getErrorType() == HANDLE_DATA) {// from logic handler
                /*
                 * logic 处理错误，转换为shutdown目标投递给 _Error
                 * 交由 IoDispatcher转发给对应的MappingHandler 执行close
                 */
                error(_Error, HANDLE_DATA, event.getContent(), event.getEventOp());
            }
        }
        else {
            switch(event.getEventType()) {
                /*
                 * NULL: 在前一个处理器event.reset().
                 * IGNORE:没有任何时间需要跨 Barrier 投递向下一层 Pipeline
                 */
                case NULL, IGNORE -> {
                }
                case BIZ_LOCAL, CLUSTER_LOCAL, WRITE -> {
                    IPair writeContent = event.getContent();
                    _Logger.debug("content:%s,%s", writeContent, event.getEventType());
                    IProtocol cmd = writeContent.getFirst();
                    ISession session = writeContent.getSecond();
                    if(session != null) {
                        try {
                            cmd.transfer();
                            publish(dispatchEncoder(session.hashCode()),
                                    IOperator.Type.WRITE,
                                    new Pair<>(cmd, session),
                                    session.getEncoder());

                        }
                        catch(ZException e) {
                            error(_Error, INITIATIVE_CLOSE, new Pair<>(e, session), session.getError());
                        }
                    }
                }
                // from io-wrote
                case WROTE -> {
                    IPair wroteContent = event.getContent();
                    int wroteCount = wroteContent.getFirst();
                    ISession session = wroteContent.getSecond();
                    if(session != null) {
                        publish(dispatchEncoder(session.hashCode()),
                                IOperator.Type.WROTE,
                                new Pair<>(wroteCount, session),
                                event.getEventOp());
                    }
                }
                /*
                 * LOGIC:from read->logic
                 * DISPATCH:logic->batch->dispatch
                 */
                case LOGIC, DISPATCH -> {
                    List<ITriple> writeContents = event.getContentList();
                    for(ITriple content : writeContents) {
                        IProtocol cmd = content.getFirst();
                        ISession session = content.getSecond();
                        _Logger.debug("→ %s", content);
                        if(session != null) {
                            try {
                                publish(dispatchEncoder(session.hashCode()),
                                        IOperator.Type.WRITE,
                                        new Pair<>(cmd, session),
                                        content.getThird());
                            }
                            catch(ZException e) {
                                error(_Error, INITIATIVE_CLOSE, new Pair<>(e, session), session.getError());
                            }
                        }
                    }
                }
                default -> {}
            }
        }
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
