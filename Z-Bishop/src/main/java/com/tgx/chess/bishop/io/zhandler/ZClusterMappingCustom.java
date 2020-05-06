/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.tgx.chess.bishop.io.zhandler;

import java.util.List;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.event.handler.cluster.IClusterCustom;
import com.tgx.chess.queen.io.core.inf.IConsistentProtocol;
import com.tgx.chess.queen.io.core.inf.ISessionManager;

/**
 * @author william.d.zk
 * @date 2020/4/20
 */
public class ZClusterMappingCustom<T extends IStorage>
        extends
        ZBaseMappingCustom<ZContext,
                           IClusterCustom<ZContext,
                                          T>>
        implements
        IClusterCustom<ZContext,
                       T>
{
    public ZClusterMappingCustom(IClusterCustom<ZContext,
                                                T> then)
    {
        super(then);
    }

    @Override
    public List<ITriple> onTimer(ISessionManager<ZContext> manager, T content)
    {
        return _Then != null ? _Then.onTimer(manager, content)
                             : null;
    }

    @Override
    public List<ITriple> consensus(ISessionManager<ZContext> manager, IConsistentProtocol request, long origin)
    {
        return _Then != null ? _Then.consensus(manager, request, origin)
                             : null;
    }

    @Override
    public boolean waitForCommit()
    {
        return _Then != null && _Then.waitForCommit();
    }
}
