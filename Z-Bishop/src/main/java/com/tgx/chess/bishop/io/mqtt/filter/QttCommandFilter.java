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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.bishop.io.mqtt.filter;

import static com.tgx.chess.bishop.io.mqtt.filter.QttCommandFactory.createQttCommand;

import com.tgx.chess.bishop.io.mqtt.QttCommand;
import com.tgx.chess.bishop.io.mqtt.QttFrame;
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
        QttCommand qttCommand = createQttCommand(input);
        if (qttCommand == null) throw new IllegalArgumentException("MQTT type error");
        else return qttCommand;
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
