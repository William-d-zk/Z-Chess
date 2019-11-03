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

package com.tgx.chess.queen.io.core.inf;

import java.io.IOException;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.event.inf.IOperator;

/**
 * @author william.d.zk
 * @date 2019-05-12
 */
public interface ISessionCloser<C extends IContext<C>>
        extends
        IOperator<Void,
                  ISession<C>,
                  Void>
{

    Logger _Logger = Logger.getLogger(ISessionCloser.class.getName());

    @Override
    default Void handle(Void v, ISession<C> session)
    {
        try {
            session.close();
            _Logger.info("closed %s", session.toString());
        }
        catch (IOException e) {
            _Logger.warning("close exception: %s", e, session.toString());
        }
        return null;
    }
}