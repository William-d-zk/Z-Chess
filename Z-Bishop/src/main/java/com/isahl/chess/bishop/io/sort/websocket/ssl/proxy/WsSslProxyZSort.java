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

package com.isahl.chess.bishop.io.sort.websocket.ssl.proxy;

import java.security.NoSuchAlgorithmException;

import com.isahl.chess.bishop.io.sort.BaseSort;
import com.isahl.chess.bishop.io.ssl.SSLZContext;
import com.isahl.chess.bishop.io.ws.WsProxyContext;
import com.isahl.chess.bishop.io.ws.filter.WsControlFilter;
import com.isahl.chess.bishop.io.ws.filter.WsFrameFilter;
import com.isahl.chess.bishop.io.ws.filter.WsHandShakeFilter;
import com.isahl.chess.bishop.io.ws.filter.WsProxyFilter;
import com.isahl.chess.queen.event.inf.ISort;
import com.isahl.chess.queen.io.core.inf.IFilterChain;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.ISessionOption;

public class WsSslProxyZSort<A extends IPContext<A>>
        extends
        BaseSort<SSLZContext<WsProxyContext<A>>>
{

    private final ISort<WsProxyContext<A>>             _ActingSort;
    private final WsHandShakeFilter<WsProxyContext<A>> _Head = new WsHandShakeFilter<>();

    public WsSslProxyZSort(Mode mode,
                           Type type,
                           ISort<WsProxyContext<A>> actingSort)
    {
        super(mode, type);
        _ActingSort = actingSort;
        _Head.linkFront(new WsFrameFilter<>())
             .linkFront(new WsControlFilter<>())
             .linkFront(new WsProxyFilter<>())
             .linkFront(actingSort.getFilterChain());
    }

    @Override
    public IFilterChain getFilterChain()
    {
        return _Head;
    }

    @Override
    public SSLZContext<WsProxyContext<A>> newContext(ISessionOption option)
    {
        try {
            return new SSLZContext<>(option, this, _ActingSort.newContext(option));
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
