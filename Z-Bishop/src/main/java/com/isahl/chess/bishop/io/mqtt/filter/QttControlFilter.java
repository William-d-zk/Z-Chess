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

package com.isahl.chess.bishop.io.mqtt.filter;

import com.isahl.chess.bishop.io.mqtt.QttContext;
import com.isahl.chess.bishop.io.mqtt.QttControl;
import com.isahl.chess.bishop.io.mqtt.QttFrame;
import com.isahl.chess.queen.io.core.async.AioFilterChain;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IProtocol;

/**
 * @author william.d.zk
 * 
 * @date 2019-05-13
 */
public class QttControlFilter
        extends
        AioFilterChain<QttContext,
                       QttControl,
                       QttFrame>
{

    public QttControlFilter()
    {
        super("mqtt_control");
    }

    @Override
    public ResultType seek(QttContext context, QttControl output)
    {
        return context.isOutConvert() ? ResultType.NEXT_STEP
                                      : ResultType.IGNORE;
    }

    @Override
    public ResultType peek(QttContext context, QttFrame input)
    {
        return context.isInConvert() && input.isCtrl() ? ResultType.HANDLED
                                                       : ResultType.IGNORE;
    }

    @Override
    public QttFrame encode(QttContext context, QttControl output)
    {
        QttFrame frame = new QttFrame();
        frame.setCtrl(output.getCtrl());
        frame.setPayload(output.encode());
        return frame;
    }

    @Override
    public QttControl decode(QttContext context, QttFrame input)
    {
        QttControl qttControl = QttCommandFactory.createQttControl(input);
        if (qttControl == null) throw new IllegalArgumentException("MQTT type error");
        else return qttControl;
    }

    @Override
    public <C extends IPContext,
            O extends IProtocol> ResultType pipeSeek(C context, O output)
    {
        return checkType(output, IProtocol.CONTROL_SERIAL) ? seek((QttContext) context, (QttControl) output)
                                                           : ResultType.IGNORE;
    }

    @Override
    public <C extends IPContext,
            I extends IProtocol> ResultType pipePeek(C context, I input)
    {
        return checkType(input, IProtocol.FRAME_SERIAL) ? peek((QttContext) context, (QttFrame) input)
                                                        : ResultType.IGNORE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends IPContext,
            O extends IProtocol,
            I extends IProtocol> I pipeEncode(C context, O output)
    {
        return (I) encode((QttContext) context, (QttControl) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends IPContext,
            O extends IProtocol,
            I extends IProtocol> O pipeDecode(C context, I input)
    {
        return (O) decode((QttContext) context, (QttFrame) input);
    }

}