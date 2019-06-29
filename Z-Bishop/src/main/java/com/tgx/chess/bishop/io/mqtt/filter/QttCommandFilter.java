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

import com.tgx.chess.bishop.io.mqtt.bean.BaseQtt;
import com.tgx.chess.bishop.io.mqtt.bean.QttFrame;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.BaseCommand;
import com.tgx.chess.queen.io.core.async.AioFilterChain;
import com.tgx.chess.queen.io.core.inf.ICommandFactory;
import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author william.d.zk
 * @date 2019-05-08
 */
public class QttCommandFilter
        extends
        AioFilterChain<ZContext,
                       BaseCommand,
                       QttFrame>
{
    public QttCommandFilter(ICommandFactory<ZContext,
                                            BaseCommand> factory)
    {
        super("mqtt-command-filter");
        _CommandFactory = factory;
    }

    private final ICommandFactory<ZContext,
                                  BaseCommand> _CommandFactory;

    @Override
    public QttFrame encode(ZContext context, BaseCommand output)
    {
        QttFrame frame = new QttFrame();
        frame.setCtrl(output.getCtrl());
        frame.setPayload(output.encode(context));
        return frame;
    }

    @Override
    public BaseCommand decode(ZContext context, QttFrame input)
    {
        switch (BaseQtt.QTT_TYPE.valueOf(input.getCtrl()))
        {
            case PUBLISH:
            case SUBSCRIBE:

        }
        BaseCommand cmd = _CommandFactory.create(0);

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
