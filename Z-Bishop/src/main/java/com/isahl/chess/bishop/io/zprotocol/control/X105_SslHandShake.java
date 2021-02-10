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

package com.isahl.chess.bishop.io.zprotocol.control;

import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLEngineResult;

import com.isahl.chess.bishop.io.zprotocol.ZCommand;
import com.isahl.chess.king.base.util.IoUtil;

public class X105_SslHandShake
        extends
        ZCommand
{
    public final static int COMMAND = 0x105;

    public X105_SslHandShake(byte[] hello)
    {
        super(COMMAND, false);
        setPayload(hello);
    }

    private byte mHandshakeStatus;

    public SSLEngineResult.HandshakeStatus getHandshakeStatus()
    {
        return switch (mHandshakeStatus)
        {
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
        switch (handshakeStatus)
        {
            case FINISHED -> mHandshakeStatus = 1;
            case NEED_TASK -> mHandshakeStatus = 2;
            case NEED_WRAP -> mHandshakeStatus = 3;
            case NEED_UNWRAP -> mHandshakeStatus = 4;
            case NEED_UNWRAP_AGAIN -> mHandshakeStatus = 5;
            default -> mHandshakeStatus = 0;
        }
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mHandshakeStatus = data[pos++];
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeByte(mHandshakeStatus, data, pos);
        return pos;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength() + 1;
    }

    @Override
    public String toString()
    {
        return String.format("X105_SslHandShake{HandshakeStatus=%s;[HELLO]:%s}",
                             getHandshakeStatus(),
                             getPayload() != null ? new String(getPayload(), StandardCharsets.UTF_8)
                                                  : "NULL");
    }

    @Override
    public int superSerial()
    {
        return CONTROL_SERIAL;
    }
}
