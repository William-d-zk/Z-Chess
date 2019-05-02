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
package com.tgx.chess.bishop.io.ws.control;

import com.tgx.chess.bishop.io.ws.bean.WsContext;
import com.tgx.chess.bishop.io.ws.bean.WsControl;
import com.tgx.chess.bishop.io.ws.bean.WsFrame;

/**
 * @author William.d.zk
 */
public class X104_Ping<C extends WsContext>
        extends
        WsControl<C>
{

    public final static int COMMAND = 0x104;

    public X104_Ping()
    {
        super(COMMAND);
        mCtrlCode = WsFrame.frame_op_code_ctrl_ping;
    }

    public X104_Ping(byte[] payload)
    {
        super(COMMAND, payload);
        mCtrlCode = WsFrame.frame_op_code_ctrl_ping;
    }

    @Override
    public X104_Ping<C> duplicate()
    {
        return new X104_Ping<C>(getPayload());
    }
}
