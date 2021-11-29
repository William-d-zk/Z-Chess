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

package com.isahl.chess.bishop.protocol.zchat;

import com.isahl.chess.bishop.io.BaseSort;
import com.isahl.chess.bishop.protocol.ws.WsContext;
import com.isahl.chess.bishop.protocol.ws.filter.WsControlFilter;
import com.isahl.chess.bishop.protocol.ws.filter.WsFrameFilter;
import com.isahl.chess.bishop.protocol.ws.filter.WsHandShakeFilter;
import com.isahl.chess.bishop.protocol.zchat.factory.ZClusterFactory;
import com.isahl.chess.bishop.protocol.zchat.factory.ZConsumerFactory;
import com.isahl.chess.bishop.protocol.zchat.factory.ZServerFactory;
import com.isahl.chess.bishop.protocol.zchat.factory.ZSymmetryFactory;
import com.isahl.chess.bishop.protocol.zchat.filter.EZContext;
import com.isahl.chess.bishop.protocol.zchat.filter.ZCommandFilter;
import com.isahl.chess.bishop.protocol.zchat.filter.ZEFilter;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilterChain;
import com.isahl.chess.queen.io.core.net.socket.features.IAioSort;

public class ZlsZSort
        extends BaseSort<EZContext<WsContext>>
{
    private final IAioSort<WsContext>            _ActingSort;
    private final ZEFilter<EZContext<WsContext>> _Head = new ZEFilter<>();

    public ZlsZSort(Mode mode, Type type, IAioSort<WsContext> actingSort)
    {
        super(mode, type, String.format("zls-%s", actingSort.getProtocol()));
        _ActingSort = actingSort;
        _Head.linkFront(new WsHandShakeFilter<>())
             .linkFront(new WsFrameFilter<>())
             .linkFront(new WsControlFilter<>())
             .linkFront(new ZCommandFilter<>(mode == Mode.CLUSTER ? new ZClusterFactory()
                                                                  : type == Type.SERVER ? new ZServerFactory()
                                                                                        : type == Type.SYMMETRY
                                                                                          ? new ZSymmetryFactory()
                                                                                          : new ZConsumerFactory()));
    }

    @Override
    public IFilterChain getFilterChain()
    {
        return _Head;
    }

    @Override
    public EZContext<WsContext> newContext(INetworkOption option)
    {
        return new EZContext<>(option, getMode(), getType(), _ActingSort.newContext(option));
    }
}
