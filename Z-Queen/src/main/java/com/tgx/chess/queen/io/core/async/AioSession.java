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
package com.tgx.chess.queen.io.core.async;

import static com.tgx.chess.king.base.util.IoUtil.longArrayToHex;
import static com.tgx.chess.queen.io.core.inf.IContext.SESSION_FLUSHED;
import static com.tgx.chess.queen.io.core.inf.IContext.SESSION_IDLE;
import static com.tgx.chess.queen.io.core.inf.IContext.SESSION_PENDING;
import static com.tgx.chess.queen.io.core.inf.IContext.SESSION_SENDING;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ReadPendingException;
import java.nio.channels.ShutdownChannelGroupException;
import java.nio.channels.WritePendingException;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.ArrayUtil;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IContextCreator;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.inf.ISessionOption;

/**
 * @author William.d.zk
 */
public class AioSession<C extends IContext<C>>
        extends
        LinkedList<IPacket>
        implements
        ISession<C>
{
    private static Logger _Logger = Logger.getLogger(AioSession.class.getSimpleName());
    /*--------------------------------------------------------------------------------------------------------------*/
    private final int                       _ReadTimeOutInSecond;
    private final int                       _WriteTimeOutInSecond;
    private final AsynchronousSocketChannel _Channel;
    private final InetSocketAddress         _RemoteAddress, _LocalAddress;
    /*
     * 与系统的 SocketOption 的 RecvBuffer 相等大小， 至少可以一次性将系统 Buffer 中的数据全部转存
     */
    private final ByteBuffer         _RecvBuf;
    private final C                  _Ctx;
    private final int                _HashCode;
    private final ISessionDismiss<C> _DismissCallback;
    private final int                _QueueSizeMax;
    /*----------------------------------------------------------------------------------------------------------------*/

    /*----------------------------------------------------------------------------------------------------------------*/
    private long mIndex = DEFAULT_INDEX;
    /*
     * 此处并不会进行空间初始化，完全依赖于 Context 的 WrBuf 初始化大小
     */
    private ByteBuffer mSending;
    /*
     * Session close 只能出现在 QueenManager 的工作线程中 所以关闭操作只需要做到全域线程可见即可，不需要处理写冲突
     */
    private long[] mPrefix;
    private long   mHashKey;
    private int    mWroteExpect;
    private int    mSendingBlank;

    @Override
    public String toString()
    {
        return String.format("@%#x %s->%s mode:%s Index:%#x close:%s\nwait to write %d,queue size %d",
                             _HashCode,
                             _LocalAddress,
                             _RemoteAddress,
                             _Ctx.getSort(),
                             mIndex,
                             isClosed(),
                             mSending.remaining(),
                             size());
    }

    public AioSession(final AsynchronousSocketChannel channel,
                      final ISessionOption sessionOption,
                      final IContextCreator<C> contextCreator,
                      final IConnectActivity<C> activity,
                      final ISessionDismiss<C> sessionDismiss) throws IOException
    {
        Objects.requireNonNull(sessionOption);
        Objects.requireNonNull(channel);
        Objects.requireNonNull(contextCreator);
        Objects.requireNonNull(activity);

        _Channel = channel;
        _HashCode = channel.hashCode();
        mHashKey = _HashCode;
        _RemoteAddress = (InetSocketAddress) channel.getRemoteAddress();
        _LocalAddress = (InetSocketAddress) channel.getLocalAddress();
        sessionOption.setOptions(channel);
        _ReadTimeOutInSecond = sessionOption.getReadTimeOutInSecond();
        _WriteTimeOutInSecond = sessionOption.getWriteTimeOutInSecond();
        _RecvBuf = ByteBuffer.allocate(sessionOption.getRcvInByte());
        _QueueSizeMax = sessionOption.getSendQueueMax();
        _Ctx = contextCreator.createContext(sessionOption, activity.getSort());
        mSending = _Ctx.getWrBuffer();
        mSending.flip();
        mSendingBlank = mSending.capacity() - mSending.limit();
        _DismissCallback = sessionDismiss;
    }

    @Override
    public boolean isValid()
    {
        return !isClosed();
    }

    @Override
    public InetSocketAddress getLocalAddress()
    {
        return _LocalAddress;
    }

    @Override
    public void setLocalAddress(InetSocketAddress address)
    {
        throw new UnsupportedOperationException(" final member!");
    }

    @Override
    public InetSocketAddress getRemoteAddress()
    {
        return _RemoteAddress;
    }

    @Override
    public void setRemoteAddress(InetSocketAddress address)
    {
        throw new UnsupportedOperationException(" final member!");
    }

    @Override
    public void reset()
    {
        mIndex = -1;
    }

    @Override
    public final void dispose()
    {
        _Ctx.dispose();
        mSending = null;
        clear();
        mPrefix = null;
        reset();
    }

    @Override
    public final void close() throws IOException
    {
        if (isClosed()) { return; }
        _Ctx.close();
        _Channel.close();
    }

    @Override
    public final boolean isClosed()
    {
        return _Ctx.isClosed();
    }

    @Override
    public final long getIndex()
    {
        return mIndex;
    }

    @Override
    public final void setIndex(long index)
    {
        mIndex = index;
        if (mIndex != -1L) {
            mHashKey = mIndex;
        }
    }

    @Override
    public final void bindPrefix(long prefix)
    {
        mPrefix = mPrefix == null ? new long[] { prefix }
                                  : ArrayUtil.setSortAdd(prefix, mPrefix, PREFIX_MAX);
    }

    @Override
    public long prefixLoad(long prefix)
    {
        _Logger.info("prefixLoad: %#x, %s", prefix, longArrayToHex(mPrefix));
        int pos = ArrayUtil.binarySearch0(mPrefix, prefix, PREFIX_MAX);
        if (pos < 0) {
            throw new IllegalArgumentException(String.format("prefix %#x miss, %s", prefix, longArrayToHex(mPrefix)));
        }
        return mPrefix[pos] & 0xFFFFFFFFL;
    }

    @Override
    public void prefixHit(long prefix)
    {
        _Logger.info("prefixHit: %#x, %s", prefix, longArrayToHex(mPrefix));
        int pos = ArrayUtil.binarySearch0(mPrefix, prefix, PREFIX_MAX);
        if (pos < 0) {
            throw new IllegalArgumentException(String.format("prefix %#x miss, %s", prefix, longArrayToHex(mPrefix)));
        }
        if ((mPrefix[pos] & SUFFIX_MASK) < 0xFFFFFFFFL) {
            mPrefix[pos] += 1;
        }
        else {
            mPrefix[pos] &= PREFIX_MAX;
        }
    }

    @Override
    public final long[] getPrefixArray()
    {
        return mPrefix;
    }

    @Override
    public final long getHashKey()
    {
        return mHashKey;
    }

    @Override
    public final AsynchronousSocketChannel getChannel()
    {
        return _Channel;
    }

    @Override
    public final void readNext(CompletionHandler<Integer,
                                                 ISession<C>> readHandler) throws NotYetConnectedException,
                                                                           ReadPendingException,
                                                                           ShutdownChannelGroupException
    {
        if (isClosed()) { return; }
        _RecvBuf.clear();
        _Channel.read(_RecvBuf, _ReadTimeOutInSecond, TimeUnit.SECONDS, this, readHandler);
    }

    @Override
    public final ByteBuffer read(int length)
    {
        if (length < 0) { throw new IllegalArgumentException(); }
        if (length != _RecvBuf.position()) { throw new ArrayIndexOutOfBoundsException(); }
        ByteBuffer read = ByteBuffer.allocate(length);
        _RecvBuf.flip();
        read.put(_RecvBuf);
        read.flip();
        return read;
    }

    @Override
    public C getContext()
    {
        return _Ctx;
    }

    @Override
    public WRITE_STATUS write(IPacket ps,
                              CompletionHandler<Integer,
                                                ISession<C>> handler) throws WritePendingException,
                                                                      NotYetConnectedException,
                                                                      ShutdownChannelGroupException,
                                                                      RejectedExecutionException
    {
        if (isClosed()) { return WRITE_STATUS.CLOSED; }
        ps.waitSend();
        if (size() > _QueueSizeMax) { throw new RejectedExecutionException(); }
        if (_Ctx.channelStateLessThan(SESSION_FLUSHED)) {
            // mSending 未托管给系统
            if (isEmpty()) {
                switch (writePacket(ps))
                {
                    case IGNORE:
                        return WRITE_STATUS.IGNORE;
                    case UNFINISHED:
                        //mSending 空间不足
                        offer(ps);
                    case IN_SENDING:
                        ps.send();
                }
            }
            else {
                offer(ps);
                writeBuffed2Sending();
            }
            _Ctx.advanceChannelState(SESSION_SENDING);
            flush(handler);
        }
        else {
            offer(ps);
            _Logger.info("aio event delay, session buffed packets %d", size());
        }
        return isEmpty() ? WRITE_STATUS.UNFINISHED
                         : WRITE_STATUS.IN_SENDING;
    }

    @Override
    public WRITE_STATUS writeNext(int wroteCnt,
                                  CompletionHandler<Integer,
                                                    ISession<C>> handler) throws WritePendingException,
                                                                          NotYetConnectedException,
                                                                          ShutdownChannelGroupException,
                                                                          RejectedExecutionException
    {
        if (isClosed()) { return WRITE_STATUS.CLOSED; }
        mWroteExpect -= wroteCnt;
        if (mWroteExpect == 0) {
            mSending.clear();
            mSending.flip();
            if (isEmpty()) {
                _Ctx.advanceChannelState(SESSION_IDLE);
                return WRITE_STATUS.IGNORE;
            }
            _Ctx.advanceChannelState(SESSION_PENDING);
        }
        writeBuffed2Sending();
        _Ctx.advanceChannelState(SESSION_SENDING);
        flush(handler);
        return WRITE_STATUS.FLUSHED;
    }

    private void writeBuffed2Sending()
    {
        /* 将待发的 packet 都写到 sending buffer 中，充满 sending buffer，
           不会出现无限循环，writePacket 中执行 remove 操作，由于都是在相同的线程中
           不存在线程安全问题
        */
        _Logger.info("session buffed packets %d", size());
        IPacket fps;
        Loop:
        do {
            fps = peek();
            if (Objects.nonNull(fps)) {
                switch (writePacket(fps))
                {
                    case IGNORE:
                        continue;
                    case UNFINISHED:
                        fps.send();
                        break Loop;
                    case IN_SENDING:
                        fps.sent();
                    default:
                        break;
                }
            }
        }
        while (Objects.nonNull(fps));
        _Logger.info("session remain buffed %d", size());
    }

    private WRITE_STATUS writePacket(IPacket ps)
    {
        ByteBuffer buf = ps.getBuffer();
        if (Objects.nonNull(buf) && buf.hasRemaining()) {
            mWroteExpect += buf.remaining();
            int pos = mSending.limit();
            mSendingBlank = mSending.capacity() - pos;
            int size = Math.min(mSendingBlank, buf.remaining());
            mSending.limit(pos + size);
            for (int i = 0; i < size; i++, mSendingBlank--, pos++) {
                mSending.put(pos, buf.get());
            }
            if (buf.hasRemaining()) {
                return WRITE_STATUS.UNFINISHED;
            }
            else {
                remove(ps);
                return WRITE_STATUS.IN_SENDING;
            }
        }
        else {
            remove(ps);
            return WRITE_STATUS.IGNORE;
        }
    }

    private void flush(CompletionHandler<Integer,
                                         ISession<C>> handler) throws WritePendingException,
                                                               NotYetConnectedException,
                                                               ShutdownChannelGroupException
    {
        if (_Ctx.channelStateLessThan(SESSION_FLUSHED) && mSending.hasRemaining()) {
            _Ctx.advanceChannelState(SESSION_FLUSHED);
            _Logger.info("session %#x %d,flush %d", getIndex(), getIndex(), mSending.remaining());
            _Channel.write(mSending, _WriteTimeOutInSecond, TimeUnit.SECONDS, this, handler);
        }
    }

    @Override
    public ISessionDismiss<C> getDismissCallback()
    {
        return _DismissCallback;
    }

    @Override
    public int getReadTimeOutSeconds()
    {
        return _ReadTimeOutInSecond;
    }
}
