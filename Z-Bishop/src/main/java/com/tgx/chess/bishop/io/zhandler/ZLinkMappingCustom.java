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

package com.tgx.chess.bishop.io.zhandler;

import java.util.List;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.queen.event.handler.mix.ILinkCustom;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IProtocol;
import com.tgx.chess.queen.io.core.inf.ISessionManager;
import com.tgx.chess.queen.io.core.inf.ITraceable;

/**
 * @author william.d.zk
 * 
 * @date 2020/4/20
 */
public class ZLinkMappingCustom
        extends
        ZBaseMappingCustom<ZContext, ILinkCustom<ZContext>>
        implements
        ILinkCustom<ZContext>
{

    public ZLinkMappingCustom(ILinkCustom<ZContext> then)
    {
        super(then);
    }

    @Override
    public List<ITriple> notify(ISessionManager<ZContext> manager, IControl<ZContext> request, long origin)
    {
        return _Then != null ?
                _Then.notify(manager, request, origin):
                null;
    }

    @Override
    public void adjudge(IProtocol consensus)
    {
        if (_Then != null)
        {
            _Then.adjudge(consensus);
        }
    }

    @Override
    public <T extends ITraceable & IProtocol> IOperator<T, Throwable, Void> getOperator()
    {
        return this::handle;
    }

    @Override
    public <T extends ITraceable & IProtocol> Void handle(T request, Throwable throwable)
    {
        if (_Then != null)
        {
            _Then.handle(request, throwable);
        }
        return null;
    }
}
