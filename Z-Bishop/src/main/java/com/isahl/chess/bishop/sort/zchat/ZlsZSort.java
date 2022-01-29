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

import com.isahl.chess.bishop.protocol.zchat.EZContext;
import com.isahl.chess.bishop.protocol.zchat.factory.*;
import com.isahl.chess.bishop.protocol.zchat.filter.ZCommandFilter;
import com.isahl.chess.bishop.protocol.zchat.filter.ZControlFilter;
import com.isahl.chess.bishop.protocol.zchat.filter.ZEFilter;
import com.isahl.chess.bishop.protocol.zchat.filter.ZFrameFilter;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilterChain;
import com.isahl.chess.queen.io.core.model.BaseSort;

public class ZlsZSort
        extends BaseSort<EZContext>
{
    private final ZEFilter<EZContext> _Head;

    public ZlsZSort(Mode mode, Type type)
    {
        super(mode, type, "zchat-ls");
        _Head = new ZEFilter<>();
        _Head.linkFront(new ZFrameFilter())
             .linkFront(new ZControlFilter((ZChatFactory) getFactory()))
             .linkFront(new ZCommandFilter((ZChatFactory) getFactory()));
    }

    @Override
    public IFilterChain getFilterChain()
    {
        return _Head;
    }

    @Override
    public EZContext newContext(INetworkOption option)
    {
        return new EZContext(option, getMode(), getType());
    }

    @Override
    public IoFactory<IProtocol> _SelectFactory(Mode mode, Type type)
    {
        return switch(mode) {
            case CLUSTER -> ZClusterFactory._Instance;
            case LINK -> switch(type) {
                case SERVER -> ZServerFactory._Instance;
                case SYMMETRY -> ZSymmetryFactory._Instance;
                case CLIENT -> ZConsumerFactory._Instance;
                case INNER -> ZInnerFactory._Instance;
            };
        };
    }
}
