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

package com.tgx.chess.queen.io.core.manager;

import com.tgx.chess.queen.config.IAioConfig;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.io.core.async.AioSessionManager;
import com.tgx.chess.queen.io.core.executor.ClusterCore;
import com.tgx.chess.queen.io.core.inf.IActivity;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;

public class ClusterManager<C extends IContext<C>>
        extends
        AioSessionManager<C, ClusterCore<C>>
        implements
        IActivity<C>
{
    private final ClusterCore<C> _ClusterCore;

    public ClusterManager(IAioConfig config, ClusterCore<C> clusterCore)
    {
        super(config);
        _ClusterCore = clusterCore;
    }

    @Override
    public void close(ISession<C> session, IOperator.Type type)
    {
        _ClusterCore.close(session, type);
    }

    @Override
    @SafeVarargs
    public final boolean send(ISession<C> session, IOperator.Type type, IControl<C>... commands)
    {
        return _ClusterCore.send(session, type, commands);
    }

    @Override
    protected ClusterCore<C> getCore()
    {
        return _ClusterCore;
    }
}
