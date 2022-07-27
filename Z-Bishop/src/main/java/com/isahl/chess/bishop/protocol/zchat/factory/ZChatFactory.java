
/*
 * MIT License
 *
 * Copyright (c) 2022. Z-Chess
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

package com.isahl.chess.bishop.protocol.zchat.factory;

import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.bishop.protocol.zchat.model.base.ZFrame;
import com.isahl.chess.bishop.protocol.zchat.model.command.X0D_PlainText;
import com.isahl.chess.bishop.protocol.zchat.model.command.X0E_Consensus;
import com.isahl.chess.bishop.protocol.zchat.model.command.X0F_Exchange;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.*;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.zls.*;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.queen.io.core.features.model.content.IProtocolFactory;

public class ZChatFactory
        implements IProtocolFactory<ZFrame, ZContext>
{
    public final static ZChatFactory _Instance = new ZChatFactory();

    @Override
    public ZControl create(ZFrame frame, ZContext context)
    {
        ByteBuf input = frame.subEncoded();
        ZControl instance = build(ZFrame.peekSubSerial(input)).wrap(context);
        instance.decode(input);
        return instance;
    }

    @Override
    public ZControl create(ByteBuf input)
    {
        ZControl instance = build(ZFrame.peekSubSerial(input));
        instance.decode(input);
        return instance;
    }

    protected ZControl build(int serial)
    {
        return switch(serial) {
            case 0x01 -> new X01_EncryptRequest();
            case 0x02 -> new X02_AsymmetricPub();
            case 0x03 -> new X03_Cipher();
            case 0x04 -> new X04_EncryptConfirm();
            case 0x05 -> new X05_EncryptStart();
            case 0x06 -> new X06_EncryptComp();
            case 0x07 -> new X07_SslHandShake();
            case 0x08 -> new X08_Identity();
            case 0x09 -> new X09_Redirect();
            case 0x0A -> new X0A_Shutdown();
            case 0x0B -> new X0B_Ping();
            case 0x0C -> new X0C_Pong();
            case 0x0D -> new X0D_PlainText();
            case 0x0E -> new X0E_Consensus();
            case 0x0F -> new X0F_Exchange();
            default -> throw new ZException("unsupported serial %d", serial);
        };
    }
}
