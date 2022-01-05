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
package com.isahl.chess.bishop.protocol.ws.filter;

import com.isahl.chess.bishop.protocol.ws.WsContext;
import com.isahl.chess.bishop.protocol.ws.ctrl.X102_Close;
import com.isahl.chess.bishop.protocol.ws.ctrl.X103_Ping;
import com.isahl.chess.bishop.protocol.ws.ctrl.X104_Pong;
import com.isahl.chess.bishop.protocol.ws.features.IWsContext;
import com.isahl.chess.bishop.protocol.ws.model.WsControl;
import com.isahl.chess.bishop.protocol.ws.model.WsFrame;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IProxyContext;
import com.isahl.chess.queen.io.core.net.socket.AioFilterChain;

/**
 * @author William.d.zk
 */
public class WsControlFilter<T extends WsContext>
        extends AioFilterChain<T, WsControl<T>, WsFrame>
{
    public WsControlFilter()
    {
        super("ws_control");
    }

    @Override
    public WsFrame encode(T context, WsControl<T> output)
    {
        WsFrame frame = new WsFrame();
        frame.header(output.header());
        frame.withSub(output.encode(context)
                            .array());
        context.promotionOut();
        return frame;
    }

    @Override
    public WsControl<T> decode(T context, WsFrame input)
    {
        WsControl<T> control = switch(input.header() & 0x0F) {
            case WsFrame.frame_op_code_ctrl_close -> new X102_Close<>();
            case WsFrame.frame_op_code_ctrl_ping -> new X103_Ping<>();
            case WsFrame.frame_op_code_ctrl_pong -> new X104_Pong<>();
            default -> throw new ZException("ws control nonsupport %d", input.header() & 0x0F);
        };
        control.decode(input.subEncoded());
        context.demotionIn();
        return control;
    }

    @Override
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.PROTOCOL_BISHOP_CONTROL_SERIAL) && output instanceof WsControl c) {
            IPContext acting = context;
            do {
                //@formatter:off
                if(acting.isOutConvert() &&
                   acting instanceof IWsContext &&
                   c.isCtrl())
                {
                //@formatter:on
                    return Pair.of(ResultType.NEXT_STEP, acting);
                }
                else if(acting.isProxy()) {
                    acting = ((IProxyContext<?>) acting).getActingContext();
                }
                else {
                    acting = null;
                }
            }
            while(acting != null);
        }
        return Pair.of(ResultType.IGNORE, context);
    }

    @Override
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.PROTOCOL_BISHOP_FRAME_SERIAL) && input instanceof WsFrame f && f.isCtrl()) {
            IPContext acting = context;
            do {
                if(acting.isInConvert() && acting instanceof IWsContext) {
                    return Pair.of(ResultType.HANDLED, acting);
                }
                else if(acting.isProxy()) {
                    acting = ((IProxyContext<?>) acting).getActingContext();
                }
                else {
                    acting = null;
                }
            }
            while(acting != null);
        }
        return Pair.of(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> I pipeEncode(IPContext context, O output)
    {
        return (I) encode((T) context, (WsControl<T>) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((T) context, (WsFrame) input);
    }

}
