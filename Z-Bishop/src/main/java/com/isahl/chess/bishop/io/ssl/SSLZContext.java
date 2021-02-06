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

package com.isahl.chess.bishop.io.ssl;

import static com.isahl.chess.king.base.schedule.inf.ITask.advanceState;
import static com.isahl.chess.queen.io.core.inf.ISession.CAPACITY;

import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import com.isahl.chess.bishop.io.ZContext;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.event.inf.ISort;
import com.isahl.chess.queen.io.core.inf.IEContext;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IPacket;
import com.isahl.chess.queen.io.core.inf.IProxyContext;
import com.isahl.chess.queen.io.core.inf.ISessionOption;

/**
 * @author william.d.zk
 */
public class SSLZContext<A extends IPContext>
        extends
        ZContext
        implements
        IEContext,
        IProxyContext<A>
{
    private final static Logger LOGGER = Logger.getLogger(SSLZContext.class.getSimpleName());
    private final SSLEngine     _SslEngine;
    private final SSLContext    _SslContext;
    private final SSLSession    _SslSession;
    private final ByteBuffer    _AppInBuffer;
    private final A             _ActingContext;

    private boolean mUpdateKeyIn, mUpdateKeyOut;

    public SSLZContext(ISessionOption option,
                       ISort.Mode mode,
                       ISort.Type type,
                       A acting) throws NoSuchAlgorithmException
    {
        super(option, mode, type);
        _ActingContext = acting;
        _SslContext = SSLContext.getInstance("TLSv1.2");
        try {
            _SslContext.init(option.getKeyManagers(), option.getTrustManagers(), null);
        }
        catch (KeyManagementException e) {
            LOGGER.fetal("ssl context init failed", e);
            throw new ZException(e, "ssl context init failed");
        }
        _SslEngine = _SslContext.createSSLEngine();
        _SslEngine.setEnabledProtocols(new String[] { "TLSv1.2"
        });
        _SslEngine.setUseClientMode(type == ISort.Type.CONSUMER);
        _SslEngine.setNeedClientAuth(type == ISort.Type.SERVER);
        _SslSession = _SslEngine.getSession();
        _AppInBuffer = ByteBuffer.allocate(_SslSession.getApplicationBufferSize());
        if (_SslSession.getPacketBufferSize() > getRvBuffer().capacity()) {
            throw new NegativeArraySizeException("pls check io config @ recv_buffer_size");
        }
    }

    @Override
    public A getActingContext()
    {
        return _ActingContext;
    }

    public SSLEngine getSSLEngine()
    {
        return _SslEngine;
    }

    public SSLContext getSSLContext()
    {
        return _SslContext;
    }

    @Override
    public void reset()
    {
        mUpdateKeyIn = false;
        mUpdateKeyOut = false;
        try {
            _SslEngine.closeInbound();
        }
        catch (SSLException e) {
            LOGGER.info("ssl in bound exception %s", e.getMessage());
        }
        _SslEngine.closeOutbound();
        super.reset();
    }

    @Override
    public void dispose()
    {
        super.dispose();
        reset();
    }

    @Override
    public boolean isProxy()
    {
        return true;
    }

    @Override
    public void ready()
    {
        advanceState(_DecodeState, DECODE_FRAME, CAPACITY);
        advanceState(_EncodeState, ENCODE_FRAME, CAPACITY);
        _ActingContext.ready();
        try {
            _SslEngine.beginHandshake();
        }
        catch (SSLException e) {
            e.printStackTrace();
        }
    }

    public SSLEngineResult.HandshakeStatus getHandShakeStatus()
    {
        return _SslEngine.getHandshakeStatus();
    }

    public SSLEngineResult.HandshakeStatus doTask()
    {
        Runnable delegatedTask;
        while ((delegatedTask = _SslEngine.getDelegatedTask()) != null) {
            delegatedTask.run();
        }
        return _SslEngine.getHandshakeStatus();
    }

    @Override
    public boolean needUpdateKeyIn()
    {
        if (mUpdateKeyIn) {
            mUpdateKeyIn = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean needUpdateKeyOut()
    {
        if (mUpdateKeyOut) {
            mUpdateKeyOut = false;
            return true;
        }
        return false;
    }

    @Override
    public void updateIn()
    {
        updateKeyIn();
    }

    /*
     * 处理流中仅提供信号即可，
     * 真正的操作在decode 中使用 cryptIn最终执行
     *
     */
    @Override
    public void updateKeyIn()
    {
        mUpdateKeyIn = true;
    }

    /*
     * 处理流中仅提供信号即可，
     * 真正的操作在encode 中使用 cryptOut最终执行
     */
    @Override
    public void updateOut()
    {
        updateKeyOut();
    }

    @Override
    public void updateKeyOut()
    {
        mUpdateKeyOut = true;
    }

    public ByteBuffer doWrap(IPacket output)
    {
        try {
            ByteBuffer appOutBuffer = output.getBuffer();
            ByteBuffer netOutBuffer = ByteBuffer.allocate(_SslSession.getPacketBufferSize());
            SSLEngineResult result = _SslEngine.wrap(appOutBuffer, netOutBuffer);
            int produced = result.bytesProduced();
            switch (result.getStatus())
            {
                case OK, BUFFER_UNDERFLOW ->
                    {
                        doTask();
                        return netOutBuffer;
                    }
                case CLOSED, BUFFER_OVERFLOW -> throw new ZException("ssl wrap error:%s", result.getStatus());
            }
            return appOutBuffer;
        }
        catch (SSLException e) {
            throw new ZException(e, "ssl wrap error");
        }
    }

    public ByteBuffer doUnwrap(ByteBuffer netInBuffer)
    {
        try {
            netInBuffer.mark();
            _AppInBuffer.mark();
            SSLEngineResult result = _SslEngine.unwrap(netInBuffer, _AppInBuffer);
            int consumed = result.bytesConsumed();
            int produced = result.bytesProduced();
            switch (result.getStatus())
            {
                case OK -> doTask();
                case BUFFER_UNDERFLOW ->
                    {
                        if (netInBuffer.hasRemaining()) {
                            throw new ZException(new IllegalStateException(),
                                                 "state error,unwrap under flow & input has remain");
                        }
                        if (netInBuffer == getRvBuffer()) {
                            netInBuffer.position(netInBuffer.limit());
                            netInBuffer.limit(netInBuffer.capacity());
                        }
                        else {
                            netInBuffer.reset();
                            getRvBuffer().put(netInBuffer);
                        }
                        _AppInBuffer.reset();
                        return _AppInBuffer;
                    }
                case CLOSED -> throw new ZException("ssl unwrap closed:%s", result.getStatus());
                case BUFFER_OVERFLOW -> throw new ZException("ssl unwrap overflow");
            }
            return _AppInBuffer;
        }
        catch (SSLException e) {
            throw new ZException(e, "ssl unwrap error");
        }
    }

    @Override
    public void finish()
    {
        super.finish();
        _AppInBuffer.clear();
    }

    @Override
    public void cryptIn()
    {
        advanceInState(DECODE_PAYLOAD);
    }

    @Override
    public void cryptOut()
    {
        advanceOutState(ENCODE_PAYLOAD);
    }

    @Override
    public boolean isInCrypt()
    {
        return _DecodeState.get() == DECODE_PAYLOAD;
    }

    @Override
    public boolean isOutCrypt()
    {
        return _EncodeState.get() == ENCODE_PAYLOAD;
    }
}
