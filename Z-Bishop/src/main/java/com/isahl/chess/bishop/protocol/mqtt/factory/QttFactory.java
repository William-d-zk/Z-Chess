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

package com.isahl.chess.bishop.protocol.mqtt.factory;

import com.isahl.chess.bishop.protocol.mqtt.command.*;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.*;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.mqtt.model.QttFrame;
import com.isahl.chess.bishop.protocol.mqtt.model.QttType;
import com.isahl.chess.bishop.protocol.mqtt.v5.ctrl.X11F_QttAuth;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.io.core.features.model.content.IProtocolFactory;

/**
 * @author william.d.zk
 * @date 2020/4/11
 */
public class QttFactory
        implements IProtocolFactory<QttFrame, QttContext>
{
    private final Logger _Logger = Logger.getLogger("protocol.bishop." + getClass().getSimpleName());

    public static final QttFactory _Instance = new QttFactory();

    @Override
    public QttControl create(ByteBuf input)
    {
        QttControl instance = build(QttFrame.peekSubSerial(input));
        instance.header(input.get());
        instance.decode(input);
        return instance;
    }

    @Override
    public QttControl create(QttFrame frame, QttContext context)
    {
        QttControl control = build(QttType.serialOf(QttType.valueOf(frame.header()))).wrap(context);
        control.header(frame.header());
        control.decode(frame.subEncoded());
        return control;
    }

    private QttControl build(int serial)
    {
        return switch(serial) {
            case 0x111 -> new X111_QttConnect();
            case 0x112 -> new X112_QttConnack();
            case 0x113 -> new X113_QttPublish();
            case 0x114 -> new X114_QttPuback();
            case 0x115 -> new X115_QttPubrec();
            case 0x116 -> new X116_QttPubrel();
            case 0x117 -> new X117_QttPubcomp();
            case 0x118 -> new X118_QttSubscribe();
            case 0x119 -> new X119_QttSuback();
            case 0x11A -> new X11A_QttUnsubscribe();
            case 0x11B -> new X11B_QttUnsuback();
            case 0x11C -> new X11C_QttPingreq();
            case 0x11D -> new X11D_QttPingresp();
            case 0x11E -> new X11E_QttDisconnect();
            case 0x11F -> new X11F_QttAuth();
            default -> throw new ZException("mqtt type error:[ %#x ], not handle by QttFactory", serial);
        };
    }

}
