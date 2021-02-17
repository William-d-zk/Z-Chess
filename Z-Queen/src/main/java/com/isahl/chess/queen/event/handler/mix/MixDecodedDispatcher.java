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

package com.isahl.chess.queen.event.handler.mix;


import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.event.handler.cluster.DecodedDispatcher;
import com.isahl.chess.queen.event.QEvent;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.ISort;
import com.lmax.disruptor.RingBuffer;

/**
 * @author william.d.zk
 */
public class MixDecodedDispatcher
        extends
        DecodedDispatcher
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
    protected IPair getNextPipe(ISort.Mode mode, IControl cmd)
    {
        if (mode == ISort.Mode.LINK) {
            if (cmd.isMapping()) {
                return new Pair<>(_Link, OperatorType.LINK);
            }
            else {
                return new Pair<>(dispatchWorker(cmd),OperatorType. LOGIC);
            }
        }
        return super.getNextPipe(mode, cmd);
    }

    public Logger getLogger()
    {
        return _Logger;
    }
}
