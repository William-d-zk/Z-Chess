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

package com.isahl.chess.bishop.io.sort.websocket;

import com.isahl.chess.bishop.io.sort.BaseSort;
import com.isahl.chess.bishop.io.ws.WsContext;
import com.isahl.chess.bishop.io.ws.filter.WsControlFilter;
import com.isahl.chess.bishop.io.ws.filter.WsFrameFilter;
import com.isahl.chess.bishop.io.ws.filter.WsHandShakeFilter;
import com.isahl.chess.bishop.io.ws.zchat.zfilter.ZCommandFilter;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.ZClusterFactory;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.ZConsumerFactory;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.ZServerFactory;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.ZSymmetryFactory;
import com.isahl.chess.queen.io.core.inf.IFilterChain;
import com.isahl.chess.queen.io.core.inf.ISessionOption;

public class WsZSort
        extends
        BaseSort<WsContext>
{

    private final WsHandShakeFilter<WsContext> _Head = new WsHandShakeFilter<>();

    public WsZSort(Mode mode,
                   Type type)
    {
        super(mode, type, "ws-z-chat");
        _Head.linkFront(new WsFrameFilter<>())
             .linkFront(new WsControlFilter<>())
             .linkFront(new ZCommandFilter<>(mode == Mode.CLUSTER ? new ZClusterFactory()
                                                                  : type == Type.SERVER ? new ZServerFactory()
                                                                                        : type == Type.SYMMETRY ? new ZSymmetryFactory()
                                                                                                                : new ZConsumerFactory()));
    }

    @Override
    public IFilterChain getFilterChain()
    {
        return _Head;
    }

    @Override
    public WsContext newContext(ISessionOption option)
    {
        return new WsContext(option, getMode(), getType());
    }
}
