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

package com.isahl.chess.bishop.protocol.zchat.model.ctrl;

import com.isahl.chess.bishop.io.ssl.SSLZContext;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.net.socket.AioPacket;

/**
 * Extended X07_SslHandShake that sends TLS handshake data when session is set.
 * This is used internally by SslHandShakeFilter to trigger SSL handshake response.
 */
public class X07_SslHandShakeSend extends X07_SslHandShake
{
    private static final Logger _Logger = Logger.getLogger(X07_SslHandShakeSend.class.getSimpleName());
    
    private transient IPacket mHandShakePacket;
    private transient SSLZContext mSslContext;

    public X07_SslHandShakeSend()
    {
        super();
    }

    public void setHandShakePacket(IPacket packet)
    {
        mHandShakePacket = packet;
    }

    public void setSslContext(SSLZContext context)
    {
        mSslContext = context;
    }

    @Override
    public X07_SslHandShakeSend with(ISession session)
    {
        super.with(session);
        // Send handshake data directly using session.write()
        if(mHandShakePacket != null && mHandShakePacket.getBuffer().isReadable()) {
            _Logger.debug("X07_SslHandShakeSend: sending handshake data via session.write(), bytes=%d", mHandShakePacket.getBuffer().readableBytes());
            try {
                session.write(mHandShakePacket, new java.nio.channels.CompletionHandler<Integer, ISession>() {
                    @Override
                    public void completed(Integer result, ISession attachment) {
                        _Logger.debug("X07_SslHandShakeSend: handshake data sent, bytes=%d", result);
                    }
                    @Override
                    public void failed(Throwable exc, ISession attachment) {
                        _Logger.error("X07_SslHandShakeSend: send failed: %s", exc.getMessage());
                    }
                });
            } catch(Exception e) {
                _Logger.error("X07_SslHandShakeSend: session.write() failed: %s", e.getMessage());
            }
            // Clear carrier to avoid sending twice
            if(mSslContext != null) {
                mSslContext.setCarrier(null);
            }
        }
        return this;
    }
}
