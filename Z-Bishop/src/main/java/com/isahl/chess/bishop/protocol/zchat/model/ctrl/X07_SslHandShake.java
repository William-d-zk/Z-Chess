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

import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;

import javax.net.ssl.SSLEngineResult;
import java.nio.charset.StandardCharsets;

@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_CONTROL_SERIAL,
                  serial = 0x07)
public class X07_SslHandShake
        extends ZControl
{

    public X07_SslHandShake()
    {
        super();
    }

    private byte mHandshakeStatus;

    public SSLEngineResult.HandshakeStatus getHandshakeStatus()
    {
        return switch(mHandshakeStatus) {
            case 1 -> SSLEngineResult.HandshakeStatus.FINISHED;
            case 2 -> SSLEngineResult.HandshakeStatus.NEED_TASK;
            case 3 -> SSLEngineResult.HandshakeStatus.NEED_WRAP;
            case 4 -> SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
            case 5 -> SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN;
            default -> SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
        };
    }

    public void setHandshakeStatus(SSLEngineResult.HandshakeStatus handshakeStatus)
    {
        switch(handshakeStatus) {
            case FINISHED -> mHandshakeStatus = 1;
            case NEED_TASK -> mHandshakeStatus = 2;
            case NEED_WRAP -> mHandshakeStatus = 3;
            case NEED_UNWRAP -> mHandshakeStatus = 4;
            case NEED_UNWRAP_AGAIN -> mHandshakeStatus = 5;
            default -> mHandshakeStatus = 0;
        }
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mHandshakeStatus = input.get();
        return remain - 1;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .put(mHandshakeStatus);
    }

    @Override
    public int length()
    {
        return super.length() + 1;
    }

    @Override
    public String toString()
    {
        return String.format("X105_SslHandShake{HandshakeStatus=%s;[HELLO]:%s}",
                             getHandshakeStatus(),
                             mPayload != null ? new String(mPayload, StandardCharsets.UTF_8) : "NULL");
    }

    public Level level()
    {
        return Level.ALMOST_ONCE;
    }

}
