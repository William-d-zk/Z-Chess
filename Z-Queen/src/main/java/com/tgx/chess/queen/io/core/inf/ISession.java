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

import java.io.Closeable;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;

import com.tgx.chess.king.base.inf.IDisposable;
import com.tgx.chess.king.base.inf.IReset;
import com.tgx.chess.king.base.inf.IValid;

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
public interface ISession<C extends IContext<C>>
        extends
        Queue<IPacket>,
        IReset,
        Closeable,
        IDisposable,
        IAddress,
        IValid,
        IReadable<ISession<C>>,
        IWritable<ISession<C>>,
        Comparable<ISession<C>>
{
    long DEFAULT_INDEX = -1;

    long getIndex();

    void setIndex(long _Index);

    long getHashKey();

    AsynchronousSocketChannel getChannel();

    C getContext();

    ISessionDismiss<C> getDismissCallback();

    long[] getPrefixArray();

    void bindPrefix(long prefix);

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
    default int compareTo(ISession<C> o)
    {
        return Long.compare(getIndex(), o.getIndex());
    }

    long PREFIX_MAX  = 0xFFFFL << 48;
    long SUFFIX_MASK = (1L << 48) - 1;

    default void innerClose()
    {
        ISessionCloser<C> closeOperator = getContext().getSort()
                                                      .getCloser();
        closeOperator.handle("inner-close", this);
        getDismissCallback().onDismiss(this);
    }
}
