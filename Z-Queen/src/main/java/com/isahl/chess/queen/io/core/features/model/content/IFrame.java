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

package com.isahl.chess.queen.io.core.features.model.content;

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;

/**
 * @author william.d.zk
 */
public interface IFrame
        extends IProtocol,
                IQoS
{
    /**
     * 虽然入参是 int ，但是仅写入单个字节，需要特别注意
     *
     * @param header frame-header
     */
    void header(int header);

    byte header();

    int lack(ByteBuf input);

    /**
     * 当前指令是否为控制层指令
     *
     * @return yes/no
     */
    boolean isCtrl();

    @Override
    default int priority()
    {
        return QOS_PRIORITY_00_NETWORK_CONTROL;
    }
}
