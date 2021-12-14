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

package com.isahl.chess.bishop.sort.ssl;

import com.isahl.chess.bishop.io.BaseSort;
import com.isahl.chess.bishop.io.ssl.SSLFilter;
import com.isahl.chess.bishop.io.ssl.SSLZContext;
import com.isahl.chess.bishop.io.ssl.SslHandShakeFilter;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilterChain;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.ssl.ISslOption;
import com.isahl.chess.queen.io.core.net.socket.features.IAioSort;

import java.security.NoSuchAlgorithmException;

public class SslZSort<T extends IPContext>
        extends BaseSort<SSLZContext<T>>
{
    private final IAioSort<T> _ActingSort;

    private final SslHandShakeFilter<SSLZContext<T>> _Head = new SslHandShakeFilter<>();

    public SslZSort(ISort.Mode mode, ISort.Type type, IAioSort<T> actingSort)
    {
        super(mode, type, String.format("ssl-%s", actingSort.getProtocol()));
        _ActingSort = actingSort;
        _Head.linkFront(new SSLFilter<>())
             .linkFront(actingSort.getFilterChain());
    }

    @Override
    public IFilterChain getFilterChain()
    {
        return _Head;
    }

    @Override
    public SSLZContext<T> newContext(INetworkOption option)
    {
        try {
            return new SSLZContext<>((ISslOption) option, getMode(), getType(), _ActingSort.newContext(option));
        }
        catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
