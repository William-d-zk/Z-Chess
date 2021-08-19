/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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

package com.isahl.chess.bishop.io.ws.zchat.zhandler;

import com.isahl.chess.king.base.disruptor.event.inf.IOperator;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.event.handler.cluster.IClusterCustom;
import com.isahl.chess.queen.io.core.inf.IConsistent;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionManager;

import java.util.List;

/**
 * @author william.d.zk
 * @date 2020/4/20
 */
public class ZClusterMappingCustom<T extends IStorage>
        extends ZBaseMappingCustom<IClusterCustom<T>>
        implements IClusterCustom<T>
{
    public ZClusterMappingCustom(IClusterCustom<T> then)
    {
        super(then);
    }

    @Override
    public List<ITriple> onTimer(ISessionManager manager, T content)
    {
        return _Then != null ? _Then.onTimer(manager, content) : null;
    }

    @Override
    public <E extends IConsistent> List<ITriple> consistent(ISessionManager manager, E request)
    {
        return _Then != null ? _Then.consistent(manager, request) : null;
    }

    @Override
    public <E extends IConsistent> List<ITriple> changeTopology(ISessionManager manager, E topology)
    {
        return _Then != null ? _Then.changeTopology(manager, topology) : null;
    }

    @Override
    public boolean waitForCommit()
    {
        return _Then != null && _Then.waitForCommit();
    }

    @Override
    public IOperator<IConsistent, ISession, IPair> getOperator()
    {
        return this::resolve;
    }

    @Override
    public IPair resolve(IConsistent request, ISession session)
    {
        return _Then == null ? null : _Then.resolve(request, session);
    }
}
