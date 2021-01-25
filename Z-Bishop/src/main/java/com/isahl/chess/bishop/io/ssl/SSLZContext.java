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
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IPacket;
import com.isahl.chess.queen.io.core.inf.IProxyContext;
import com.isahl.chess.queen.io.core.inf.ISessionOption;
import com.isahl.chess.queen.io.core.inf.ISslContext;

/**
 * @author william.d.zk
 */
public class SSLZContext<A extends IPContext>
        extends
        ZContext
        implements
        ISslContext,
        IProxyContext<A>
{
    private final static Logger LOGGER = Logger.getLogger(SSLZContext.class.getSimpleName());
    private final SSLEngine     _SslEngine;
    private final SSLContext    _SslContext;
    private final SSLSession    _SslSession;
    private final ByteBuffer    _AppInBuffer;
    private final A             _ActingContext;
    private boolean             mNetBuffered;

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
        super.reset();
        try {
            _SslEngine.closeInbound();
        }
        catch (SSLException e) {
            LOGGER.info("ssl in bound exception %s", e.getMessage());
        }
        _SslEngine.closeOutbound();
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
    public void updateIn()
    {
        advanceInState(DECODE_PAYLOAD);
    }

    @Override
    public void updateOut()
    {
        advanceOutState(ENCODE_PAYLOAD);
    }

    public SSLEngineResult.HandshakeStatus doWrap(IPacket toSend)
    {
        try {
            ByteBuffer toSendBuffer = toSend.getBuffer();
            ByteBuffer netSendBuffer = ByteBuffer.allocate(_SslSession.getPacketBufferSize());
            SSLEngineResult result = _SslEngine.wrap(toSendBuffer, netSendBuffer);
            int produced = result.bytesProduced();
            switch (result.getStatus())
            {
                case OK, BUFFER_UNDERFLOW ->
                    {
                        doTask();
                        netSendBuffer.flip();
                        toSend.replaceWith(netSendBuffer);
                    }
                case CLOSED, BUFFER_OVERFLOW -> throw new ZException("ssl wrap error:%s", result.getStatus());
            }
            return _SslEngine.getHandshakeStatus();
        }
        catch (SSLException e) {
            throw new ZException(e, "ssl wrap error");
        }
    }

    public SSLEngineResult.HandshakeStatus doUnwrap(IPacket input)
    {
        try {
            ByteBuffer netInBuffer = input.getBuffer();
            netInBuffer.mark();
            if (mNetBuffered) {
                getRvBuffer().put(netInBuffer);
                getRvBuffer().flip();
                netInBuffer = getRvBuffer();
            }
            SSLEngineResult.Status resultStatus;
            do {
                int mark = netInBuffer.position();
                SSLEngineResult result = _SslEngine.unwrap(netInBuffer, _AppInBuffer);
                int consumed = result.bytesConsumed();
                int produced = result.bytesProduced();
                resultStatus = result.getStatus();
                switch (resultStatus)
                {
                    case OK ->
                        {
                            doTask();
                            if (netInBuffer.hasRemaining()) {
                                if (netInBuffer != getRvBuffer()) {
                                    getRvBuffer().put(netInBuffer);
                                    mNetBuffered = true;
                                }
                                else {
                                    netInBuffer.compact();
                                }
                            }
                            else {
                                mNetBuffered = false;
                                netInBuffer.clear();
                            }
                        }
                    case BUFFER_UNDERFLOW ->
                        {
                            if (netInBuffer.hasRemaining()) {
                                throw new ZException(new IllegalStateException(),
                                                     "state error,unwrap under flow & input has remain");
                            }
                            if (mNetBuffered) {
                                if (netInBuffer.position() > 0) {
                                    netInBuffer.compact();
                                }
                                else {
                                    netInBuffer.position(netInBuffer.limit());
                                    netInBuffer.limit(netInBuffer.capacity());
                                }
                            }
                            else {
                                /*
                                   netInBuffer ==  input.getBuffer()
                                 */
                                netInBuffer.reset();
                                getRvBuffer().put(netInBuffer);
                                mNetBuffered = true;
                            }
                            return _SslEngine.getHandshakeStatus();
                        }
                    case CLOSED -> throw new ZException("ssl unwrap closed:%s", result.getStatus());
                    case BUFFER_OVERFLOW -> throw new ZException("ssl unwrap overflow");
                }
            }
            while (resultStatus != SSLEngineResult.Status.OK);
            return _SslEngine.getHandshakeStatus();
        }
        catch (

        SSLException e) {
            throw new ZException(e, "ssl unwrap error");
        }
    }

}
