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

package com.tgx.chess.bishop.io.mqtt.filter;

import com.tgx.chess.bishop.io.mqtt.bean.QttControl;
import com.tgx.chess.bishop.io.mqtt.bean.QttFrame;
import com.tgx.chess.bishop.io.mqtt.control.*;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.queen.io.core.async.AioFilterChain;
import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author william.d.zk
 * @date 2019-05-13
 */
public class QttControlFilter
        extends
        AioFilterChain<ZContext,
                       QttControl,
                       QttFrame>
{

    public QttControlFilter()
    {
        super("mqtt-control-filter");
    }

    @Override
    public ResultType preEncode(ZContext context, IProtocol output)
    {
        return preControlEncode(context, output);
    }

    @Override
    public ResultType preDecode(ZContext context, QttFrame input)
    {
        return preControlDecode(context, input);
    }

    @Override
    public QttFrame encode(ZContext context, QttControl output)
    {
        QttFrame frame = new QttFrame();
        frame.setCtrl(output.getCtrl());
        frame.setPayload(output.encode());
        return frame;
    }

    @Override
    public QttControl decode(ZContext context, QttFrame input)
    {
        QttControl qttControl;
        switch (input.getType())
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
                throw new IllegalArgumentException("MQTT type error");
        }
        qttControl.decode(input.getPayload());
        return qttControl;
    }
}
