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

package com.isahl.chess.queen.events.client;

import com.isahl.chess.king.base.disruptor.components.Health;
import com.isahl.chess.king.base.disruptor.features.debug.IHealth;
import com.isahl.chess.king.base.disruptor.features.flow.IPipeHandler;
import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.lmax.disruptor.RingBuffer;

import java.util.List;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.WRITE;

/**
 * @author william.d.zk
 */
public class ClientWriteDispatcher
        implements IPipeHandler<QEvent>
{
    private final Logger _Logger = Logger.getLogger("io.queen.dispatcher." + getClass().getSimpleName());

    final RingBuffer<QEvent> _Error;
    final RingBuffer<QEvent> _Encoder;

    private final IHealth _Health = new Health(-1);

    public ClientWriteDispatcher(RingBuffer<QEvent> error, RingBuffer<QEvent> encoder)
    {
        _Error = error;
        _Encoder = encoder;
    }

    @Override
    public IHealth getHealth()
    {
        return _Health;
    }

    @Override
    public void onEvent(QEvent event, long sequence) throws Exception
    {
        if(event.hasError()) {
            error(_Error, event.getErrorType(), event.getContent(), event.getEventOp());
        }
        else {
            switch(event.getEventType()) {
                case BIZ_LOCAL, WRITE, LOGIC -> {
                    IPair content = event.getContent();
                    IProtocol output = content.getFirst();
                    ISession session = content.getSecond();
                    if(session.isValid() && output != null) {
                        publish(_Encoder, WRITE, content, session.encoder());
                    }
                }
                case DISPATCH -> {
                    List<ITriple> contents = event.getContentList();
                    if(contents != null) {
                        for(ITriple triple : contents) {
                            IProtocol output = triple.getFirst();
                            ISession session = triple.getSecond();
                            if(output != null && session.isValid()) {
                                publish(_Encoder, WRITE, Pair.of(output, session), triple.getThird());
                            }
                        }
                    }
                }
                case WROTE -> {
                    IPair wrote = event.getContent();
                    ISession session = wrote.getSecond();
                    if(session.isValid()) {
                        publish(_Encoder, IOperator.Type.WROTE, wrote, event.getEventOp());
                    }
                }
                default -> {}
            }
        }
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
