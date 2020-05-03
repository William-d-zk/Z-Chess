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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.queen.event.handler.client;

import static com.tgx.chess.queen.event.inf.IOperator.Type.WRITE;
import static com.tgx.chess.queen.event.inf.IOperator.Type.WROTE;

import java.util.List;
import java.util.Objects;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.IPipeEventHandler;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class ClientWriteDispatcher<C extends IContext<C>>
        implements
        IPipeEventHandler<QEvent>
{
    private final Logger _Logger = Logger.getLogger("io.queen.dispatcher." + getClass().getSimpleName());

    final RingBuffer<QEvent> _Error;
    final RingBuffer<QEvent> _Encoder;

    public ClientWriteDispatcher(RingBuffer<QEvent> error,
                                 RingBuffer<QEvent> encoder)
    {
        _Error = error;
        _Encoder = encoder;
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        if (event.hasError()) {
            switch (event.getErrorType())
            {
                case FILTER_DECODE:
                case ILLEGAL_STATE:
                case ILLEGAL_BIZ_STATE:
                default:
                    error(_Error, event.getErrorType(), event.getContent(), event.getEventOp());
            }
        }
        else {
            switch (event.getEventType())
            {
                case BIZ_LOCAL://from biz local
                case WRITE://from LinkIo
                case LOGIC://from read->logic
                    IPair writeContent = event.getContent();
                    IControl<C>[] commands = writeContent.getFirst();
                    ISession<C> session = writeContent.getSecond();
                    if (session.isValid() && Objects.nonNull(commands)) {
                        IOperator<IControl<C>[],
                                  ISession<C>,
                                  List<ITriple>> transferOperator = event.getEventOp();
                        List<ITriple> triples = transferOperator.handle(commands, session);
                        for (ITriple triple : triples) {
                            publish(_Encoder, WRITE, new Pair<>(triple.getFirst(), session), triple.getThird());
                        }
                    }
                    break;
                case WROTE://from io-wrote
                    IPair wroteContent = event.getContent();
                    session = wroteContent.getSecond();
                    if (session.isValid()) {
                        publish(_Encoder, WROTE, wroteContent, event.getEventOp());
                    }
                    break;
                default:
                    break;
            }
        }
        event.reset();
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
