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

import com.isahl.chess.bishop.protocol.ProtocolContext;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IProxyContext;
import com.isahl.chess.queen.io.core.features.model.session.ssl.ISslOption;

import javax.net.ssl.*;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * @author william.d.zk
 */
public class SSLZContext<A extends IPContext>
        extends ProtocolContext<IPacket>
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

    public void close()
    {
        try {
            _SslEngine.closeInbound();
            _SslEngine.closeOutbound();
        }
        catch(SSLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isProxy()
    {
        return true;
    }

    @Override
    public void ready()
    {
        advanceOutState(ENCODE_PAYLOAD);
        advanceInState(DECODE_FRAME);
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

    public ByteBuf doWrap(ByteBuf output)
    {
        try {
            ByteBuf netOutBuffer = ByteBuf.allocate(_SslSession.getPacketBufferSize());
            SSLEngineResult result = _SslEngine.wrap(output.toReadBuffer(), netOutBuffer.toWriteBuffer());
            int produced = result.bytesProduced();
            switch(result.getStatus()) {
                case OK, BUFFER_UNDERFLOW -> {
                    doTask();
                    return netOutBuffer.seek(produced);
                }
                case CLOSED, BUFFER_OVERFLOW -> throw new ZException("ssl wrap error:%s", result.getStatus());
            }
            return null;
        }
        catch(SSLException e) {
            throw new ZException(e, "ssl wrap error");
        }
    }

    public ByteBuf doUnwrap(ByteBuf netInBuffer)
    {
        try {
            ByteBuf appInBuffer = ByteBuf.allocate(_AppInBufferSize);
            ByteBuffer inputBuffer = netInBuffer.toReadBuffer();
            SSLEngineResult result = _SslEngine.unwrap(inputBuffer, appInBuffer.toWriteBuffer());
            int consumed = result.bytesConsumed();
            int produced = result.bytesProduced();
            switch(result.getStatus()) {
                case OK -> doTask();
                case BUFFER_UNDERFLOW -> {
                    if(inputBuffer.hasRemaining()) {
                        throw new ZException(new IllegalStateException(),
                                             "state error,unwrap under flow & input has remain");
                    }
                    return null;
                }
                case CLOSED -> throw new ZException("ssl unwrap closed:%s", result.getStatus());
                case BUFFER_OVERFLOW -> throw new ZException("ssl unwrap overflow");
            }
            netInBuffer.skip(consumed);
            return produced > 0 ? appInBuffer.seek(produced) : null;
        }
        catch(SSLException e) {
            throw new ZException(e, "ssl unwrap error");
        }
    }

}
