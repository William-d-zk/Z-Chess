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
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.IError;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeTransfer;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.util.List;

public interface ILogicHandler
        extends IBatchHandler<QEvent>
{
    Logger getLogger();

    IManager getManager();

    @Override
    default void onEvent(QEvent event, long sequence)
    {
        switch(event.getEventType()) {
            case SERVICE -> {
                IoSerial request = event.getContent()
                                        .getFirst();
                try {
                    serviceHandle(request);
                }
                catch(Exception e) {
                    getLogger().warning("service handler %s", e, request);
                }
                event.ignore();
            }
            case LOGIC -> {
                IControl<?> content = event.getContent()
                                           .getFirst();
                ISession session = event.getContent()
                                        .getSecond();
                IOperator<IProtocol, ISession, List<ITriple>> transfer = event.getEventOp();
                if(transfer == null) {
                    transfer = defaultTransfer();
                }
                if(content != null) {
                    try {
                        List<ITriple> responses = transfer.handle(content, session);
                        if(responses != null && !responses.isEmpty()) {
                            event.produce(IOperator.Type.DISPATCH, responses);
                        }
                        else {
                            event.ignore();
                        }
                    }
                    catch(Exception e) {
                        getLogger().warning("logic handler interface", e);
                        event.error(IError.Type.HANDLE_DATA, new Pair<>(e, session), session.getError());
                    }
                }
                else {
                    event.ignore();
                }
            }
            default -> throw new ZException("Unsupported type[%s] no handle,%s",
                                            event.getEventType(),
                                            event.getContent());
        }
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
