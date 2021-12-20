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

package com.isahl.chess.queen.events.server;

import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.events.cluster.DecodedDispatcher;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.io.core.features.model.session.IMessage;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.lmax.disruptor.RingBuffer;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.LINK;

/**
 * @author william.d.zk
 */
public class MixDecodedDispatcher
        extends DecodedDispatcher
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
    protected IPair getNextPipe(ISort.Mode mode, IoSerial input)
    {
        if(input instanceof IMessage msg) {
            if(mode == ISort.Mode.LINK && msg.isMapping()) {
                return Pair.of(_Link, LINK);
            }
        }
        return super.getNextPipe(mode, input);
    }

    public Logger getLogger()
    {
        return _Logger;
    }
}
