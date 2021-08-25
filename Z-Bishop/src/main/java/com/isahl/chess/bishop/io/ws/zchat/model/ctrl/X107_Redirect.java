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

package com.isahl.chess.bishop.io.ws.zchat.model.ctrl;

import com.isahl.chess.bishop.io.ws.model.WsControl;
import com.isahl.chess.bishop.io.ws.model.WsFrame;
import com.isahl.chess.king.base.util.IoUtil;

/**
 * @author william.d.zk
 */
public class X107_Redirect
        extends WsControl
{
    public final static int COMMAND = 0x107;

    public X107_Redirect(byte[] payload)
    {
        super(WsFrame.frame_op_code_ctrl_redirect, COMMAND, payload);
    }

    public String getHost()
    {
        return IoUtil.readIpAdr(payload());
    }

    public int getPort()
    {
        return IoUtil.readUnsignedShort(payload(), 4);
    }

    @Override
    public X107_Redirect duplicate()
    {
        return new X107_Redirect(payload());
    }
}