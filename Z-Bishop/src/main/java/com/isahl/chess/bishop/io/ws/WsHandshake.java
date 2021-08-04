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
package com.isahl.chess.bishop.io.ws;

import java.nio.charset.StandardCharsets;

/**
 * @author William.d.zk
 */
public abstract class WsHandshake
        extends WsControl
{
    public WsHandshake(int command, String msg, int code)
    {
        super(WsFrame.frame_op_code_ctrl_handshake, command, msg.getBytes(StandardCharsets.UTF_8));
        _Code = code;
    }

    @Override
    public String toString()
    {
        return String.format("%s", new String(payload(), StandardCharsets.UTF_8));
    }

    private final int _Code;

    public boolean isClientOk()
    {
        return _Code == IWsContext.HS_State_CLIENT_OK;
    }

    public boolean isServerAccept()
    {
        return _Code == IWsContext.HS_State_ACCEPT_OK;
    }
}
