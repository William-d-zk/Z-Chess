/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.queen.io.core.inf;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.queen.event.inf.IOperator;

/**
 * @author William.d.zk
 */
public interface IPipeEncoder
        extends
        IOperator<IControl,
                  ISession,
                  ITriple>
{
    default <C extends IPContext<C>,
             A extends IPContext<A>> IPacket protocolWrite(IControl output, ISession session)
    {

        C context = session.getContext();
        if (context == null || output == null) { return null; }
        IFilterChain previous = context.getSort()
                                       .getFilterChain()
                                       .getChainTail();
        IProtocol protocol = output;
        CHAIN:
        {
            IFilter.ResultType resultType = IFilter.ResultType.IGNORE;
            while (previous != null) {
                IPipeFilter pipeFilter = previous.getPipeFilter();
                switch (resultType = pipeFilter.pipeSeek(context, protocol))
                {
                    case ERROR:
                        throw new ZException("error output: %s ; filter: %s", protocol, previous.getName());
                    case NEED_DATA:
                        return null;
                    case NEXT_STEP:
                        protocol = pipeFilter.pipeEncode(context, protocol);
                        break;
                    case HANDLED:
                        protocol = pipeFilter.pipeEncode(context, protocol);
                        break CHAIN;
                    case IGNORE:
                        break;
                }
                previous = previous.getPrevious();
            }
            if (resultType == IFilter.ResultType.IGNORE) {
                throw new ZException("no filter handle output: %s ", protocol);
            }
        }
        return (IPacket) protocol;
    }

}
