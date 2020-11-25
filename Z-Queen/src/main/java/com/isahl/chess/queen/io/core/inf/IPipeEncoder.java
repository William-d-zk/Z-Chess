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

import java.nio.ByteBuffer;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.event.inf.IOperator;
import com.isahl.chess.queen.io.core.async.AioPacket;

/**
 * @author William.d.zk
 */
public interface IPipeEncoder
        extends
        IOperator<IControl,
                  ISession,
                  ITriple>
{
    default IPacket protocolWrite(IControl output, ISession session)
    {

        IPContext context = session.getContext();
        if (context == null || output == null) { return null; }
        IFilterChain previous = session.getFilterChain()
                                       .getChainTail();
        IProtocol protocol = output;
        CHAIN:
        {
            IFilter.ResultType resultType = IFilter.ResultType.IGNORE;
            while (previous != null) {
                IPipeFilter pipeFilter = previous.getPipeFilter();
                Pair<IFilter.ResultType,
                     IPContext> seekResult = pipeFilter.pipeSeek(context, protocol);
                resultType = seekResult.getFirst();
                context = seekResult.getSecond();
                switch (resultType)
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
                    case CANCEL:
                        return null;
                }
                previous = previous.getPrevious();
            }
            if (resultType == IFilter.ResultType.IGNORE) {
                throw new ZException("no filter handle output: %s ", protocol);
            }
        }
        return protocol instanceof IPacket ? (IPacket) protocol: new AioPacket(ByteBuffer.wrap(protocol.encode()));
    }

}
