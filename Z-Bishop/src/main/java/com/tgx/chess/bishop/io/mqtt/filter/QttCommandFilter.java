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

import com.tgx.chess.bishop.io.mqtt.bean.QttContext;
import com.tgx.chess.bishop.io.mqtt.bean.QttFrame;
import com.tgx.chess.bishop.io.zprotocol.BaseCommand;
import com.tgx.chess.queen.io.core.async.AioFilterChain;
import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author william.d.zk
 * @date 2019-05-08
 */
public class QttCommandFilter<C extends QttContext>
        extends
        AioFilterChain<C>
{
    public QttCommandFilter()
    {
        super("mqtt-command-filter");
    }

    @Override
    public ResultType preEncode(C context, IProtocol output)
    {
        return null;
    }

    @Override
    public ResultType preDecode(C context, IProtocol input)
    {
        if (context == null || input == null) return ResultType.ERROR;
        return input instanceof QttFrame && ((QttFrame) input).isNoCtrl() ? ResultType.HANDLED
                                                                          : ResultType.IGNORE;
    }

    @Override
    public IProtocol encode(C context, IProtocol output)
    {
        QttFrame frame = new QttFrame();
        @SuppressWarnings("unchecked")
        BaseCommand<C> command = (BaseCommand<C>) output;
        frame.setPayload(command.encode(context));
        return frame;
    }

    @Override
    public IProtocol decode(C context, IProtocol input)
    {
        return null;
    }
}
