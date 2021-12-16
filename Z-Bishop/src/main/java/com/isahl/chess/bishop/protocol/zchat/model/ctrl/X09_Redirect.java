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

import java.nio.charset.StandardCharsets;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_CONTROL_SERIAL,
                  serial = 0x09)
public class X09_Redirect
        extends ZControl
{

    private int mPort;

    public X09_Redirect()
    {
        super();
    }

    public String getHost()
    {
        return new String(mPayload, StandardCharsets.UTF_8);
    }

    public int getPort()
    {
        return mPort;
    }

    public void setHost(String host)
    {
        mPayload = host.getBytes(StandardCharsets.UTF_8);
    }

    public void setPort(int port)
    {
        mPort = port;
    }

    @Override
    public X09_Redirect copy()
    {
        X09_Redirect x107 = new X09_Redirect();
        x107.setHost(getHost());
        x107.setPort(getPort());
        return x107;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mPort = input.getUnsignedShort();
        return remain - 2;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putShort((short) mPort);
    }

    @Override
    public Level getLevel()
    {
        return Level.ALMOST_ONCE;
    }

    @Override
    public int length()
    {
        return super.length() + 2;
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }
}