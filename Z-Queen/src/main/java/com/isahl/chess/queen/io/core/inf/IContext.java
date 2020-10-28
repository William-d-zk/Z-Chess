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

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import com.isahl.chess.king.base.inf.IDisposable;
import com.isahl.chess.king.base.inf.IReset;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.inf.IValid;
import com.isahl.chess.queen.event.inf.IOperator;

/**
 * @author William.d.zk
 */
public interface IContext
        extends
        IReset,
        IDisposable,
        Closeable,
        IValid,
        IConnectMode
{
    ByteBuffer getWrBuffer();

    ByteBuffer getRvBuffer();

    int getSessionState();

    void setSessionState(int state);

    boolean sessionStateLessThan(int state);

    int getSendMaxSize();

    long getNetTransportDelay();

    /**
     * @return negative client ahead server , otherwise client behind server
     */
    long getDeltaTime();

    void ntp(long clientStart, long serverArrived, long serverResponse, long clientArrived);

    long getNtpArrivedTime();

    /* 最多支持8种状态 -3~4 */
    int COUNT_BITS         = Integer.SIZE - 3;
    int CAPACITY           = (1 << COUNT_BITS) - 1;
    int SESSION_CREATED    = -3 << COUNT_BITS;
    int SESSION_CONNECTED  = -2 << COUNT_BITS;     /* 只有链接成功时才会创建 ISession 和 IContext */
    int SESSION_IDLE       = -1 << COUNT_BITS;     /* 处于空闲状态 */
    int SESSION_PENDING    = 00 << COUNT_BITS;     /* 有待发数据，尚未完成编码 */
    int SESSION_SENDING    = 01 << COUNT_BITS;     /* 有编码完成的数据在发送，已装入待发sending-buffer */
    int SESSION_FLUSHED    = 02 << COUNT_BITS;     /* 有编码完成的数据在发送，write->wrote 事件等待 */
    int SESSION_CLOSE      = 03 << COUNT_BITS;     /* 链路关闭，尚未完成清理 [any]->[close] */
    int SESSION_TERMINATED = 04 << COUNT_BITS;     /* 终态，清理结束 */

    default String getSessionStateStr(int c)
    {
        return switch (c)
        {
            case SESSION_CREATED -> "SESSION_CREATED";
            case SESSION_CONNECTED -> "SESSION_CONNECTED";
            case SESSION_IDLE -> "SESSION_IDLE";
            case SESSION_PENDING -> "SESSION_PENDING";
            case SESSION_SENDING -> "SESSION_SENDING";
            case SESSION_FLUSHED -> "SESSION_FLUSHED";
            case SESSION_CLOSE -> "SESSION_CLOSE";
            case SESSION_TERMINATED -> "SESSION_TERMINATED";
            default -> "UNKNOWN";
        };
    }

    default int stateOf(int c)
    {
        return c & ~CAPACITY;
    }

    default int countOf(int c)
    {
        return c & CAPACITY;
    }

    default int ctlOf(int rs, int wc)
    {
        return rs | wc;
    }

    default boolean stateLessThan(int c, int s)
    {
        return c < s;
    }

    default boolean stateAtLeast(int c, int s)
    {
        return c >= s;
    }

    default boolean isConnected(int c)
    {
        return c < SESSION_CLOSE;
    }

    default boolean isClosed(int c)
    {
        return c >= SESSION_CLOSE;
    }

    boolean isClosed();

    default void advanceState(AtomicInteger ctl, int targetState)
    {
        for (;;) {
            int c = ctl.get();
            if (stateOf(c) == targetState || ctl.compareAndSet(c, ctlOf(targetState, countOf(c)))) break;
        }
    }

    void advanceChannelState(int targetState);

    ISessionError getError();

    ISessionCloser getCloser();

    IOperator<IPacket,
              ISession,
              ITriple> getReader();
}
