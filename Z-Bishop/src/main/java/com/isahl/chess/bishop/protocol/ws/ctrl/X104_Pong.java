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
package com.isahl.chess.bishop.protocol.ws.ctrl;

import com.isahl.chess.bishop.protocol.ws.model.WsControl;
import com.isahl.chess.bishop.protocol.ws.model.WsFrame;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;

import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE;

/**
 * @author William.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_CONTROL_SERIAL,
                  serial = 0x104)
public class X104_Pong
        extends WsControl
{
    public X104_Pong()
    {
        this(null);
    }

    public X104_Pong(byte[] payload)
    {
        super(WsFrame.frame_op_code_ctrl_pong);
        mPayload = payload;
    }

    @Override
    public X104_Pong copy()
    {
        return new X104_Pong(mPayload);
    }

    @Override
    public Level getLevel()
    {
        return ALMOST_ONCE;
    }
}
