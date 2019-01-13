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

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.config.Config;
import com.tgx.chess.queen.io.core.async.AioSessionManager;
import com.tgx.chess.queen.io.core.executor.ServerCore;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ISession;

public abstract class QueenManager
        extends
        AioSessionManager
{
    private final Logger       _Logger = Logger.getLogger(getClass().getName());
    protected final ServerCore _ServerCore;

    public QueenManager(Config config,
                        ServerCore serverCore)
    {
        super(config);
        _ServerCore = serverCore;
    }

    public void localClose(ISession session)
    {
        _ServerCore.localClose(session);
    }

    public boolean localSend(ISession session, ICommand... commands)
    {
        return _ServerCore.localSend(session, commands);
    }

    public abstract ICommand save(ICommand tar, ISession session);

    public abstract ICommand find(ICommand key, ISession session);

}
