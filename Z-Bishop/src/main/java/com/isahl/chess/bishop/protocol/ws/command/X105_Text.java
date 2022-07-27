/*
 * MIT License
 *
 * Copyright (c) 2021~2021. Z-Chess
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

package com.isahl.chess.bishop.protocol.ws.command;

import com.isahl.chess.bishop.protocol.ws.WsContext;
import com.isahl.chess.bishop.protocol.ws.model.WsControl;
import com.isahl.chess.bishop.protocol.ws.model.WsFrame;
import com.isahl.chess.board.annotation.ISerialGenerator;

import java.nio.charset.StandardCharsets;

import static com.isahl.chess.board.base.ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x105)
public class X105_Text<T extends WsContext>
        extends WsControl<T>
{

    public X105_Text()
    {
        super(WsFrame.frame_op_code_ctrl_text);
    }

    public X105_Text(String resp)
    {
        this();
        mPayload = resp.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Level level()
    {
        return Level.ALMOST_ONCE;
    }

    public String getText()
    {
        return mPayload != null ? new String(mPayload, StandardCharsets.UTF_8) : null;
    }

}
