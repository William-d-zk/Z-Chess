/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.bishop.io.mqtt.filter;

import com.tgx.chess.bishop.io.mqtt.MqttProtocol;
import com.tgx.chess.bishop.io.mqtt.QttCommand;
import com.tgx.chess.bishop.io.mqtt.QttControl;
import com.tgx.chess.bishop.io.mqtt.QttFrame;
import com.tgx.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.tgx.chess.bishop.io.mqtt.control.X112_QttConnack;
import com.tgx.chess.bishop.io.mqtt.control.X113_QttPublish;
import com.tgx.chess.bishop.io.mqtt.control.X114_QttPuback;
import com.tgx.chess.bishop.io.mqtt.control.X115_QttPubrec;
import com.tgx.chess.bishop.io.mqtt.control.X116_QttPubrel;
import com.tgx.chess.bishop.io.mqtt.control.X117_QttPubcomp;
import com.tgx.chess.bishop.io.mqtt.control.X118_QttSubscribe;
import com.tgx.chess.bishop.io.mqtt.control.X119_QttSuback;
import com.tgx.chess.bishop.io.mqtt.control.X11A_QttUnsubscribe;
import com.tgx.chess.bishop.io.mqtt.control.X11B_QttUnsuback;
import com.tgx.chess.bishop.io.mqtt.control.X11C_QttPingreq;
import com.tgx.chess.bishop.io.mqtt.control.X11D_QttPingresp;
import com.tgx.chess.bishop.io.mqtt.control.X11E_QttDisconnect;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.queen.io.core.inf.ICommandFactory;
import com.tgx.chess.queen.io.core.inf.IControl;

/**
 * @author william.d.zk
 * @date 2020/4/11
 */
public class QttCommandFactory
        implements
        ICommandFactory<IControl<ZContext>,
                        QttFrame>
{

    @Override
    public IControl<ZContext> create(QttFrame frame)
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
    public IControl<ZContext> create(int serial)
    {
        MqttProtocol.QTT_TYPE qttType = MqttProtocol.QTT_TYPE.valueOf(serial);
        switch (qttType)
        {
            case CONNECT:
                return new X111_QttConnect();
            case CONNACK:
                return new X112_QttConnack();
            case PINGREQ:
                return new X11C_QttPingreq();
            case PINGRESP:
                return new X11D_QttPingresp();
            case DISCONNECT:
                return new X11E_QttDisconnect();
            case PUBLISH:
                return new X113_QttPublish();
            case PUBACK:
                return new X114_QttPuback();
            case PUBREC:
                return new X115_QttPubrec();
            case PUBREL:
                return new X116_QttPubrel();
            case PUBCOMP:
                return new X117_QttPubcomp();
            case SUBSCRIBE:
                return new X118_QttSubscribe();
            case SUBACK:
                return new X119_QttSuback();
            case UNSUBSCRIBE:
                return new X11A_QttUnsubscribe();
            case UNSUBACK:
                return new X11B_QttUnsuback();
            default:
                throw new IllegalArgumentException("MQTT type error");
        }
    }

    public static QttControl createQttControl(QttFrame frame)
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

    public static QttCommand createQttCommand(QttFrame frame)
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
