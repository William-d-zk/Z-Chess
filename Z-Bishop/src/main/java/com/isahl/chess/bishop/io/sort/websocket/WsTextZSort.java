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

package com.isahl.chess.bishop.io.sort.websocket;

import com.isahl.chess.bishop.io.sort.BaseSort;
import com.isahl.chess.bishop.io.ws.WsContext;
import com.isahl.chess.bishop.io.ws.filter.WsControlFilter;
import com.isahl.chess.bishop.io.ws.filter.WsFrameFilter;
import com.isahl.chess.bishop.io.ws.filter.WsHandShakeFilter;
import com.isahl.chess.bishop.io.ws.filter.WsTextFilter;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilterChain;

/**
 * @author william.d.zk
 * @date 2021/2/14
 */
public class WsTextZSort
        extends BaseSort<WsContext>
{
    private final WsHandShakeFilter<WsContext> _Head = new WsHandShakeFilter<>();

    public WsTextZSort(Mode mode, Type type)
    {
        super(mode, type, "ws-text");
        _Head.linkFront(new WsFrameFilter<>())
             .linkFront(new WsControlFilter<>())
             .linkFront(new WsTextFilter<>());
    }

    @Override
    public IFilterChain getFilterChain()
    {
        return _Head;
    }

    @Override
    public WsContext newContext(INetworkOption option)
    {
        return new WsContext(option, getMode(), getType());
    }
}
