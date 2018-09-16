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
package com.tgx.chess.queen.io.core.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.ISessionOption;

/**
 * @author William.d.zk
 */
public class AioContext
        implements
        IContext
{
    private int                 mDecodingPosition = -1, mLackData = 1;
    private final AtomicInteger _EncodeState      = new AtomicInteger(ENCODE_NULL);
    private final AtomicInteger _DecodeState      = new AtomicInteger(DECODE_NULL);
    private final AtomicInteger _ChannelState     = new AtomicInteger(SESSION_CONNECTED);

    /*
     * 用于写出的 ByteBuffer 属于4096及其倍数的对齐块，应与 SocketOption 中系统写出 Buffer 的大小进行调整，存在 一次性投递多个 ICommand 对象的可能性也是存在的 AioPacket 中的 ByteBuffer 仅用于串行化
     * ICommand 对象
     */
    private ByteBuffer          mWrBuf;

    /*
     * 用于缓存 IPoS 分块带入的 RecvBuffer 内容 由于 AioWorker 中 channel 的 read_buffer - protocol_buffer - 都以 SocketOption 设定为准，所以不存在 IPoS 带入一个包含多个分页的协议
     * 内容的情况
     */
    private ByteBuffer          mRvBuf;

    private boolean             mInitFromHandshake;

    private long                mClientStartTime;
    private long                mServerArrivedTime;
    private long                mServerResponseTime;
    private long                mClientArrivedTime;

    public AioContext(ISessionOption option) {
        mRvBuf = ByteBuffer.allocate(option.setRCV());
        mWrBuf = ByteBuffer.allocate(option.setSNF());
    }

    @Override
    public void reset() {
        if (mInitFromHandshake) handshake();
        else {
            _EncodeState.set(ctlOf(ENCODE_FRAME, 0));
            _DecodeState.set(ctlOf(DECODE_FRAME, 0));
        }
        _ChannelState.set(ctlOf(SESSION_IDLE, 0));
        mDecodingPosition = -1;
        mLackData = 1;
    }

    @Override
    public void close() throws IOException {
        advanceState(_ChannelState, SESSION_CLOSE);
    }

    @Override
    public void handshake() {
        if (stateOf(_EncodeState.get()) == ENCODE_NULL) {
            advanceState(_EncodeState, ENCODE_HANDSHAKE);
        }
        if (stateOf(_DecodeState.get()) == DECODE_NULL) {
            advanceState(_DecodeState, DECODE_HANDSHAKE);
        }
        mInitFromHandshake = true;
    }

    @Override
    public final boolean needHandshake() {
        return mInitFromHandshake;
    }

    @Override
    public void transfer() {
        advanceState(_EncodeState, ENCODE_FRAME);
        advanceState(_DecodeState, DECODE_FRAME);
    }

    @Override
    public int lackLength(int length, int target) {
        mDecodingPosition += length;
        mLackData = target - mDecodingPosition;
        return mLackData;
    }

    @Override
    public int position() {
        return mDecodingPosition;
    }

    @Override
    public int lack() {
        return mLackData;
    }

    @Override
    public void finish() {
        mDecodingPosition = -1;
        mLackData = 1;
    }

    @Override
    public int outState() {
        return stateOf(_EncodeState.get());
    }

    @Override
    public void cryptOut() {
        advanceState(_EncodeState, ENCODE_TLS);
    }

    @Override
    public IContext setOutState(int state) {
        advanceState(_EncodeState, state);
        return this;
    }

    @Override
    public int inState() {
        return stateOf(_DecodeState.get());
    }

    @Override
    public void cryptIn() {
        advanceState(_DecodeState, DECODE_TLS);
    }

    @Override
    public IContext setInState(int state) {
        advanceState(_DecodeState, state);
        return this;
    }

    @Override
    public IContext setChannelState(int state) {
        advanceState(_ChannelState, state);
        return this;
    }

    @Override
    public int getChannelState() {
        return stateOf(_ChannelState.get());
    }

    @Override
    public boolean isInConvert() {
        return isInConvert(_DecodeState.get());
    }

    @Override
    public boolean isOutConvert() {
        return isOutConvert(_EncodeState.get());
    }

    @Override
    public boolean isInErrorState() {
        return isInErrorState(_DecodeState.get());
    }

    @Override
    public boolean isOutErrorState() {
        return isOutErrorState(_EncodeState.get());
    }

    @Override
    public boolean channelStateLessThan(int targetState) {
        return stateLessThan(_ChannelState.get(), targetState);
    }

    @Override
    public void advanceChannelState(int state) {
        advanceState(_ChannelState, state);
    }

    @Override
    public ByteBuffer getWrBuffer() {
        return mWrBuf;
    }

    @Override
    public ByteBuffer getRvBuffer() {
        return mRvBuf;
    }

    @Override
    public int getSendMaxSize() {
        return mWrBuf.capacity();
    }

    @Override
    public void ntp(long clientStart, long serverArrived, long serverResponse, long clientArrived) {
        if (mClientStartTime != 0) mClientStartTime = clientStart;
        if (mServerArrivedTime != 0) mServerArrivedTime = serverArrived;
        if (mServerResponseTime != 0) mServerResponseTime = serverResponse;
        if (mClientArrivedTime != 0) mClientArrivedTime = clientArrived;
    }

    @Override
    public long getNetTransportDelay() {
        return (mClientArrivedTime - mClientStartTime - mServerResponseTime + mServerArrivedTime) >> 1;
    }

    @Override
    public long getDeltaTime() {
        return (mServerArrivedTime - mClientStartTime + mServerResponseTime - mClientArrivedTime) >> 1;
    }

    @Override
    public long getNtpArrivedTime() {
        return mServerArrivedTime;
    }

    @Override
    public boolean isInCrypt() {
        return stateAtLeast(_DecodeState.get(), DECODE_TLS);
    }

    @Override
    public boolean isOutCrypt() {
        return stateAtLeast(_EncodeState.get(), ENCODE_TLS);
    }

    @Override
    public boolean isClosed() {
        return isClosed(_ChannelState.get());
    }
}
