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

/**
 * @author William.d.zk
 */
public interface IFilterChain
{
    void idempotentLeftShift(int previous);

    IFilterChain getPrevious();

    void setPrevious(IFilterChain filter);

    IFilterChain getNext();

    void setNext(IFilterChain filter);

    IFilterChain getChainHead();

    IFilterChain getChainTail();

    IFilterChain linkAfter(IFilterChain curFilter);

    IFilterChain linkFront(IFilterChain curFilter);

    /**
     * 取得当前filter所占有的右向幂等位
     *
     * @return idempotent bit
     */
    int getRightIdempotentBit();

    /**
     * 取得当前filter所占有的左向幂等位
     *
     * @return idempotent bit
     */
    int getLeftIdempotentBit();

    /**
     * 32bit 确保每个Protocol 从chain上只流经一次
     * filter chain 不得成环
     *
     * @param previous previous idempotent bit
     */
    void idempotentRightShift(int previous);

    IPipeFilter getPipeFilter();

    String getName();
}
