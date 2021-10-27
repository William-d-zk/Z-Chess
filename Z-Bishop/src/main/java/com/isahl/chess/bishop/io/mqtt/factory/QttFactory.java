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

package com.isahl.chess.bishop.io.mqtt.factory;

import com.isahl.chess.bishop.io.mqtt.command.*;
import com.isahl.chess.bishop.io.mqtt.ctrl.*;
import com.isahl.chess.bishop.io.mqtt.model.QttContext;
import com.isahl.chess.bishop.io.mqtt.model.QttFrame;
import com.isahl.chess.bishop.io.mqtt.v5.ctrl.X11F_QttAuth;
import com.isahl.chess.queen.io.core.features.model.content.ICommand;
import com.isahl.chess.queen.io.core.features.model.content.IControl;

/**
 * @author william.d.zk
 * @date 2020/4/11
 */
public class QttFactory
        implements ICommand.Factory<IControl, QttFrame, QttContext>
{

    @Override
    public IControl create(QttFrame frame, QttContext context)
    {
        return CREATE(frame, context);
    }

    @Override
    public IControl create(int serial, byte[] data, QttContext context)
    {
        if(serial < X111_QttConnect.COMMAND || serial > X11F_QttAuth.COMMAND) {
            return null;
        }
        QttCommand qttCommand = switch(serial) {
            case X113_QttPublish.COMMAND -> new X113_QttPublish();
            case X114_QttPuback.COMMAND -> new X114_QttPuback();
            case X115_QttPubrec.COMMAND -> new X115_QttPubrec();
            case X116_QttPubrel.COMMAND -> new X116_QttPubrel();
            case X117_QttPubcomp.COMMAND -> new X117_QttPubcomp();
            case X118_QttSubscribe.COMMAND -> new X118_QttSubscribe();
            case X119_QttSuback.COMMAND -> new X119_QttSuback();
            case X11A_QttUnsubscribe.COMMAND -> new X11A_QttUnsubscribe();
            case X11B_QttUnsuback.COMMAND -> new X11B_QttUnsuback();
            default -> null;
        };
        if(qttCommand != null) {
            qttCommand.putContext(context);
            qttCommand.decode(data, context);
            return qttCommand;
        }
        QttControl qttControl = switch(serial) {
            case X111_QttConnect.COMMAND -> new X111_QttConnect();
            case X112_QttConnack.COMMAND -> new X112_QttConnack();
            case X11C_QttPingreq.COMMAND -> new X11C_QttPingreq();
            case X11D_QttPingresp.COMMAND -> new X11D_QttPingresp();
            case X11E_QttDisconnect.COMMAND -> new X11E_QttDisconnect();
            case X11F_QttAuth.COMMAND -> new X11F_QttAuth();
            default -> null;
        };
        qttControl.putContext(context);
        qttControl.decode(data);
        return qttControl;
    }

    public static IControl CREATE(QttFrame frame, QttContext context)
    {
        QttCommand qttCommand = switch(frame.getType()) {
            case PUBLISH -> new X113_QttPublish();
            case PUBACK -> new X114_QttPuback();
            case PUBREC -> new X115_QttPubrec();
            case PUBREL -> new X116_QttPubrel();
            case PUBCOMP -> new X117_QttPubcomp();
            case SUBSCRIBE -> new X118_QttSubscribe();
            case SUBACK -> new X119_QttSuback();
            case UNSUBSCRIBE -> new X11A_QttUnsubscribe();
            case UNSUBACK -> new X11B_QttUnsuback();
            default -> null;
        };
        if(qttCommand != null) {
            qttCommand.putContext(context);
            qttCommand.putCtrl(frame.ctrl());
            qttCommand.decode(frame.payload(), context);
            return qttCommand;
        }
        QttControl qttControl = switch(frame.getType()) {
            case CONNECT -> new X111_QttConnect();
            case CONNACK -> new X112_QttConnack();
            case PINGREQ -> new X11C_QttPingreq();
            case PINGRESP -> new X11D_QttPingresp();
            case DISCONNECT -> new X11E_QttDisconnect();
            case AUTH -> new X11F_QttAuth();
            default -> null;
        };
        if(qttControl != null) {
            qttControl.putContext(context);
            qttControl.decode(frame.payload());
        }
        return qttControl;
    }
}
