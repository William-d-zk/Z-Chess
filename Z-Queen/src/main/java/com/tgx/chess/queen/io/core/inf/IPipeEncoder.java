/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.queen.io.core.inf;

import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.queen.event.inf.IOperator;

/**
 * @author William.d.zk
 */
public interface IPipeEncoder<C extends IContext<C>>
        extends
        IOperator<IControl<C>,
                  ISession<C>,
                  ITriple>
{
    @SuppressWarnings("unchecked")
    default <O extends IProtocol> O filterWrite(IControl<C> output, C context)
    {
        IFilter.ResultType resultType;
        IFilterChain<C> previous = context.getSort()
                                          .getFilterChain()
                                          .getChainTail();
        IProtocol toWrite = output;
        while (previous != null) {
            resultType = previous.getFilter()
                                 .preEncode(context, toWrite);
            switch (resultType)
            {
                case ERROR:
                case NEED_DATA:
                    return null;
                case NEXT_STEP:
                    toWrite = previous.getFilter()
                                      .encode(context, toWrite);
                    break;
                default:
                    break;
            }
            previous = previous.getPrevious();
        }
        return (O) toWrite;
    }

}
