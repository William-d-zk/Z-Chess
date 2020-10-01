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

package com.tgx.chess.queen.event.handler.mix;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.queen.event.handler.cluster.IoDispatcher;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IContext;

/**
 * @author william.d.zk
 */
public class MixIoDispatcher<C extends IContext<C>>
        extends
        IoDispatcher<C>
{
    private final RingBuffer<QEvent> _Link;

    @SafeVarargs
    public MixIoDispatcher(RingBuffer<QEvent> link,
                           RingBuffer<QEvent> cluster,
                           RingBuffer<QEvent> wrote,
                           RingBuffer<QEvent>... read)
    {
        super(cluster, wrote, read);
        _Link = link;
    }

    @Override
    protected RingBuffer<QEvent> getNextPipe(ISort<C> sort)
    {
        if (sort.getMode() == ISort.Mode.LINK) { return _Link; }
        return super.getNextPipe(sort);
    }
}
