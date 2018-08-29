/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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

package com.tgx.z.queen.io.external.websokcet.operator;

import static com.tgx.z.queen.event.operator.OperatorHolder.AIO_WRITER;
import static com.tgx.z.queen.event.operator.OperatorHolder.ERROR_OPERATOR;

import com.tgx.z.queen.base.util.Triple;
import com.tgx.z.queen.event.inf.IOperator;
import com.tgx.z.queen.event.operator.MODE;
import com.tgx.z.queen.io.core.async.AioContext;
import com.tgx.z.queen.io.core.inf.ICommand;
import com.tgx.z.queen.io.core.inf.IFilterChain;
import com.tgx.z.queen.io.core.inf.IPipeEncoder;
import com.tgx.z.queen.io.core.inf.ISession;
import com.tgx.z.queen.io.external.filter.ZTlsFilter;
import com.tgx.z.queen.io.external.websokcet.WsContext;
import com.tgx.z.queen.io.external.websokcet.filter.WsFrameFilter;
import com.tgx.z.queen.io.external.websokcet.filter.WsHandShakeFilter;

public enum ENCODER_OPERATOR
        implements
        IPipeEncoder,
        IOperator<ICommand, ISession>
{
    CIPHER_ACCEPT(new ZTlsFilter<WsContext>().linkFront(new WsHandShakeFilter(MODE.SERVER))
                                             .linkFront(new WsFrameFilter())) {

    };
    private final IFilterChain<? extends AioContext> _Filter;

    ENCODER_OPERATOR(IFilterChain<? extends AioContext> filter) {
        _Filter = filter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Triple<Throwable, ISession, IOperator<Throwable, ISession>> handle(ICommand _inCommand, ISession session) {
        Throwable t = sessionWrite(_inCommand, _Filter, session, AIO_WRITER());
        return t == null ? null : new Triple<>(t, session, ERROR_OPERATOR());
    }
}
