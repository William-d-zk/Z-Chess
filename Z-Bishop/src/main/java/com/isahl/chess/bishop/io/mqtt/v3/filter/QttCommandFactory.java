/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.bishop.io.mqtt.v3.filter;

import com.isahl.chess.bishop.io.mqtt.MqttProtocol;
import com.isahl.chess.bishop.io.mqtt.QttCommand;
import com.isahl.chess.bishop.io.mqtt.QttControl;
import com.isahl.chess.bishop.io.mqtt.v3.QttFrameV3;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X111_QttConnect;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X112_QttConnack;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X113_QttPublish;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X114_QttPuback;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X115_QttPubrec;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X116_QttPubrel;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X117_QttPubcomp;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X118_QttSubscribe;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X119_QttSuback;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X11A_QttUnsubscribe;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X11B_QttUnsuback;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X11C_QttPingreq;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X11D_QttPingresp;
import com.isahl.chess.bishop.io.mqtt.v3.protocol.X11E_QttDisconnect;
import com.isahl.chess.queen.io.core.inf.ICommandFactory;
import com.isahl.chess.queen.io.core.inf.IControl;

/**
 * @author william.d.zk
 * 
 * @date 2020/4/11
 */
public class QttCommandFactory
        implements
        ICommandFactory<IControl,
                QttFrameV3>
{

    @Override
    public IControl create(QttFrameV3 frame)
    {
        QttCommand qttCommand = createQttCommand(frame);
        if (qttCommand == null) {
            QttControl qttControl = createQttControl(frame);
            if (qttControl != null) {
                return qttControl;
            }
            else throw new IllegalArgumentException("MQTT type error");
        }
        else return qttCommand;
    }

    @Override
    public IControl create(int serial)
    {
        MqttProtocol.QTT_TYPE qttType = MqttProtocol.QTT_TYPE.valueOf(serial);
        return switch (qttType)
        {
            case CONNECT -> new X111_QttConnect();
            case CONNACK -> new X112_QttConnack();
            case PINGREQ -> new X11C_QttPingreq();
            case PINGRESP -> new X11D_QttPingresp();
            case DISCONNECT -> new X11E_QttDisconnect();
            case PUBLISH -> new X113_QttPublish();
            case PUBACK -> new X114_QttPuback();
            case PUBREC -> new X115_QttPubrec();
            case PUBREL -> new X116_QttPubrel();
            case PUBCOMP -> new X117_QttPubcomp();
            case SUBSCRIBE -> new X118_QttSubscribe();
            case SUBACK -> new X119_QttSuback();
            case UNSUBSCRIBE -> new X11A_QttUnsubscribe();
            case UNSUBACK -> new X11B_QttUnsuback();
        };
    }

    public static QttControl createQttControl(QttFrameV3 frame)
    {
        {
            QttControl qttControl;
            switch (frame.getType())
            {
                case CONNECT:
                    qttControl = new X111_QttConnect();
                    break;
                case CONNACK:
                    qttControl = new X112_QttConnack();
                    break;
                case PINGREQ:
                    qttControl = new X11C_QttPingreq();
                    break;
                case PINGRESP:
                    qttControl = new X11D_QttPingresp();
                    break;
                case DISCONNECT:
                    qttControl = new X11E_QttDisconnect();
                    break;
                default:
                    return null;
            }
            byte[] payload = frame.getPayload();
            if (payload != null) {
                qttControl.decode(payload);
            }
            return qttControl;
        }
    }

    public static QttCommand createQttCommand(QttFrameV3 frame)
    {
        QttCommand qttCommand;
        switch (frame.getType())
        {
            case PUBLISH:
                qttCommand = new X113_QttPublish();
                break;
            case PUBACK:
                qttCommand = new X114_QttPuback();
                break;
            case PUBREC:
                qttCommand = new X115_QttPubrec();
                break;
            case PUBREL:
                qttCommand = new X116_QttPubrel();
                break;
            case PUBCOMP:
                qttCommand = new X117_QttPubcomp();
                break;
            case SUBSCRIBE:
                qttCommand = new X118_QttSubscribe();
                break;
            case SUBACK:
                qttCommand = new X119_QttSuback();
                break;
            case UNSUBSCRIBE:
                qttCommand = new X11A_QttUnsubscribe();
                break;
            case UNSUBACK:
                qttCommand = new X11B_QttUnsuback();
                break;
            default:
                return null;
        }
        qttCommand.setCtrl(frame.getCtrl());
        byte[] payload = frame.getPayload();
        if (payload != null) {
            qttCommand.decode(payload);
        }
        return qttCommand;
    }
}
