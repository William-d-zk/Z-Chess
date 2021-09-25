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

import com.isahl.chess.queen.io.core.features.model.session.IContext;
import com.isahl.chess.queen.io.core.features.model.channels.IRespConnected;

/**
 * @author william.d.zk
 * 
 * @date 2019-05-17
 */
public interface ICommand
        extends IControl,
        IStreamProtocol
{
    @Override
    default int superSerial()
    {
        return COMMAND_SERIAL;
    }

    long getMsgId();

    void setMsgId(long msgId);

    /**
     * 核心方法
     * 根据命令字 创建具体的通讯指令
     * 通讯指令 必须是 IRouteLv4 & IStreamProtocol & IControl 的子集
     * IControl 作为 ISession 的最基本的指令单元，严格化的支持 Route 和 Stream 处理
     *
     * @author william.d.zk
     * @see IRespConnected
     */
    interface Factory<T extends IProtocol, I extends IFrame, C extends IContext>
    {
        T create(I frame, C context);

        T create(int serial, byte[] payload, C context);
    }
}