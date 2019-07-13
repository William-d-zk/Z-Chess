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

import com.tgx.chess.bishop.io.mqtt.bean.QttCommand;
import com.tgx.chess.bishop.io.mqtt.bean.QttFrame;
import com.tgx.chess.bishop.io.mqtt.control.X113_QttPublish;
import com.tgx.chess.bishop.io.mqtt.control.X114_QttPuback;
import com.tgx.chess.bishop.io.mqtt.control.X115_QttPubrec;
import com.tgx.chess.bishop.io.mqtt.control.X116_QttPubrel;
import com.tgx.chess.bishop.io.mqtt.control.X117_QttPubcomp;
import com.tgx.chess.bishop.io.mqtt.control.X118_QttSubscribe;
import com.tgx.chess.bishop.io.mqtt.control.X119_QttSuback;
import com.tgx.chess.bishop.io.mqtt.control.X11A_QttUnsubscribe;
import com.tgx.chess.bishop.io.mqtt.control.X11B_QttUnsuback;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.queen.io.core.async.AioFilterChain;
import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author william.d.zk
 * @date 2019-05-08
 */
public class QttCommandFilter
        extends
        AioFilterChain<ZContext,
                       QttCommand,
                       QttFrame>
{
    public QttCommandFilter()
    {
        super("mqtt-command-filter");
    }

    @Override
    public QttFrame encode(ZContext context, QttCommand output)
    {
        QttFrame frame = new QttFrame();
        frame.setCtrl(output.getCtrl());
        frame.setPayload(output.encode(context));
        return frame;
    }

    @Override
    public QttCommand decode(ZContext context, QttFrame input)
    {
        QttCommand cmd;
        switch (input.getType())
        {
            case PUBLISH:
                cmd = new X113_QttPublish();
                break;
            case PUBACK:
                cmd = new X114_QttPuback();
                break;
            case PUBREC:
                cmd = new X115_QttPubrec();
                break;
            case PUBREL:
                cmd = new X116_QttPubrel();
                break;
            case PUBCOMP:
                cmd = new X117_QttPubcomp();
                break;
            case SUBSCRIBE:
                cmd = new X118_QttSubscribe();
                break;
            case SUBACK:
                cmd = new X119_QttSuback();
                break;
            case UNSUBSCRIBE:
                cmd = new X11A_QttUnsubscribe();
                break;
            case UNSUBACK:
                cmd = new X11B_QttUnsuback();
                break;
            default:
                throw new IllegalArgumentException("MQTT type error");
        }
        cmd.setCtrl(input.getCtrl());
        cmd.decode(input.getPayload());
        return cmd;
    }

    @Override
    public ResultType preEncode(ZContext context, IProtocol output)
    {
        return preCommandEncode(context, output);
    }

    @Override
    public ResultType preDecode(ZContext context, QttFrame input)
    {
        return preCommandDecode(context, input);
    }

}
