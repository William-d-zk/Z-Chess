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
import java.nio.ByteBuffer;

import com.tgx.z.queen.io.core.inf.IContext;
import com.tgx.z.queen.io.core.inf.ISessionOption;

/**
 * @author William.d.zk
 */
public class AioContext
        implements
        IContext
{
    private int          mDecodingPosition = -1, mLackData = 1;
    private EncodeState  mEState           = EncodeState.NULL;
    private DecodeState  mDState           = DecodeState.NULL;
    private ChannelState mCState           = ChannelState.CLOSED;

    /*
     * 用于写出的 ByteBuffer 属于4096及其倍数的对齐块，应与 SocketOption 中系统写出 Buffer 的大小进行调整，存在 一次性投递多个 ICommand 对象的可能性也是存在的 AioPacket 中的 ByteBuffer 仅用于串行化
     * ICommand 对象
     */
    private ByteBuffer   mWrBuf;

    /*
     * 用于缓存 IPoS 分块带入的 RecvBuffer 内容 由于 AioWorker 中 channel 的 read_buffer - protocol_buffer - 都以 SocketOption 设定为准，所以不存在 IPoS 带入一个包含多个分页的协议
     * 内容的情况
     */
    private ByteBuffer   mRvBuf;

    private boolean      mInitFromHandshake;

    private long         mClientStartTime;
    private long         mServerArrivedTime;
    private long         mServerResponseTime;
    private long         mClientArrivedTime;

    public AioContext(ISessionOption option) {
        mRvBuf = ByteBuffer.allocate(option.setRCV());
        mWrBuf = ByteBuffer.allocate(option.setSNF());
    }

    @Override
    public void reset() {
        if (mInitFromHandshake) handshake();
        else {
            mEState = EncodeState.NULL;
            mDState = DecodeState.NULL;
        }
        mCState = ChannelState.CLOSED;
        mDecodingPosition = -1;
        mLackData = 1;
    }

    @Override
    public void close() throws IOException {
        mCState = ChannelState.CLOSED;
    }

    @Override
    public void handshake() {
        mEState = mEState == EncodeState.NULL ? EncodeState.ENCODE_HANDSHAKE : mEState;
        mDState = mDState == DecodeState.NULL ? DecodeState.DECODE_HANDSHAKE : mDState;
        mInitFromHandshake = true;
    }

    @Override
    public final boolean hasHandshake() {
        return mInitFromHandshake;
    }

    @Override
    public void transfer() {
        mEState = EncodeState.ENCODING_FRAME;
        mDState = DecodeState.DECODING_FRAME;
    }

    @Override
    public void tls() {
        mEState = EncodeState.ENCODED_TLS;
        mDState = DecodeState.DECODED_TLS;
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
    public IContext setEncodeState(EncodeState state) {
        mEState = state;
        return this;
    }

    @Override
    public IContext setDecodeState(DecodeState state) {
        mDState = state;
        return this;
    }

    @Override
    public IContext setChannelState(ChannelState state) {
        mCState = state;
        return this;
    }

    @Override
    public EncodeState getEncodeState() {
        return mEState;
    }

    @Override
    public DecodeState getDecodeState() {
        return mDState;
    }

    @Override
    public ChannelState getChannelState() {
        return mCState;
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
    public boolean isCrypt() {
        return outState().equals(EncryptState.ENCRYPTED);
    }
}
