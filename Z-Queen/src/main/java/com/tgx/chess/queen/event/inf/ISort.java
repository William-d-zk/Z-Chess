/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.queen.event.inf;

import com.tgx.chess.queen.event.operator.IgnoreOperator;
import com.tgx.chess.queen.io.core.inf.ICommandFactory;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IFilterChain;
import com.tgx.chess.queen.io.core.inf.IFrame;
import com.tgx.chess.queen.io.core.inf.IPipeDecoder;
import com.tgx.chess.queen.io.core.inf.IPipeEncoder;
import com.tgx.chess.queen.io.core.inf.IPipeTransfer;
import com.tgx.chess.queen.io.core.inf.ISessionCloser;
import com.tgx.chess.queen.io.core.inf.ISessionError;
import com.tgx.chess.queen.io.core.inf.ISessionOption;

/**
 * @author william.d.zk
 *         基于通讯协议的Pipeline 固有模式分类
 */
public interface ISort<C extends IContext<C>>
{

    enum Mode
    {
        CLUSTER,
        LINK
    }

    enum Type
    {
        SERVER,
        CONSUMER,
        SYMMETRY
    }

    default boolean isSSL()
    {
        return false;
    }

    /**
     *
     * 用于区分当前处理过程属于哪个Pipeline
     */
    Mode getMode();

    /**
     * 
     * 用于区分 IO 的角色，是服务端还是客户端，或者是对称式
     */
    Type getType();

    IPipeEncoder<C> getEncoder();

    IPipeDecoder<C> getDecoder();

    IPipeTransfer<C> getTransfer();

    IFilterChain<C> getFilterChain();

    ISessionCloser<C> getCloser();

    ISessionError<C> getError();

    IgnoreOperator<C> getIgnore();

    C newContext(ISessionOption option);

    ICommandFactory<C,
                    ? extends IControl<C>,
                    ? extends IFrame> getCommandFactory();
}
