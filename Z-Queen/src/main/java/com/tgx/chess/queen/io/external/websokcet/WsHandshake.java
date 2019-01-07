/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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
package com.tgx.chess.queen.io.external.websokcet;

import java.util.Objects;

/**
 * @author William.d.zk
 */
public abstract class WsHandshake
        extends
        WsControl
{

    private String rMsg;

    public WsHandshake(int command, String msg)
    {
        super(msg, command);
    }

    public String getMessage()
    {
        byte[] payload = getPayload();
        return Objects.nonNull(payload) ? new String(payload) : rMsg;
    }

    public byte getControl()
    {
        return WsFrame.frame_op_code_ctrl_handshake;
    }

    public byte[] getPayload()
    {
        byte[] payload = super.getPayload();
        return Objects.isNull(payload) ? rMsg == null ? null : rMsg.getBytes() : payload;
    }

    @Override
    public String toString()
    {
        return String.format("web socket handshake %s", getMessage());
    }

    @Override
    public int dataLength()
    {
        return Objects.nonNull(getPayload()) ? super.dataLength() : Objects.nonNull(rMsg) ? rMsg.getBytes().length : 0;
    }

    public void append(String x)
    {
        rMsg = Objects.isNull(rMsg) ? x : rMsg + x;
    }

    public WsHandshake ahead(String x)
    {
        rMsg = rMsg == null ? x : x + rMsg;
        return this;
    }

    @Override
    public byte[] encode()
    {
        return getPayload();
    }

    @Override
    public void dispose()
    {
        rMsg = null;
    }
}
