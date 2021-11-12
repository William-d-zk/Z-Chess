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

package com.isahl.chess.queen.io.core.features.model.session;

import com.isahl.chess.king.base.features.IDisposable;
import com.isahl.chess.king.base.features.IReset;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.queen.io.core.features.model.channels.IAddress;
import com.isahl.chess.queen.io.core.features.model.channels.IConnectMode;
import com.isahl.chess.queen.io.core.features.model.channels.IReadable;
import com.isahl.chess.queen.io.core.features.model.channels.IWritable;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilterChain;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeDecoder;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeEncoder;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeTransfer;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IPContext;

import java.io.Closeable;
import java.nio.channels.Channel;
import java.util.Queue;

/**
 * session index 以 H16Bit 为限 提供port-channel聚合能力,
 * 所以Prefix 0x1<<48~0xFFFF<<48 需注意java 没有unsigned-long
 * 负值是有效值，只有 0 是无效值 L32Bit 记录通过聚合方式 send 的次数
 * 用于实现channel的load balance，需注意balance仅考虑了在当前port下
 * 并不关注session的全局负载，当多个channel共用session 时 balance
 * 需考虑Queue.size的作为指标进行balance的实现
 *
 * @author William.d.zk
 */
public interface ISession
        extends Queue<IPacket>,
                IReset,
                Closeable,
                IDisposable,
                IAddress,
                IValid,
                IConnectMode,
                IReadable<ISession>,
                IWritable<ISession>,
                Comparable<ISession>
{
    boolean isMultiBind();

    long[] getBindIndex();

    void bindIndex(long index);

    void unbindIndex(long index);

    long getIndex();

    void setIndex(long index);

    Channel getChannel();

    <T extends IPContext> T getContext();

    <T extends IPContext> T getContext(Class<T> clazz);

    IDismiss getDismissCallback();

    long[] getPrefixArray();

    void bindPrefix(long prefix);

    default String summary()
    {
        return String.format("%s->%s,closed:%s", getLocalAddress(), getReadTimeOutSeconds(), isClosed());
    }

    boolean isClosed();

    int getReadTimeOutSeconds();

    /**
     * 获取 prefix 对应的 write load
     *
     * @param prefix
     * @return
     */
    long prefixLoad(long prefix);

    /**
     * 增加被选中prefix hit的记录
     *
     * @param prefix
     */
    void prefixHit(long prefix);

    @Override
    default int compareTo(ISession o)
    {
        return Long.compare(getIndex(), o.getIndex());
    }

    long PREFIX_MAX  = 0xFFFFL << 48;
    long SUFFIX_MASK = (1L << 48) - 1;

    default void innerClose()
    {
        ICloser closeOperator = getCloser();
        closeOperator.handle("inner-close", this);
        getDismissCallback().onDismiss(this);
    }

    /* 最多支持8种状态 -3~4 */ int COUNT_BITS         = Integer.SIZE - 3;
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
        return switch(c) {
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

    static boolean isConnected(int c)
    {
        return c < SESSION_CLOSE;
    }

    static boolean isClosed(int c)
    {
        return c >= SESSION_CLOSE;
    }

    IFailed getError();

    ICloser getCloser();

    IPipeEncoder getEncoder();

    IPipeDecoder getDecoder();

    IPipeTransfer getTransfer();

    IFilterChain getFilterChain();

    void ready();
}
