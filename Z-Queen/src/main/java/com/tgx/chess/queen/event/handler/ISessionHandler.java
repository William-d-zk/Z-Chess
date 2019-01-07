/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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

import static com.tgx.chess.queen.io.core.inf.IContext.ENCODE_ERROR;

import java.util.Objects;

import com.lmax.disruptor.EventHandler;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.event.inf.IError.Type;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.ISession;

public interface ISessionHandler
        extends
        EventHandler<QEvent>
{
    default <A> void encodeHandler(QEvent event, A a, ISession session, IOperator<A, ISession> operator)
    {
        IContext context = session.getContext();
        if (!context.isOutErrorState()) {
            Triple<Throwable, ISession, IOperator<Throwable, ISession>> result = operator.handle(a, session);
            if (Objects.nonNull(result)) {
                event.error(Type.FILTER_DECODE, result.first(), result.second(), result.third());
                context.setOutState(ENCODE_ERROR);
            }
            else {
                event.ignore();
            }
        }
    }
}
