/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.queen.io.core.manager;

import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.io.core.async.AioSessionManager;
import com.isahl.chess.queen.io.core.executor.ServerCore;
import com.isahl.chess.queen.io.core.inf.IActivity;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public abstract class MixManager
        extends
        AioSessionManager<ServerCore>
        implements
        IActivity
{
    private final ServerCore _ServerCore;

    public MixManager(IAioConfig config,
                      ServerCore serverCore)
    {
        super(config);
        _ServerCore = serverCore;
    }

    @Override
    public void close(ISession session, OperatorType type)
    {
        _ServerCore.close(session, type);
    }

    @Override
    public final boolean send(ISession session, OperatorType type, IControl... commands)
    {
        return _ServerCore.send(session, type, commands);
    }

    @Override
    protected ServerCore getCore()
    {
        return _ServerCore;
    }
}
