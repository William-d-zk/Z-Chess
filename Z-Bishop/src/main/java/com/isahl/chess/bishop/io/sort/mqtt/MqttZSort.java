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

package com.isahl.chess.bishop.io.sort.mqtt;

import com.isahl.chess.bishop.io.mqtt.QttContext;
import com.isahl.chess.bishop.io.mqtt.filter.QttCommandFilter;
import com.isahl.chess.bishop.io.mqtt.filter.QttControlFilter;
import com.isahl.chess.bishop.io.mqtt.filter.QttFrameFilter;
import com.isahl.chess.bishop.io.sort.BaseSort;
import com.isahl.chess.queen.io.core.inf.IFilterChain;
import com.isahl.chess.queen.io.core.inf.ISessionOption;

public class MqttZSort
        extends
        BaseSort<QttContext>
{
    final QttFrameFilter _Head = new QttFrameFilter();
    {
        _Head.linkFront(new QttControlFilter())
             .linkFront(new QttCommandFilter());
    }

    public MqttZSort(Mode mode,
                     Type type)
    {
        super(mode, type, "mqtt");
    }

    @Override
    public IFilterChain getFilterChain()
    {
        return _Head;
    }

    @Override
    public QttContext newContext(ISessionOption option)
    {
        return new QttContext(option, getMode(), getType());
    }
}
