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
package com.isahl.chess.queen.io.core.inf;

/**
 * stream protocol 存在stream control的状态更新机制
 * 状态机由 IContext 进行管理。
 * 
 * @see IContext
 * 
 * @author William.d.zk
 */
public interface IStreamProtocol
        extends
        IProtocol
{
    /**
     * 编码结束后 更新状态机状态
     * 
     * @param ctx
     */
    default <C extends IPContext> void afterEncode(C ctx)
    {
    }

    /**
     * 解码结束后更新状态机状态
     * 
     * @param ctx
     */
    default <C extends IPContext> void afterDecode(C ctx)
    {
    }

    /**
     * 扩展IProtocol 未对IContext进行约定
     * 编码过程
     * 
     * @param ctx
     * 
     * @return
     */
    default <C extends IPContext> byte[] encode(C ctx)
    {
        byte[] data = encode();
        afterEncode(ctx);
        return data;
    }

    /**
     * 扩展IProtocol 未对IContext进行约定
     * 解码过程
     * 
     * @param data
     * @param ctx
     */
    default <C extends IPContext> void decode(byte[] data, C ctx)
    {
        decode(data);
        afterDecode(ctx);
    }
}
