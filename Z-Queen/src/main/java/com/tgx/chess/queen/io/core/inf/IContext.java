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
package com.tgx.chess.queen.io.core.inf;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import com.tgx.chess.king.base.inf.IDisposable;
import com.tgx.chess.king.base.inf.IReset;

/**
 * @author William.d.zk
 */
public interface IContext
        extends
        ITlsContext,
        IReset,
        IDisposable,
        Closeable
{
    void handshake();

    void transfer();

    boolean needHandshake();

    int lackLength(int length, int target);

    int position();

    int lack();

    void finish();

    @Override
    default int outState()
    {
        return ENCODE_FRAME;
    }

    IContext setOutState(int state);

    @Override
    default int inState()
    {
        return DECODE_FRAME;
    }

    IContext setInState(int state);

    int getChannelState();

    IContext setChannelState(int state);

    boolean channelStateLessThan(int state);

    ByteBuffer getWrBuffer();

    ByteBuffer getRvBuffer();

    int getSendMaxSize();

    long getNetTransportDelay();

    /**
     * @return negative client ahead server , otherwise client behind server
     */
    long getDeltaTime();

    void ntp(long clientStart, long serverArrived, long serverResponse, long clientArrived);

    long getNtpArrivedTime();

    /*最多支持8种状态 -3~4 */
    int COUNT_BITS       = Integer.SIZE - 3;
    int CAPACITY         = (1 << COUNT_BITS) - 1;
    int DECODE_NULL      = -2 << COUNT_BITS;
    int DECODE_HANDSHAKE = -1 << COUNT_BITS;
    int DECODE_FRAME     = 00 << COUNT_BITS;
    int DECODE_TLS       = 01 << COUNT_BITS;
    int DECODE_TLS_ERROR = 02 << COUNT_BITS;
    int DECODE_ERROR     = 03 << COUNT_BITS;

    int ENCODE_NULL      = -2 << COUNT_BITS;
    int ENCODE_HANDSHAKE = -1 << COUNT_BITS;
    int ENCODE_FRAME     = 00 << COUNT_BITS;
    int ENCODE_TLS       = 01 << COUNT_BITS;
    int ENCODE_TLS_ERROR = 02 << COUNT_BITS;
    int ENCODE_ERROR     = 03 << COUNT_BITS;

    /* 只有链接成功时才会创建 ISession 和 IContext */
    int SESSION_CONNECTED = -3 << COUNT_BITS;
    /* 处于空闲状态 */
    int SESSION_IDLE = -2 << COUNT_BITS;
    /* 有待发数据，尚未完成编码 */
    int SESSION_PENDING = -1 << COUNT_BITS;
    /* 有编码完成的数据在发送，write->wrote 事件等待 */
    int SESSION_SENDING = 00 << COUNT_BITS;
    /* 链路关闭，尚未完成清理 [any]->[close]*/
    int SESSION_CLOSE = 01 << COUNT_BITS;
    /* 终态，清理结束*/
    int SESSION_TERMINATED = 02 << COUNT_BITS;

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

    default boolean isInConvert(int c)
    {
        return stateAtLeast(c, DECODE_FRAME) && stateLessThan(c, DECODE_TLS_ERROR);
    }

    default boolean isOutConvert(int c)
    {
        return stateAtLeast(c, ENCODE_FRAME) && stateLessThan(c, ENCODE_TLS_ERROR);
    }

    boolean isInConvert();

    boolean isOutConvert();

    default boolean isInErrorState(int c)
    {
        return stateAtLeast(c, DECODE_ERROR) || inState() == DECODE_NULL;
    }

    default boolean isOutErrorState(int c)
    {
        return stateAtLeast(c, ENCODE_ERROR) | inState() == ENCODE_NULL;
    }

    boolean isInErrorState();

    boolean isOutErrorState();

    boolean isClosed();

    default void advanceState(AtomicInteger ctl, int targetState)
    {
        for (;;) {
            int c = ctl.get();
            if (stateOf(c) == targetState || ctl.compareAndSet(c, ctlOf(targetState, countOf(c)))) break;
        }
    }

    void advanceChannelState(int targetState);
}
