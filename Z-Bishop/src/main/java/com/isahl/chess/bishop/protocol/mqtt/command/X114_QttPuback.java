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

package com.isahl.chess.bishop.protocol.mqtt.command;

import com.isahl.chess.bishop.protocol.mqtt.model.QttType;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;

import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE;

/**
 * @author william.d.zk
 * @date 2019-05-30
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x114)
public class X114_QttPuback
        extends QttCommand
{

    public X114_QttPuback()
    {
        generateCtrl(false, false, ALMOST_ONCE, QttType.PUBACK);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        setMsgId(input.getUnsignedShort());
        return input.readableBytes();
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output.putShort((short) getMsgId());
        if(mPayload != null) {
            output.put(mPayload);
        }
        return output;
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_09_CONFIRM_MESSAGE;
    }

    @Override
    public String toString()
    {
        return String.format("x114 puback:{msg-id:%d}", getMsgId());
    }
}
