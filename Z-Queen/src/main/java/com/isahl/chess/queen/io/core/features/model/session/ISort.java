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

package com.isahl.chess.queen.io.core.features.model.session;

import com.isahl.chess.queen.events.functions.SessionIgnore;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilterChain;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeDecoder;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeEncoder;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeTransfer;

/**
 * @author william.d.zk
 * 基于通讯协议的Pipeline 固有模式分类
 */
public interface ISort
{

    enum Mode
    {
        CLUSTER,
        LINK
    }

    enum Type
    {
        CLIENT,
        SERVER,
        SYMMETRY,
        INNER
    }

    /**
     * 用于区分当前处理过程属于哪个Pipeline
     */
    Mode getMode();

    /**
     * 用于区分 IO 的角色，是服务端还是客户端，或者是对称式
     */
    Type getType();

    IPipeEncoder getEncoder();

    IPipeDecoder getDecoder();

    IPipeTransfer getTransfer();

    ICloser getCloser();

    IFailed getError();

    IFilterChain getFilterChain();

    SessionIgnore getIgnore();

    String getProtocol();
}
