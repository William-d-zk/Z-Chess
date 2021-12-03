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

package com.isahl.chess.bishop.sort.zchat;

import com.isahl.chess.bishop.io.BaseSort;
import com.isahl.chess.bishop.protocol.ws.WsContext;
import com.isahl.chess.bishop.protocol.ws.filter.WsControlFilter;
import com.isahl.chess.bishop.protocol.ws.filter.WsFrameFilter;
import com.isahl.chess.bishop.protocol.ws.filter.WsHandShakeFilter;
import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.bishop.protocol.zchat.factory.ZClusterFactory;
import com.isahl.chess.bishop.protocol.zchat.factory.ZConsumerFactory;
import com.isahl.chess.bishop.protocol.zchat.factory.ZServerFactory;
import com.isahl.chess.bishop.protocol.zchat.factory.ZSymmetryFactory;
import com.isahl.chess.bishop.protocol.zchat.filter.ZCommandFilter;
import com.isahl.chess.bishop.protocol.zchat.filter.ZControlFilter;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.content.IoFactory;
import com.isahl.chess.queen.io.core.features.model.content.IFrame;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilterChain;

public class WsZSort
        extends BaseSort<WsContext>
{

    private final WsHandShakeFilter<WsContext> _Head = new WsHandShakeFilter<>();

    public WsZSort(Mode mode, Type type)
    {
        super(mode, type, "ws-zchat");
        IoFactory<IFrame, ZContext> factory = switch(mode) {
            case CLUSTER -> new ZClusterFactory();
            case LINK -> switch(type) {
                case SERVER -> new ZServerFactory();
                case SYMMETRY -> new ZSymmetryFactory();
                case CLIENT -> new ZConsumerFactory();
                case INNER -> throw new IllegalArgumentException("ws-zchat no support INNER type");
            };
        };
        _Head.linkFront(new WsFrameFilter<>())
             .linkFront(new WsControlFilter<>())
             .linkFront(new ZControlFilter<>(factory))
             .linkFront(new ZCommandFilter<>(factory));
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
