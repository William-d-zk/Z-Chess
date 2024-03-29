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

package com.isahl.chess.queen.io.core.features.model.pipe;

import com.isahl.chess.king.base.disruptor.features.functions.IBinaryOperator;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.util.LinkedList;
import java.util.List;

import static com.isahl.chess.king.base.disruptor.features.functions.OperateType.BATCH;
import static com.isahl.chess.king.base.disruptor.features.functions.OperateType.SINGLE;

/**
 * @author William.d.zk
 */
public interface IPipeDecoder
        extends IBinaryOperator<IPacket, ISession, ITriple>
{
    default ITriple filterRead(IPacket input, ISession session)
    {
        IPContext context = session.getContext();
        if(context == null || input == null) {return null;}
        final IFilterChain _Header = session.getFilterChain()
                                            .getChainHead();
        IProtocol protocol = input;
        IPacket proxy = null;
        ITriple result = null;
        for(IFilterChain next = _Header; ; next = _Header, protocol = input, context = session.getContext()) {
            Chain:
            {
                IFilter.ResultType resultType = IFilter.ResultType.IGNORE;
                while(next != null) {
                    IPipeFilter pipeFilter = next.getPipeFilter();
                    Pair<IFilter.ResultType, IPContext> peekResult = pipeFilter.pipePeek(context, protocol);
                    resultType = peekResult.getFirst();
                    context = peekResult.getSecond();
                    switch(resultType) {
                        case ERROR -> throw new ZException("error input: %s ; filter: %s ", protocol, next.getName());
                        case NEED_DATA -> {
                            if(result != null) {
                                /*
                                  协议层已经完成处理，返回所有已处理完毕的
                                  IControl 对象。
                                 */
                                return result;
                            }
                            else if(proxy != null) {
                                /*
                                  代理层跳出、数据已经在frame所在的context全量缓存
                                  进入下一个循环进行检查代理持有的context所持有的的状态
                                 */
                                proxy = null;
                                break Chain;
                            }
                            return null;
                        }
                        case NEXT_STEP -> protocol = pipeFilter.pipeDecode(context, protocol);
                        case PROXY -> protocol = proxy = pipeFilter.pipeDecode(context, protocol);
                        case HANDLED -> {
                            IProtocol cmd = pipeFilter.pipeDecode(context, protocol);
                            if(cmd != null) {
                                cmd.with(session);
                                if(result == null) {
                                    result = Triple.of(cmd, session, SINGLE);
                                }
                                else if(result.getThird() == SINGLE) {
                                    IProtocol previous = result.getFirst();
                                    List<IProtocol> pushList = new LinkedList<>(List.of(previous, cmd));
                                    result = Triple.of(pushList, session, BATCH);
                                }
                                else {
                                    List<IProtocol> pushList = result.getFirst();
                                    pushList.add(cmd);
                                }
                            }
                            break Chain;
                        }
                        case CANCEL -> {
                            return null;
                        }
                        case IGNORE -> {}
                    }
                    next = next.getNext();
                }
                if(resultType == IFilter.ResultType.IGNORE) {
                    throw new ZException("no filter handle input: %s ", protocol);
                }
            }
        }
    }
}
