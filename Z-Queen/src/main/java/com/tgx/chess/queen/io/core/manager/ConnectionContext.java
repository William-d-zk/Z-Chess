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

package com.tgx.chess.queen.io.core.manager;

import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.io.core.inf.ICommandCreator;
import com.tgx.chess.queen.io.core.inf.IConnectActive;
import com.tgx.chess.queen.io.core.inf.IConnectionContext;
import com.tgx.chess.queen.io.core.inf.ISessionCreated;
import com.tgx.chess.queen.io.core.inf.ISessionCreator;

public class ConnectionContext
        implements
        IConnectionContext
{
    private final ISort           _Sorter;
    private final ISessionCreator _SessionCreator;
    private final IConnectActive  _ConnectActive;
    private final ICommandCreator _CommandCreator;
    private final ISessionCreated _SessionCreated;

    public ConnectionContext(ISort dispatcher,
                             IConnectActive connectActive,
                             ISessionCreator sessionCreator,
                             ICommandCreator commandCreator,
                             ISessionCreated sessionCreated)
    {
        _Sorter = dispatcher;
        _ConnectActive = connectActive;
        _SessionCreator = sessionCreator;
        _CommandCreator = commandCreator;
        _SessionCreated = sessionCreated;
    }

    @Override
    public ISessionCreator getSessionCreator()
    {
        return _SessionCreator;
    }

    @Override
    public ICommandCreator getCommandCreator()
    {
        return _CommandCreator;
    }

    @Override
    public ISessionCreated getSessionCreated()
    {
        return _SessionCreated;
    }

    @Override
    public ISort getHandler()
    {
        return _Sorter;
    }

    @Override
    public IConnectActive getConnectActive()
    {
        return _ConnectActive;
    }

}
