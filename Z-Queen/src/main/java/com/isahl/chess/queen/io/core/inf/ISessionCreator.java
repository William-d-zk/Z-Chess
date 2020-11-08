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

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author William.d.zk
 */
public interface ISessionCreator
        extends
        ISessionOption
{
    /**
     * 由于继承了 ISessionOption 和 IContextCreator 所以在 create_session 入参中不再显示声明这两个参数
     * 
     * @param socketChannel
     *            已完成连接的 socket
     * @param activity
     *            连接执行器实际执行单元
     * 
     * @return session
     */
    ISession createSession(AsynchronousSocketChannel socketChannel, IConnectActivity activity) throws IOException;

    @Override
    default void configChannel(AsynchronousSocketChannel channel)
    {
        if (channel != null) {
            try {
                channel.setOption(StandardSocketOptions.TCP_NODELAY, isTcpNoDelay());
                channel.setOption(StandardSocketOptions.SO_RCVBUF, getRcvInByte());
                channel.setOption(StandardSocketOptions.SO_SNDBUF, getSnfInByte());
                channel.setOption(StandardSocketOptions.SO_KEEPALIVE, isKeepAlive());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
