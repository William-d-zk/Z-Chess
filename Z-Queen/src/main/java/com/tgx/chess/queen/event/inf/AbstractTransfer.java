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

package com.tgx.chess.queen.event.inf;

import java.util.Arrays;
import java.util.Objects;

import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ISession;

public class AbstractTransfer
        implements
        IOperator<ICommand[],
                  ISession>
{
    private final IOperator<ICommand,
                            ISession> _Encoder;

    public AbstractTransfer(IOperator<ICommand,
                                      ISession> encoder)
    {
        _Encoder = encoder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Triple<ICommand,
                  ISession,
                  IOperator<ICommand,
                            ISession>>[] transfer(ICommand[] commands, ISession session)
    {
        Objects.requireNonNull(commands);
        Triple<ICommand,
               ISession,
               IOperator<ICommand,
                         ISession>>[] triples = new Triple[commands.length];
        Arrays.setAll(triples, slot -> new Triple<>(commands[slot], session, _Encoder));
        return triples;
    }
}
