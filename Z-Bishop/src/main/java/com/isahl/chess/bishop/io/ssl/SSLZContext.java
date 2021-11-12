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

package com.isahl.chess.bishop.io.ssl;

import com.isahl.chess.bishop.protocol.ws.zchat.ZContext;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IProxyContext;
import com.isahl.chess.queen.io.core.features.model.session.ssl.ISslOption;

import javax.net.ssl.*;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static com.isahl.chess.king.base.cron.features.ITask.advanceState;
import static com.isahl.chess.queen.io.core.features.model.session.ISession.CAPACITY;

/**
 * @author william.d.zk
 */
public class SSLZContext<A extends IPContext>
        extends ZContext
        implements IProxyContext<A>
{
    private final SSLEngine  _SslEngine;
    private final SSLContext _SslContext;
    private final SSLSession _SslSession;
    private final A          _ActingContext;
    private final int        _AppInBufferSize;

    public SSLZContext(ISslOption option, ISort.Mode mode, ISort.Type type, A acting) throws NoSuchAlgorithmException
    {
        super(option, mode, type);
        _ActingContext = acting;
        _SslContext = SSLContext.getInstance("TLSv1.2");
        try {
            _SslContext.init(option.getKeyManagers(), option.getTrustManagers(), null);
        }
        catch(KeyManagementException e) {
            throw new ZException(e, "ssl context init failed");
        }
        _SslEngine = _SslContext.createSSLEngine();
        _SslEngine.setEnabledProtocols(new String[]{ "TLSv1.2" });
        _SslEngine.setUseClientMode(type == ISort.Type.CLIENT);
        _SslEngine.setNeedClientAuth(type == ISort.Type.SERVER && option.isSslClientAuth());
        _SslSession = _SslEngine.getSession();
        _AppInBufferSize = option.getSslAppSize();
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
        try {
            _SslEngine.closeInbound();
            _SslEngine.closeOutbound();
        }
        catch(SSLException e) {
            e.printStackTrace();
        }
        finally {
            super.reset();
        }
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
        catch(SSLException e) {
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
        while((delegatedTask = _SslEngine.getDelegatedTask()) != null) {
            delegatedTask.run();
        }
        return _SslEngine.getHandshakeStatus();
    }

    public ByteBuffer doWrap(ByteBuffer output)
    {
        try {
            ByteBuffer netOutBuffer = ByteBuffer.allocate(_SslSession.getPacketBufferSize());
            SSLEngineResult result = _SslEngine.wrap(output, netOutBuffer);
            int produced = result.bytesProduced();
            switch(result.getStatus()) {
                case OK, BUFFER_UNDERFLOW -> {
                    doTask();
                    return netOutBuffer;
                }
                case CLOSED, BUFFER_OVERFLOW -> throw new ZException("ssl wrap error:%s", result.getStatus());
            }
            return null;
        }
        catch(SSLException e) {
            throw new ZException(e, "ssl wrap error");
        }
    }

    public ByteBuffer doUnwrap(ByteBuffer netInBuffer)
    {
        try {
            netInBuffer.mark();
            ByteBuffer appInBuffer = ByteBuffer.allocate(_AppInBufferSize);
            SSLEngineResult result = _SslEngine.unwrap(netInBuffer, appInBuffer);
            int consumed = result.bytesConsumed();
            int produced = result.bytesProduced();
            switch(result.getStatus()) {
                case OK -> doTask();
                case BUFFER_UNDERFLOW -> {
                    if(netInBuffer.hasRemaining()) {
                        throw new ZException(new IllegalStateException(),
                                             "state error,unwrap under flow & input has remain");
                    }
                    if(netInBuffer == getRvBuffer()) {
                        netInBuffer.position(netInBuffer.limit());
                        netInBuffer.limit(netInBuffer.capacity());
                    }
                    else {
                        netInBuffer.reset();
                        IoUtil.write(netInBuffer, getRvBuffer());
                    }
                    return null;
                }
                case CLOSED -> throw new ZException("ssl unwrap closed:%s", result.getStatus());
                case BUFFER_OVERFLOW -> throw new ZException("ssl unwrap overflow");
            }
            return produced > 0 ? appInBuffer : null;
        }
        catch(SSLException e) {
            throw new ZException(e, "ssl unwrap error");
        }
    }

    @Override
    protected ByteBuffer allocateRcv(INetworkOption option)
    {
        return ByteBuffer.allocate(Math.max(option.getRcvByte(), ((ISslOption) option).getSslPacketSize()));
    }

    @Override
    protected ByteBuffer allocateSnf(INetworkOption option)
    {
        return ByteBuffer.allocate(Math.max(option.getSnfByte(), ((ISslOption) option).getSslPacketSize()));
    }
}
