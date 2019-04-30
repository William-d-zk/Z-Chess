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

package com.tgx.chess.queen.event.operator;

import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.ISession;

public class WroteOperator<C extends IContext>
        implements
        IOperator<Integer, ISession<C>, ITriple>
{
    private final AioWriter<C>     _AioWrite;
    private final CloseOperator<C> _CloseOperator = new CloseOperator<>();
    private final ErrorOperator<C> _ErrorOperator = new ErrorOperator<>(_CloseOperator);

    public WroteOperator(AioWriter<C> aioWriter) {
        _AioWrite = aioWriter;
    }

    @Override
    public ITriple handle(Integer wroteCnt, ISession<C> session) {
        try {
            session.writeNext(wroteCnt, _AioWrite);
        }
        catch (Exception e) {
            return new Triple<>(e, session, _ErrorOperator);
        }
        return null;
    }
}
