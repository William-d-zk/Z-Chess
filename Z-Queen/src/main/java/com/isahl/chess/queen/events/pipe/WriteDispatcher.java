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
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
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
                case NULL,// 在前一个处理器event.reset().
                        IGNORE // 没有任何时间需要跨 Barrier 投递向下一层 Pipeline
                        -> {
                }
                case BIZ_LOCAL, // from biz local
                        CLUSTER_LOCAL,// from cluster local
                        WRITE// from LinkIo/Cluster
                        -> {
                    IPair writeContent = event.getContent();
                    _Logger.debug("content:%s,%s", writeContent, event.getEventType());
                    IControl cmd = writeContent.getFirst();
                    ISession session = writeContent.getSecond();
                    if(cmd.isShutdown()) {
                        if(!session.isValid()) {
                            error(_Error,
                                  INITIATIVE_CLOSE,
                                  new Pair<>(new ZException("session to shutdown"), session),
                                  session.getError());
                        }
                    }
                    else {
                        publish(dispatchEncoder(session.hashCode()),
                                IOperator.Type.WRITE,
                                new Pair<>(cmd, session),
                                session.getEncoder());

                    }
                }
                case WROTE// from io-wrote
                        -> {
                    IPair wroteContent = event.getContent();
                    int wroteCount = wroteContent.getFirst();
                    ISession session = wroteContent.getSecond();
                    if(session.isValid()) {
                        publish(dispatchEncoder(session.hashCode()),
                                IOperator.Type.WROTE,
                                new Pair<>(wroteCount, session),
                                event.getEventOp());
                    }
                }
                case LOGIC,// from read->logic
                        DISPATCH -> {
                    List<ITriple> writeContents = event.getContentList();
                    for(ITriple content : writeContents) {
                        IControl command = content.getFirst();
                        ISession session = content.getSecond();
                        if(command.isShutdown()) {
                            if(session != null && session.isValid()) {
                                error(_Error,
                                      INITIATIVE_CLOSE,
                                      new Pair<>(new ZException("session to shutdown"), session),
                                      session.getError());
                            }
                            /*
                             * session == null 意味着 _SessionManager.find..失败
                             * !session.isValid 意味着 session 已经被关闭
                             * 这两种情形都忽略执行即可
                             */
                            else {
                                _Logger.warning("dispatch failed [ %s ]", session);
                            }
                        }
                        else {
                            publish(dispatchEncoder(session.hashCode()),
                                    IOperator.Type.WRITE,
                                    new Pair<>(command, session),
                                    content.getThird());
                        }
                    }
                }
                default -> {}
            }
        }
        event.reset();
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
