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

import static com.isahl.chess.queen.event.inf.IOperator.Type.LOGIC;

import java.util.Objects;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.event.inf.IError;
import com.isahl.chess.queen.event.processor.QEvent;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionManager;
import com.lmax.disruptor.EventHandler;

public interface ILogicHandler
        extends
        EventHandler<QEvent>
{
    Logger getLogger();

    ISessionManager getISessionManager();

    @Override
    default void onEvent(QEvent event, long sequence, boolean endOfBatch)
    {
        doEvent(event, sequence, endOfBatch);
    }

    default void doEvent(QEvent event, long sequence, boolean endOfBatch)
    {
        if (event.getEventType() == LOGIC) {
            IControl content = event.getContent()
                                    .getFirst();
            ISession session = event.getContent()
                                    .getSecond();
            if (content != null) {
                try {
                    IControl[] response = handle(getISessionManager(), session, content);
                    if (Objects.nonNull(response) && response.length > 0) {
                        event.produce(LOGIC,
                                      new Pair<>(response, session),
                                      session.getContext()
                                             .getSort()
                                             .getTransfer());
                    }
                    else {
                        event.ignore();
                    }
                }
                catch (Exception e) {
                    getLogger().warning("logic handler interface", e);
                    event.error(IError.Type.HANDLE_DATA,
                                new Pair<>(e, session),
                                session.getContext()
                                       .getSort()
                                       .getError());
                }
            }
            else {
                event.ignore();
            }
        }
    }

    IControl[] handle(ISessionManager manager, ISession session, IControl content) throws Exception;

}
