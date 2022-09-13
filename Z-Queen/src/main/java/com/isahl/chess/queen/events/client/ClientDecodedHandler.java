/*
 * MIT License
 *
 * Copyright (c) 2021. Z-Chess
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
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.events.model.QEvent;
import com.lmax.disruptor.RingBuffer;

import java.util.List;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.LOGIC;
import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.WRITE;

/**
 * @author william.d.zk
 * @date 2021-12-21
 */
public class ClientDecodedHandler
        implements IPipeHandler<QEvent>
{

    private final Logger             _Logger = Logger.getLogger("io.queen.dispatcher." + getClass().getSimpleName());
    private final RingBuffer<QEvent> _LogicPublisher;
    private final IHealth            _Health = new Health(-1);

    public ClientDecodedHandler(RingBuffer<QEvent> logicPublisher)
    {
        _LogicPublisher = logicPublisher;
    }

    @Override
    public IHealth _Health()
    {
        return _Health;
    }

    @Override
    public void onEvent(QEvent event, long sequence) throws Exception
    {
        switch(event.getEventType()) {
            case SINGLE -> publish(_LogicPublisher, LOGIC, event.getContent(), event.getEventOp());
            case BATCH -> {
                List<ITriple> contents = event.getContentList();
                for(ITriple content : contents) {
                    publish(_LogicPublisher,
                            LOGIC,
                            Pair.of(content.getFirst(), content.getSecond()),
                            content.getThird());
                }
            }
            case WRITE -> publish(_LogicPublisher, WRITE, event.getContent(), event.getEventOp());
            case DISPATCH -> {
                List<ITriple> outputs = event.getContentList();
                for(ITriple output : outputs) {
                    publish(_LogicPublisher, WRITE, Pair.of(output.getFirst(), output.getSecond()), output.getThird());
                }
            }
        }
    }

    @Override
    public Logger _Logger()
    {
        return _Logger;
    }
}
