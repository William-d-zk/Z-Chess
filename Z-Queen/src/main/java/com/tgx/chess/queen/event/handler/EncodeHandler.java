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

package com.tgx.chess.queen.event.handler;

import static com.tgx.chess.queen.event.inf.IOperator.Type.WRITE;
import static com.tgx.chess.queen.event.inf.IOperator.Type.WROTE;
import static com.tgx.chess.queen.io.core.inf.IContext.ENCODE_ERROR;

import java.util.Objects;

import com.lmax.disruptor.EventHandler;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IError;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class EncodeHandler<C extends IContext<C>>
        implements
        EventHandler<QEvent>
{
    private final Logger _Logger = Logger.getLogger("io.queen.processor." + getClass().getSimpleName());

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        if (event.hasError()) {
            switch (event.getErrorType())
            {
                case FILTER_ENCODE:
                case ILLEGAL_STATE:
                case ILLEGAL_BIZ_STATE:
                default:
                    /*
                     透传到 EncodedHandler 去投递 _Error
                     */
                    break;
            }
        }
        else {
            _Logger.debug("%s→  %s ",
                          event.getEventType(),
                          event.getContent()
                               .getFirst());
            switch (event.getEventType())
            {
                case WRITE:
                    IPair pairWriteContent = event.getContent();
                    IControl<C> cmd = pairWriteContent.getFirst();
                    ISession<C> session = pairWriteContent.getSecond();
                    IOperator<IControl<C>,
                              ISession<C>,
                              ITriple> writeOperator = event.getEventOp();
                    _Logger.debug("%s→  %s | %s", WRITE, cmd, session);
                    encodeHandler(event, cmd, session, writeOperator);
                    cmd.dispose();
                    break;
                case WROTE:
                    IPair pairWroteContent = event.getContent();
                    int wroteCnt = pairWroteContent.getFirst();
                    session = pairWroteContent.getSecond();
                    IOperator<Integer,
                              ISession<C>,
                              ITriple> wroteOperator = event.getEventOp();
                    _Logger.debug("%s→  %s | %s", WROTE, wroteCnt, session);
                    encodeHandler(event, wroteCnt, session, wroteOperator);
                    break;
                default:
                    break;
            }
        }
    }

    private <A> void encodeHandler(QEvent event,
                                   A a,
                                   ISession<C> session,
                                   IOperator<A,
                                             ISession<C>,
                                             ITriple> operator)
    {
        C context = session.getContext();
        if (!context.isOutErrorState()) {
            ITriple result = operator.handle(a, session);
            if (Objects.nonNull(result)) {
                Throwable throwable = result.getFirst();
                event.error(IError.Type.FILTER_ENCODE, new Pair<>(throwable, session), result.getThird());
                context.setOutState(ENCODE_ERROR);
            }
            else {
                event.reset();
            }
        }
    }
}
