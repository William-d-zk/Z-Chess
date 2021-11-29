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
package com.isahl.chess.bishop.protocol.zchat.filter;

import com.isahl.chess.bishop.protocol.ws.model.WsFrame;
import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.content.*;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IProxyContext;
import com.isahl.chess.queen.io.core.net.socket.AioFilterChain;

import java.util.Objects;

/**
 * @author William.d.zk
 */
public class ZCommandFilter<T extends ZContext>
        extends AioFilterChain<T, IControl, IFrame>
{

    private final IFactory<IFrame, T> _CommandFactory;

    public ZCommandFilter(IFactory<IFrame, T> factory)
    {
        super("z_command");
        _CommandFactory = Objects.requireNonNull(factory);
    }

    @Override
    public WsFrame encode(T context, IControl output)
    {
        WsFrame frame = new WsFrame();
        frame.put(output.encode(context)
                        .array());
        frame.put(WsFrame.frame_op_code_no_ctrl_bin);
        return frame;
    }

    @Override
    public IControl decode(T context, IFrame input)
    {
        return _CommandFactory.create(input, context);
    }

    @Override
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.PROTOCOL_BISHOP_COMMAND_SERIAL)) {
            if(context instanceof ZContext && context.isOutConvert()) {
                return new Pair<>(ResultType.NEXT_STEP, context);
            }
            IPContext acting = context;
            while(acting.isProxy()) {
                acting = ((IProxyContext<?>) acting).getActingContext();
                if(acting instanceof ZContext && acting.isOutConvert()) {
                    return new Pair<>(ResultType.NEXT_STEP, acting);
                }
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.PROTOCOL_BISHOP_FRAME_SERIAL) && !((IFrame) input).isCtrl()) {
            if(context instanceof ZContext && context.isInConvert()) {
                return new Pair<>(ResultType.HANDLED, context);
            }
            IPContext acting = context;
            while(acting.isProxy()) {
                acting = ((IProxyContext<?>) acting).getActingContext();
                if(acting instanceof ZContext && acting.isInConvert()) {
                    return new Pair<>(ResultType.HANDLED, acting);
                }
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> I pipeEncode(IPContext context, O output)
    {
        return (I) encode((T) context, (ICommand) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((T) context, (IFrame) input);
    }

}
