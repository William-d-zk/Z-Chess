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

package com.isahl.chess.bishop.protocol.mqtt.filter;

import com.isahl.chess.bishop.protocol.mqtt.command.QttCommand;
import com.isahl.chess.bishop.protocol.mqtt.factory.QttFactory;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.mqtt.model.QttFrame;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.content.IFrame;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IProxyContext;
import com.isahl.chess.queen.io.core.net.socket.AioFilterChain;

/**
 * @author william.d.zk
 * @date 2019-05-08
 */
public class QttCommandFilter
        extends AioFilterChain<QttContext, QttCommand, QttFrame>
{
    public QttCommandFilter()
    {
        super("mqtt_command");
    }

    @Override
    public QttFrame encode(QttContext context, QttCommand output)
    {
        output.wrap(context);
        QttFrame frame = new QttFrame();
        frame.header(output.header());
        frame.withSub(output);
        return frame;
    }

    @Override
    public QttCommand decode(QttContext context, QttFrame input)
    {
        return (QttCommand) QttFactory._Instance.create(input, context);
    }

    @Override
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.PROTOCOL_BISHOP_COMMAND_SERIAL)) {
            if(context instanceof QttContext) {
                if(context.isOutConvert()) {
                    return new Pair<>(ResultType.NEXT_STEP, context);
                }
                else {
                    throw new ZException("connect state isn't ok, drop command %#x", output.serial());
                }
            }
            IPContext acting = context;
            while(acting.isProxy()) {
                acting = ((IProxyContext<?>) acting).getActingContext();
                if(acting instanceof QttContext && acting.isInConvert()) {
                    return new Pair<>(ResultType.NEXT_STEP, acting);
                }
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.PROTOCOL_BISHOP_FRAME_SERIAL) && input instanceof IFrame f && !f.isCtrl()) {
            if(context.isInFrame() && context instanceof QttContext) {return new Pair<>(ResultType.HANDLED, context);}
            IPContext acting = context;
            while(acting.isProxy()) {
                acting = ((IProxyContext<?>) context).getActingContext();
                if(context.isInFrame() && acting instanceof QttContext) {return new Pair<>(ResultType.HANDLED, acting);}
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> I pipeEncode(IPContext context, O output)
    {
        return (I) encode((QttContext) context, (QttCommand) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((QttContext) context, (QttFrame) input);
    }

}