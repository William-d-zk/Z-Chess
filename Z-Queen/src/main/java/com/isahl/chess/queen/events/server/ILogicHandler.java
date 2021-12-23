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

import com.isahl.chess.king.base.disruptor.features.flow.IBatchHandler;
import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.features.IError;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeTransfer;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.util.List;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.DISPATCH;

public interface ILogicHandler
        extends IBatchHandler<QEvent>
{
    Logger getLogger();

    IManager getManager();

    @Override
    default void onEvent(QEvent event, long sequence)
    {
        switch(event.getEventType()) {
            case SERVICE -> _ServiceHandle(event);
            case LOGIC -> _LogicHandle(event);
            case WRITE, IGNORE, NULL -> getLogger().info("ignore handler[%s]", event.getEventType());
            default -> getLogger().warning("Unsupported type[%s] no handle, %s",
                                           event.getEventType(),
                                           event.getContent());
        }
    }

    default void _LogicHandle(QEvent event)
    {
        IPair content = event.getContent();
        if(content != null) {
            IProtocol input = content.getFirst();
            ISession session = content.getSecond();
            IOperator<IProtocol, ISession, List<ITriple>> transfer = event.getEventOp();
            if(transfer == null) {
                transfer = defaultTransfer();
            }
            if(input != null && transfer != null) {
                try {
                    List<ITriple> responses = transfer.handle(input, session);
                    if(responses != null && !responses.isEmpty()) {
                        event.produce(DISPATCH, responses);
                        return;
                    }
                }
                catch(Exception e) {
                    getLogger().warning("logic handler interface", e);
                    event.error(IError.Type.HANDLE_DATA, new Pair<>(e, session), session.getError());
                    return;
                }
            }
        }
        event.ignore();
    }

    default void _ServiceHandle(QEvent event)
    {
        IPair content = event.getContent();
        if(content != null) {
            IoSerial request = content.getFirst();
            try {
                serviceHandle(request);
            }
            catch(Exception e) {
                getLogger().warning("service handler %s", e, request);
            }
        }
        event.ignore();
    }

    default void serviceHandle(IoSerial request)
    {

    }

    interface factory
    {
        ILogicHandler create(int slot);
    }

    IPipeTransfer defaultTransfer();
}
