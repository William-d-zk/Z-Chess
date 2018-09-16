/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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

import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author William.d.zk
 */
public interface ISessionOption
{
    int SO_TCP_MTU      = 1500 - 40;
    int SO_TCP_BIG_MTU  = 9000 - 40;
    int SO_TCP_HUGE_MTU = 64000 - 40;
    int INC_RECV_SIZE   = 1 << 12;
    int INC_SEND_SIZE   = 1 << 12;
    int INC_QUEUE_SIZE  = 64;

    void setOptions(AsynchronousSocketChannel channel);

    int setSNF();

    int setRCV();

    int setQueueMax();

    int setReadTimeOut();

    int setWriteTimeOut();

    boolean setKeepAlive();

    default String getConfigName() {
        return "SocketOption";
    }

    default String getConfigGroup() {
        return "socket";
    }
}
