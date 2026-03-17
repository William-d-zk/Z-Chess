/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
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

package com.isahl.chess.bishop.protocol.spi;

import com.isahl.chess.king.base.content.ByteBuf;

/**
 * 协议处理器 SPI 接口
 * 
 * 第三方可以通过实现此接口来扩展自定义协议处理。
 * 
 * @author william.d.zk
 */
public interface ProtocolHandler
{
    
    /**
     * 获取协议名称
     * 
     * @return 协议名称
     */
    String getProtocolName();
    
    /**
     * 获取协议版本
     * 
     * @return 协议版本
     */
    String getProtocolVersion();
    
    /**
     * 获取协议标识符（用于协议识别）
     * 
     * @return 协议标识符
     */
    byte[] getProtocolSignature();
    
    /**
     * 解码协议数据
     * 
     * @param buffer 输入缓冲区
     * @return 解码后的消息对象，如果数据不完整返回 null
     */
    Object decode(ByteBuf buffer);
    
    /**
     * 编码协议数据
     * 
     * @param message 消息对象
     * @return 编码后的字节数组
     */
    byte[] encode(Object message);
    
    /**
     * 获取协议优先级（数值越小优先级越高）
     * 
     * @return 优先级
     */
    default int getPriority()
    {
        return 100;
    }
    
    /**
     * 协议处理器初始化
     */
    default void init()
    {
    }
    
    /**
     * 协议处理器销毁
     */
    default void destroy()
    {
    }
}
