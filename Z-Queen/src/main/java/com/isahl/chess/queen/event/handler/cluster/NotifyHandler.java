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

package com.isahl.chess.queen.event.handler.cluster;

import com.isahl.chess.queen.event.processor.QEvent;
import com.lmax.disruptor.EventHandler;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.event.inf.IOperator;
import com.isahl.chess.queen.io.core.inf.IProtocol;

public class NotifyHandler
        implements
        EventHandler<QEvent>
{
    private final Logger _Logger = Logger.getLogger("io.queen.processor." + getClass().getSimpleName());

    @Override
    public void onEvent(QEvent event, long seq, boolean batch) throws Exception
    {
        _Logger.debug("notify handler: -> %s", event);
        IPair                            content  = event.getContent();
        IProtocol                        protocol = content.getFirst();
        IOperator<IProtocol, Void, Void> notifier = event.getEventOp();
        if (notifier != null)
        {
            notifier.handle(protocol, null);
        }
        event.reset();
    }
}
