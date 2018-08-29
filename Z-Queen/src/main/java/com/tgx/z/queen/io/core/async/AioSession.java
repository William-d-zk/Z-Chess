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
package com.tgx.z.queen.io.core.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ShutdownChannelGroupException;
import java.nio.channels.WritePendingException;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.tgx.z.queen.base.log.Logger;
import com.tgx.z.queen.base.util.ArrayUtil;
import com.tgx.z.queen.base.util.TimeUtil;
import com.tgx.z.queen.event.inf.IOperator;
import com.tgx.z.queen.event.operator.MODE;
import com.tgx.z.queen.io.core.inf.IConnectActive;
import com.tgx.z.queen.io.core.inf.IContext;
import com.tgx.z.queen.io.core.inf.IContextCreator;
import com.tgx.z.queen.io.core.inf.IPacket;
import com.tgx.z.queen.io.core.inf.ISession;
import com.tgx.z.queen.io.core.inf.ISessionDismiss;
import com.tgx.z.queen.io.core.inf.ISessionOption;

/**
 * @author William.d.zk
 */
public class AioSession
        extends
        LinkedList<IPacket>
        implements
        ISession
{
    static Logger                              _Log         = Logger.getLogger(AioSession.class.getSimpleName());
    /*--------------------------------------------------------------------------------------------------------------*/

    private final int                          _ReadTimeOut;
    private final int                          _HeartBeatGap;
    private final int                          _WriteTimeOut;
    private final AsynchronousSocketChannel    _Channel;
    private final InetSocketAddress            _RemoteAddress, _LocalAddress;
    /*
     * 与系统的 SocketOption 的 RecvBuffer 相等大小， 至少可以一次性将系统 Buffer 中的数据全部转存
     */
    private final ByteBuffer                   _RecvBuf;
    private final int                          _HashKey;
    private final IContext                     _Ctx;
    private final MODE                         _Mode;
    private final ISessionDismiss              _DismissCallback;
    private final IOperator<IPacket, ISession> _InOperator;
    private final int                          _QueueSizeMax;
    private final int                          _HaIndex, _PortIndex;
    /*----------------------------------------------------------------------------------------------------------------*/

    private long                               mIndex       = _DEFAULT_INDEX;
    /*
     * 此处并不会进行空间初始化，完全依赖于 Context 的 WrBuf 初始化大小
     */
    private ByteBuffer                         mSending;
    /*
     * Session close 只能出现在 QueenManager 的工作线程中 所以关闭操作只需要做到全域线程可见即可，不需要处理写冲突
     */
    private volatile boolean                   vClosed;
    private long[]                             mPortChannels;

    private int                                mWroteExpect;
    private int                                mSendingBlank;
    private int                                mWaitWrite;
    private boolean                            bWriteFinish = true;
    private long                               mReadTimeStamp;

    @Override
    public String toString() {
        return String.format("@%x %s->%s mode:%s HA:%x Port:%x Index:%x close:%s\nwait to write %d,queue size %d",
                             _HashKey,
                             _LocalAddress,
                             _RemoteAddress,
                             _Mode,
                             _HaIndex,
                             _PortIndex,
                             mIndex,
                             vClosed,
                             mWaitWrite,
                             size());
    }

    public AioSession(final AsynchronousSocketChannel channel,
                      final IConnectActive active,
                      final IContextCreator contextCreator,
                      final ISessionOption sessionOption,
                      final ISessionDismiss sessionDismiss,
                      final IOperator<IPacket, ISession> operator)
            throws IOException {
        Objects.requireNonNull(sessionOption);
        _Channel = channel;
        _Mode = active.getMode();
        _RemoteAddress = (InetSocketAddress) channel.getRemoteAddress();
        _LocalAddress = (InetSocketAddress) channel.getLocalAddress();
        _DismissCallback = sessionDismiss;
        _HashKey = channel.hashCode();
        _PortIndex = active.getPortIndex();
        _HaIndex = active.getHaIndex();
        sessionOption.setOptions(channel);
        _Ctx = contextCreator.createContext(sessionOption, _Mode);
        _ReadTimeOut = sessionOption.setReadTimeOut();
        _HeartBeatGap = (_ReadTimeOut >> 1) - 1;
        _WriteTimeOut = sessionOption.setWriteTimeOut();
        _RecvBuf = ByteBuffer.allocate(sessionOption.setRCV());
        _QueueSizeMax = sessionOption.setQueueMax();
        _InOperator = operator;
        mSending = _Ctx.getWrBuffer();
        mSending.flip();
        mSendingBlank = mSending.capacity() - mSending.limit();
    }

    @Override
    public boolean isValid() {
        return !isClosed();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return _LocalAddress;
    }

    @Override
    public void setLocalAddress(InetSocketAddress address) {
        throw new UnsupportedOperationException(" final member!");
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return _RemoteAddress;
    }

    @Override
    public void setRemoteAddress(InetSocketAddress address) {
        throw new UnsupportedOperationException(" final member!");
    }

    @Override
    public int getHaIndex() {
        return _HaIndex;
    }

    @Override
    public int getPortIndex() {
        return _PortIndex;
    }

    @Override
    public void reset() {
        mIndex = -1;
    }

    @Override
    public final void dispose() {
        _Ctx.dispose();
        mSending = null;
        clear();
        mPortChannels = null;
        reset();
    }

    @Override
    public final void close() throws IOException {
        if (isClosed()) return;
        vClosed = true;
        _Ctx.close();
        _Channel.close();
    }

    @Override
    public final boolean isClosed() {
        return vClosed;
    }

    @Override
    public final long getIndex() {
        return mIndex;
    }

    @Override
    public final void setIndex(long index) {
        this.mIndex = index;
    }

    @Override
    public final void bindport2channel(long channelport) {
        mPortChannels = mPortChannels == null ? new long[] { channelport } : ArrayUtil.setSortAdd(channelport, mPortChannels);
    }

    @Override
    public final long[] getPortChannels() {
        return mPortChannels;
    }

    @Override
    public final int getHashKey() {
        return _HashKey;
    }

    @Override
    public final AsynchronousSocketChannel getChannel() {
        return _Channel;
    }

    @Override
    public final int getReadTimeOut() {
        return _ReadTimeOut;
    }

    @Override
    public final int getHeartBeatSap() {
        return _HeartBeatGap;
    }

    @Override
    public final void readNext(CompletionHandler<Integer, ISession> readHandler) throws NotYetConnectedException,
                                                                                 ShutdownChannelGroupException {
        if (isClosed()) return;
        _RecvBuf.clear();
        _Channel.read(_RecvBuf, _ReadTimeOut, TimeUnit.SECONDS, this, readHandler);
    }

    public final ByteBuffer read(int length) {
        mReadTimeStamp = TimeUtil.CURRENT_TIME_MILLIS_CACHE;
        if (length < 0) throw new IllegalArgumentException();
        if (length != _RecvBuf.position()) throw new ArrayIndexOutOfBoundsException();
        ByteBuffer read = ByteBuffer.allocate(length);
        _RecvBuf.flip();
        read.put(_RecvBuf);
        read.flip();
        return read;
    }

    @Override
    public long nextBeat() {
        long delta = TimeUtil.CURRENT_TIME_MILLIS_CACHE - mReadTimeStamp;
        return delta >= TimeUnit.SECONDS.toMillis(_HeartBeatGap) ? -1 : TimeUnit.SECONDS.toMillis(_HeartBeatGap) - delta;
    }

    @Override
    public IOperator<IPacket, ISession> getDecodeOperator() {
        return _InOperator;
    }

    public IContext getContext() {
        return _Ctx;
    }

    @Override
    public WRITE_STATUS write(IPacket ps, CompletionHandler<Integer, ISession> handler) throws WritePendingException,
                                                                                        NotYetConnectedException,
                                                                                        ShutdownChannelGroupException,
                                                                                        RejectedExecutionException {
        if (isClosed()) return WRITE_STATUS.CLOSED;
        ps.waitSend();
        if (isEmpty()) {
            WRITE_STATUS status = writeChannel(ps);
            switch (status) {
                case IGNORE:
                    return status;
                case UNFINISHED:
                    offer(ps);
                case IN_SENDING:
                    ps.send();
                default:
                    if (mWaitWrite > 0 && bWriteFinish) flush(handler);
                    return status;
            }
        }
        else if (size() > _QueueSizeMax) throw new RejectedExecutionException();
        else {
            IPacket fps = peek();
            switch (writeChannel(fps)) {
                case IGNORE:
                    break;
                case UNFINISHED:
                case IN_SENDING:
                    fps.send();
                default:
                    if (mWaitWrite > 0 && bWriteFinish) flush(handler);
                    break;
            }
            offer(ps);
            return WRITE_STATUS.UNFINISHED;
        }
    }

    @Override
    public WRITE_STATUS writeNext(int wroteCnt, CompletionHandler<Integer, ISession> handler) throws WritePendingException,
                                                                                              NotYetConnectedException,
                                                                                              ShutdownChannelGroupException,
                                                                                              RejectedExecutionException {
        if (isClosed()) { return WRITE_STATUS.CLOSED; }
        mWroteExpect -= wroteCnt;
        bWriteFinish = true;
        if (mWroteExpect == 0) {
            mSending.clear();
            mSending.flip();
        }
        if (isEmpty()) { return WRITE_STATUS.IGNORE; }
        IPacket fps;
        Loop:
        do {
            fps = peek();
            if (Objects.nonNull(fps)) {
                switch (writeChannel(fps)) {
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
        while (Objects.isNull(fps));
        if (mWaitWrite > 0) {
            flush(handler);
            return WRITE_STATUS.FLUSHED;
        }
        return WRITE_STATUS.IGNORE;
    }

    private WRITE_STATUS writeChannel(IPacket ps) {
        ByteBuffer buf = ps.getBuffer();
        if (buf != null && buf.hasRemaining()) {
            mWroteExpect += buf.remaining();
            mWaitWrite = mSending.remaining();
            mSendingBlank = mSending.capacity() - mSending.limit();
            int pos = mSending.limit();
            int size = Math.min(mSendingBlank, buf.remaining());
            mSending.limit(pos + size);
            for (int i = 0; i < size; i++, mSendingBlank--, mWaitWrite++, pos++)
                mSending.put(pos, buf.get());
            if (buf.hasRemaining()) return WRITE_STATUS.UNFINISHED;
            else {
                remove(ps);
                return WRITE_STATUS.IN_SENDING;
            }
        }
        else remove(ps);
        return WRITE_STATUS.IGNORE;
    }

    private void flush(CompletionHandler<Integer, ISession> handler) throws WritePendingException,
                                                                     NotYetConnectedException,
                                                                     ShutdownChannelGroupException {
        bWriteFinish = false;
        _Channel.write(mSending, _WriteTimeOut, TimeUnit.SECONDS, this, handler);
    }

    @Override
    public boolean isWroteFinish() {
        return bWriteFinish;
    }

    @Override
    public MODE getMode() {
        return _Mode;
    }

    @Override
    public ISessionDismiss getDismissCallback() {
        return _DismissCallback;
    }

}
