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
package com.isahl.chess.queen.io.core.net.socket;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.ArrayUtil;
import com.isahl.chess.queen.io.core.features.model.channels.IConnectActivity;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilterChain;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeDecoder;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeEncoder;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeTransfer;
import com.isahl.chess.queen.io.core.features.model.session.*;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IProxyContext;
import com.isahl.chess.queen.io.core.features.model.session.ssl.ISslOption;
import com.isahl.chess.queen.io.core.net.socket.features.IAioSort;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.isahl.chess.king.base.cron.features.ITask.*;
import static com.isahl.chess.king.base.util.IoUtil.longArrayToHex;
import static com.isahl.chess.queen.io.core.features.model.session.IManager.INVALID_INDEX;
import static com.isahl.chess.queen.io.core.features.model.session.IManager.NULL_INDEX;

/**
 * @author William.d.zk
 */
public class AioSession<C extends IPContext>
        extends LinkedList<IPacket>
        implements ISession
{
    private final Logger                    _Logger = Logger.getLogger(
            "io.queen.session." + getClass().getSimpleName());
    /*--------------------------------------------------------------------------------------------------------------*/
    private final int                       _ReadTimeOutInSecond;
    private final int                       _WriteTimeOutInSecond;
    private final AsynchronousSocketChannel _Channel;
    private final InetSocketAddress         _RemoteAddress, _LocalAddress;
    /*
     * 与系统的 SocketOption 的 RecvBuffer 相等大小， 至少可以一次性将系统 Buffer 中的数据全部转存
     */
    private final ByteBuffer      _RecvBuf;
    private final C               _Context;
    private final int      _HashCode;
    private final IDismiss _DismissCallback;
    private final int      _QueueSizeMax;
    private final IAioSort<C>     _Sort;
    private final AtomicInteger   _State = new AtomicInteger(SESSION_CREATED);
    private final boolean         _MultiBind;
    /*----------------------------------------------------------------------------------------------------------------*/

    /*----------------------------------------------------------------------------------------------------------------*/
    private long                                 mIndex = INVALID_INDEX;
    private long[]                               mBindIndex;
    /*
     * 此处并不会进行空间初始化，完全依赖于 Context 的 Wrbuf 实体，仅作为reference
     */
    private ByteBuffer                           mSending;
    /*
     * Session close 只能出现在 QueenManager 的工作线程中 所以关闭操作只需要做到全域线程可见即可，不需要处理写冲突
     */
    private long[]                               mPrefix;
    private int                                  mWroteExpect;
    private int                                  mSendingBlank;
    /* reader */
    private CompletionHandler<Integer, ISession> mReader;

    @Override
    public String toString()
    {
        return String.format("@%#x %s->%s index:%#x valid:%s wait_to_write %d queue_size %d",
                             _HashCode,
                             _LocalAddress,
                             _RemoteAddress,
                             mIndex,
                             isValid(),
                             mSending.remaining(),
                             size());
    }

    public AioSession(AsynchronousSocketChannel channel,
                      ISslOption option,
                      IAioSort<C> sort,
                      IConnectActivity activity,
                      IDismiss sessionDismiss,
                      boolean multiBind) throws IOException
    {
        Objects.requireNonNull(option);
        Objects.requireNonNull(channel);
        Objects.requireNonNull(activity);
        Objects.requireNonNull(sort);
        //------------------------------------------------------------
        _MultiBind = multiBind;
        _Channel = channel;
        _HashCode = channel.hashCode();
        _RemoteAddress = (InetSocketAddress) channel.getRemoteAddress();
        _LocalAddress = (InetSocketAddress) channel.getLocalAddress();
        _DismissCallback = sessionDismiss;
        _ReadTimeOutInSecond = option.getReadTimeOutInSecond();
        _WriteTimeOutInSecond = option.getWriteTimeOutInSecond();
        _RecvBuf = ByteBuffer.allocate(option.getRcvByte());
        _QueueSizeMax = option.getSendQueueMax();
        _Sort = sort;
        _Context = sort.newContext(option);
        //------------------------------------------------------------
        option.configChannel(channel);
        mIndex = INVALID_INDEX;
        mSending = _Context.getWrBuffer();
        mSending.flip();
        mSendingBlank = mSending.capacity() - mSending.limit();
    }

    @Override
    public boolean isValid()
    {
        return !isClosed();
    }

    @Override
    public boolean isInvalid()
    {
        return isClosed();
    }

    @Override
    public boolean isMultiBind()
    {
        return _MultiBind;
    }

    @Override
    public InetSocketAddress getLocalAddress()
    {
        return _LocalAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress()
    {
        return _RemoteAddress;
    }

    @Override
    public void reset()
    {
        mIndex = INVALID_INDEX;
    }

    @Override
    public final void dispose()
    {
        clear();
        _Context.dispose();
        mReader = null;
        mSending = null;
        mPrefix = null;
        reset();
    }

    @Override
    public final void close() throws IOException
    {
        if(isClosed()) {return;}
        advanceState(_State, SESSION_CLOSE, CAPACITY);
        try {
            _Context.dispose();
        }
        finally {
            if(_Channel != null) {
                _Channel.close();
            }
        }
    }

    @Override
    public final boolean isClosed()
    {
        return stateAtLeast(_State.get(), SESSION_CLOSE);
    }

    @Override
    public final long getIndex()
    {
        return mIndex;
    }

    @Override
    public long[] getBindIndex()
    {
        return mBindIndex;
    }

    @Override
    public final void setIndex(long index)
    {
        mIndex = index;
    }

    @Override
    public void bindIndex(long index)
    {
        if(index != INVALID_INDEX && index != NULL_INDEX) {
            mBindIndex = ArrayUtil.setSortAdd(index, mBindIndex);
        }
    }

    @Override
    public void unbindIndex(long index)
    {
        if(index != INVALID_INDEX && index != NULL_INDEX) {
            mBindIndex = ArrayUtil.setNoZeroSortRm(index, mBindIndex);
        }
    }

    @Override
    public final void bindPrefix(long prefix)
    {
        mPrefix = mPrefix == null ? new long[]{ prefix } : ArrayUtil.setSortAdd(prefix, mPrefix, PREFIX_MAX);
    }

    @Override
    public long prefixLoad(long prefix)
    {
        _Logger.debug("prefixLoad: %#x, %s", prefix, longArrayToHex(mPrefix));
        int pos = ArrayUtil.binarySearch0(mPrefix, prefix, PREFIX_MAX);
        if(pos < 0) {
            throw new IllegalArgumentException(String.format("prefix %#x miss, %s", prefix, longArrayToHex(mPrefix)));
        }
        return mPrefix[pos] & 0xFFFFFFFFL;
    }

    @Override
    public void prefixHit(long prefix)
    {
        _Logger.debug("prefixHit: %#x, %s", prefix, longArrayToHex(mPrefix));
        int pos = ArrayUtil.binarySearch0(mPrefix, prefix, PREFIX_MAX);
        if(pos < 0) {
            throw new IllegalArgumentException(String.format("prefix %#x miss, %s", prefix, longArrayToHex(mPrefix)));
        }
        if((mPrefix[pos] & SUFFIX_MASK) < 0xFFFFFFFFL) {
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
    public final Channel getChannel()
    {
        return _Channel;
    }

    @Override
    public final void readNext(CompletionHandler<Integer, ISession> readHandler) throws NotYetConnectedException, ReadPendingException, ShutdownChannelGroupException
    {
        if(isClosed()) {return;}
        mReader = readHandler;
        _RecvBuf.clear();
        _Channel.read(_RecvBuf, _ReadTimeOutInSecond, TimeUnit.SECONDS, this, readHandler);
    }

    @Override
    public final void readNext() throws IllegalStateException
    {
        readNext(mReader);
    }

    @Override
    public final ByteBuffer read(int length)
    {
        if(length < 0) {throw new IllegalArgumentException();}
        if(length != _RecvBuf.position()) {throw new ArrayIndexOutOfBoundsException();}
        ByteBuffer read = ByteBuffer.allocate(length);
        _RecvBuf.flip();
        read.put(_RecvBuf);
        read.flip();
        return read;
    }

    @Override
    public C getContext()
    {
        return _Context;
    }

    @Override
    public <T extends IPContext> T getContext(Class<T> clazz)
    {
        if(_Context.getClass() == clazz) {
            return (T) _Context;
        }
        else {
            IPContext context = _Context;
            while(context.isProxy()) {
                context = ((IProxyContext<?>) context).getActingContext();
                if(context.getClass() == clazz) {return (T) context;}
            }
            throw new ZException("not found context instanceof %s", clazz.getSimpleName());
        }
    }

    @Override
    public WRITE_STATUS write(IPacket ps,
                              CompletionHandler<Integer, ISession> handler) throws WritePendingException, NotYetConnectedException, ShutdownChannelGroupException, RejectedExecutionException
    {
        if(isClosed()) {return WRITE_STATUS.CLOSED;}
        ps.waitSend();
        if(size() > _QueueSizeMax) {throw new RejectedExecutionException();}
        if(stateLessThan(_State.get(), SESSION_FLUSHED)) {
            // mSending 未托管给系统
            if(isEmpty()) {
                switch(writePacket(ps)) {
                    case IGNORE:
                        return WRITE_STATUS.IGNORE;
                    case UNFINISHED:
                        // mSending 空间不足
                        offer(ps);
                    case IN_SENDING:
                        ps.send();
                }
            }
            else {
                offer(ps);
                writeBuffed2Sending();
            }
            advanceState(_State, SESSION_SENDING, CAPACITY);
            flush(handler);
        }
        else {
            offer(ps);
            _Logger.debug("aio event delay, session buffed packets %d", size());
        }
        return isEmpty() ? WRITE_STATUS.UNFINISHED : WRITE_STATUS.IN_SENDING;
    }

    @Override
    public WRITE_STATUS writeNext(int wroteCnt,
                                  CompletionHandler<Integer, ISession> handler) throws WritePendingException, NotYetConnectedException, ShutdownChannelGroupException, RejectedExecutionException
    {
        if(isClosed()) {return WRITE_STATUS.CLOSED;}
        mWroteExpect -= wroteCnt;
        if(mWroteExpect == 0) {
            mSending.clear();
            mSending.flip();
            if(isEmpty()) {
                recedeState(_State, SESSION_IDLE, CAPACITY);
                return WRITE_STATUS.IGNORE;
            }
            recedeState(_State, SESSION_PENDING, CAPACITY);
        }
        writeBuffed2Sending();
        advanceState(_State, SESSION_SENDING, CAPACITY);
        flush(handler);
        return WRITE_STATUS.FLUSHED;
    }

    private void writeBuffed2Sending()
    {
        /*
         * 将待发的 packet 都写到 sending buffer 中，充满 sending buffer，
         * 不会出现无限循环，writePacket 中执行 remove 操作，由于都是在相同的线程中
         * 不存在线程安全问题
         */
        _Logger.debug("session buffed packets %d", size());
        IPacket fps;
        Loop:
        do {
            fps = peek();
            if(Objects.nonNull(fps)) {
                switch(writePacket(fps)) {
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
        while(Objects.nonNull(fps));
        _Logger.debug("session remain buffed %d", size());
    }

    private WRITE_STATUS writePacket(IPacket ps)
    {
        ByteBuffer buf = ps.getBuffer();
        if(Objects.nonNull(buf) && buf.hasRemaining()) {
            int pos = mSending.limit();
            mSendingBlank = mSending.capacity() - pos;
            int size = Math.min(mSendingBlank, buf.remaining());
            mWroteExpect += size;
            mSending.limit(pos + size);
            for(int i = 0; i < size; i++, mSendingBlank--, pos++) {
                mSending.put(pos, buf.get());
            }
            if(buf.hasRemaining()) {
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

    private void flush(CompletionHandler<Integer, ISession> handler) throws WritePendingException, NotYetConnectedException, ShutdownChannelGroupException
    {
        if(stateLessThan(_State.get(), SESSION_FLUSHED) && mSending.hasRemaining()) {
            _Logger.debug("flush %d | %s", mSending.remaining(), this);
            _Channel.write(mSending, _WriteTimeOutInSecond, TimeUnit.SECONDS, this, handler);
            advanceState(_State, SESSION_FLUSHED, CAPACITY);
        }
    }

    @Override
    public IDismiss getDismissCallback()
    {
        return _DismissCallback;
    }

    @Override
    public int getReadTimeOutSeconds()
    {
        return _ReadTimeOutInSecond;
    }

    @Override
    public IFailed getError()
    {
        return _Sort.getError();
    }

    @Override
    public ICloser getCloser()
    {
        return _Sort.getCloser();
    }

    @Override
    public IPipeEncoder getEncoder()
    {
        return _Sort.getEncoder();
    }

    @Override
    public IPipeDecoder getDecoder()
    {
        return _Sort.getDecoder();
    }

    @Override
    public IPipeTransfer getTransfer()
    {
        return _Sort.getTransfer();
    }

    @Override
    public IFilterChain getFilterChain()
    {
        return _Sort.getFilterChain();
    }

    @Override
    public ISort.Mode getMode()
    {
        return _Sort.getMode();
    }

    @Override
    public void ready()
    {
        advanceState(_State, SESSION_CONNECTED, CAPACITY);
        _Context.ready();
    }

    @Override
    public boolean equals(Object o)
    {
        return this == o;
    }

    @Override
    public int hashCode()
    {
        return _HashCode;
    }
}
