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
import com.isahl.chess.bishop.protocol.mqtt.model.MqttProtocol;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.mqtt.model.QttFrame;
import com.isahl.chess.bishop.protocol.mqtt.v5.ctrl.X11F_QttAuth;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IoFactory;

import java.nio.ByteBuffer;

import static java.lang.String.format;

/**
 * @author william.d.zk
 * @date 2020/4/11
 */
public class QttFactory
        implements IoFactory<QttFrame, QttContext>
{
    private static final QttFactory _Instance = new QttFactory();

    public static <T extends MqttProtocol & IControl> T Create(QttFrame frame, QttContext context)
    {
        return _Instance.create(frame, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IControl> T create(int serial, ByteBuffer input)
    {
        IControl control = build(serial);
        if(control != null) {
            control.decode(input);
        }
        return (T) control;
    }

    @Override
    public <T extends IControl> T create(QttFrame frame, QttContext context)
    {
        return build(frame, context);
    }

    @SuppressWarnings("unchecked")
    private <T extends IControl, E extends MqttProtocol & IControl> T build(QttFrame frame, QttContext context)
    {
        E control = build(frame._sub());
        if(control != null) {
            control.put(frame.ctrl());
            control.putContext(context);
            control.decode(frame.payload(), context);
        }
        return (T) control;
    }

    private <T extends MqttProtocol & IControl> T build(int serial)
    {
        if(serial < 0x111 || serial > 0x11F) {
            return null;
        }
        return (T) switch(serial) {
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
            default -> throw new IllegalArgumentException(format("mqtt type error: %#x", serial));
        };
    }

}
