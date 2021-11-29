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

package com.isahl.chess.bishop.protocol.zchat.custom;

import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.queen.events.server.ILinkCustom;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.IManager;

import java.util.List;

/**
 * @author william.d.zk
 * @date 2020/4/20
 */
public class ZLinkMappingCustom
        extends ZBaseMappingCustom<ILinkCustom>
        implements ILinkCustom
{

    public ZLinkMappingCustom(ILinkCustom then)
    {
        super(then);
    }

    @Override
    public List<ITriple> notify(IManager manager, IControl request, long origin, boolean isConsistency)
    {
        return _Then != null ? _Then.notify(manager, request, origin, isConsistency) : null;
    }

    @Override
    public void close(ISession session)
    {
        if(_Then != null) {
            _Then.close(session);
        }
    }

    @Override
    public <T extends IProtocol> IOperator<IConsistent, ISession, T> getOperator()
    {
        return this::adjudge;
    }

    @Override
    public <T extends IProtocol> T adjudge(IConsistent consistency, ISession session)
    {
        return _Then != null ? _Then.adjudge(consistency, session) : null;
    }
}
