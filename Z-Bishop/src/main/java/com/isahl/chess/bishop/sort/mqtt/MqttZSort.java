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

package com.isahl.chess.bishop.sort.mqtt;

import com.isahl.chess.queen.io.core.model.BaseSort;
import com.isahl.chess.bishop.protocol.mqtt.factory.QttFactory;
import com.isahl.chess.bishop.protocol.mqtt.filter.QttCommandFilter;
import com.isahl.chess.bishop.protocol.mqtt.filter.QttControlFilter;
import com.isahl.chess.bishop.protocol.mqtt.filter.QttFrameFilter;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilterChain;

public class MqttZSort
        extends BaseSort<QttContext>
{
    final QttFrameFilter _Head = new QttFrameFilter();

    {
        _Head.linkFront(new QttControlFilter())
             .linkFront(new QttCommandFilter());
    }

    public MqttZSort(Mode mode, Type type)
    {
        super(mode, type, "mqtt");
    }

    @Override
    public IFilterChain getFilterChain()
    {
        return _Head;
    }

    @Override
    public QttContext newContext(INetworkOption option)
    {
        return new QttContext(option, getMode(), getType());
    }

    @Override
    public IoFactory _SelectFactory(Mode mode, Type type)
    {
        return QttFactory._Instance;
    }
}
