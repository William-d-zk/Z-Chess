/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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
package com.tgx.chess.queen.io.core.inf;

import com.tgx.chess.king.base.inf.IDisposable;

/**
 * 用于ISession 发送的指令最小单元 需要满足如下的行为特征
 * 
 * @author William.d.zk
 * @see ISession
 */
public interface IControl<C extends IContext<C>>
        extends
        IFrame,
        IQoS,
        IMappingMessage<C>,
        IDisposable
{
    @Override
    default IControl<C> setSession(ISession<C> session)
    {
        return this;
    }

    @Override
    default int superSerial()
    {
        return CONTROL_SERIAL;
    }

    @Override
    default int getPriority()
    {
        return QOS_PRIORITY_00_NETWORK_CONTROL;
    }

    /**
     * 当前 Command 是否对route-mapping 存在影响
     *
     * @return success / failed
     */
    default boolean isMapping()
    {
        return false;
    }

    default boolean isShutdown()
    {
        return false;
    }

    default IControl<C> failed(int code)
    {
        return null;
    }

    default boolean isProxy()
    {
        return false;
    }

    default <T extends IContext<T>> IControl<T>[] getProxyBodies()
    {
        return null;
    }
}
