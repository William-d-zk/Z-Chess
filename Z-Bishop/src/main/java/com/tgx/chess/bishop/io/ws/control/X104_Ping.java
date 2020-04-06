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

import static com.tgx.chess.queen.io.core.inf.IQoS.Level.AT_LEAST_ONCE;

import com.tgx.chess.bishop.io.ws.WsControl;
import com.tgx.chess.bishop.io.ws.WsFrame;

/**
 * @author William.d.zk
 */
public class X104_Ping
        extends
        WsControl
{

    public final static int COMMAND = 0x104;

    public X104_Ping()
    {
        this(null);
    }

    public X104_Ping(byte[] payload)
    {
        super(WsFrame.frame_op_code_ctrl_ping, COMMAND, payload);
    }

    @Override
    public X104_Ping duplicate()
    {
        return new X104_Ping(getPayload());
    }

    @Override
    public Level getLevel()
    {
        return AT_LEAST_ONCE;
    }
}
