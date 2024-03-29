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
package com.isahl.chess.queen.io.core.net.socket.features.server;

import com.isahl.chess.king.base.disruptor.features.functions.OperateType;
import com.isahl.chess.queen.io.core.net.socket.AioWorker;
import com.isahl.chess.queen.io.core.net.socket.features.IAioConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author William.d.zk
 */
public interface IAioServer
        extends IAioConnection,
                CompletionHandler<AsynchronousSocketChannel, IAioServer>
{

    void bindAddress(InetSocketAddress address, AsynchronousChannelGroup channelGroup) throws IOException;

    void pendingAccept();

    @Override
    default void completed(AsynchronousSocketChannel channel, IAioServer server)
    {
        AioWorker worker = (AioWorker) Thread.currentThread();
        worker.publishConnected(server.getConnectedOperator(), server, OperateType.ACCEPTED, channel);
        server.pendingAccept();
    }

    @Override
    default void failed(Throwable exc, IAioServer server)
    {
        AioWorker worker = (AioWorker) Thread.currentThread();
        worker.publishAcceptError(server.getErrorOperator(), exc, server);
        server.pendingAccept();
    }

}
