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

package com.tgx.chess.queen.event.handler.mix;

import static com.tgx.chess.queen.event.inf.IOperator.Type.LINK;
import static com.tgx.chess.queen.event.inf.IOperator.Type.LOGIC;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.handler.cluster.DecodedDispatcher;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;

/**
 * @author william.d.zk
 */
public class MixDecodedDispatcher<C extends IContext<C>>
        extends
        DecodedDispatcher<C>
{
    private final Logger             _Logger = Logger.getLogger("io.queen.dispatcher." + getClass().getSimpleName());
    private final RingBuffer<QEvent> _Link;

    public MixDecodedDispatcher(RingBuffer<QEvent> link,
                                RingBuffer<QEvent> cluster,
                                RingBuffer<QEvent> error,
                                RingBuffer<QEvent>[] workers)
    {
        super(cluster, error, workers);
        _Link = link;
    }

    @Override
    protected IPair getNextPipe(ISort.Mode mode, IControl<C> cmd)
    {
        if (mode == ISort.Mode.LINK)
        {
            if (cmd.isMapping())
            {
                return new Pair<>(_Link, LINK);
            }
            else
            {
                return new Pair<>(dispatchWorker(cmd), LOGIC);
            }
        }
        return super.getNextPipe(mode, cmd);
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
