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
package com.isahl.chess.bishop.io.ws.filter;

import com.isahl.chess.bishop.io.ws.ctrl.X101_HandShake;
import com.isahl.chess.bishop.io.ws.ctrl.X102_Close;
import com.isahl.chess.bishop.io.ws.ctrl.X103_Ping;
import com.isahl.chess.bishop.io.ws.ctrl.X104_Pong;
import com.isahl.chess.bishop.io.ws.features.IWsContext;
import com.isahl.chess.bishop.io.ws.model.WsControl;
import com.isahl.chess.bishop.io.ws.model.WsFrame;
import com.isahl.chess.bishop.io.ws.zchat.ZContext;
import com.isahl.chess.bishop.io.ws.zchat.model.ctrl.X106_Identity;
import com.isahl.chess.bishop.io.ws.zchat.model.ctrl.X107_Redirect;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.content.IFrame;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IProxyContext;
import com.isahl.chess.queen.io.core.net.socket.AioFilterChain;

/**
 * @author William.d.zk
 */
public class WsControlFilter<T extends ZContext & IWsContext>
        extends AioFilterChain<T, WsControl, WsFrame>
{
    public WsControlFilter()
    {
        super("ws_control");
    }

    @Override
    public WsFrame encode(T context, WsControl output)
    {
        WsFrame frame = new WsFrame();
        _Logger.debug("control %s", output);
        frame.putPayload(output.payload());
        frame.putCtrl(output.ctrl());
        return frame;
    }

    @Override
    public ResultType peek(T context, WsFrame input)
    {
        return context.isInConvert() && input.isCtrl() ? ResultType.HANDLED : ResultType.IGNORE;
    }

    @Override
    public WsControl decode(T context, WsFrame input)
    {
        return switch(input.frame_op_code & 0x0F) {
            case WsFrame.frame_op_code_ctrl_close -> new X102_Close(input.payload());
            case WsFrame.frame_op_code_ctrl_ping -> new X103_Ping(input.payload());
            case WsFrame.frame_op_code_ctrl_pong -> new X104_Pong(input.payload());
            case WsFrame.frame_op_code_ctrl_cluster -> new X106_Identity(input.payload());
            case WsFrame.frame_op_code_ctrl_redirect -> new X107_Redirect(input.payload());
            default -> throw new UnsupportedOperationException(String.format("web socket frame with control code %d.",
                                                                             input.frame_op_code & 0x0F));
        };
    }

    @Override
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.CONTROL_SERIAL) && output instanceof WsControl) {
            if(context instanceof IWsContext && context.isOutConvert()) {
                return new Pair<>(output.serial() != X101_HandShake.COMMAND ? ResultType.NEXT_STEP : ResultType.ERROR,
                                  context);
            }
            IPContext acting = context;
            while(acting.isProxy() || acting instanceof IWsContext) {
                if(acting instanceof IWsContext && acting.isOutConvert()) {
                    return new Pair<>(
                            output.serial() != X101_HandShake.COMMAND ? ResultType.NEXT_STEP : ResultType.ERROR,
                            acting);
                }
                else if(acting.isProxy()) {
                    acting = ((IProxyContext<?>) acting).getActingContext();
                }
                else {break;}
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.FRAME_SERIAL) && input instanceof WsFrame && ((IFrame) input).isCtrl()) {
            if(context instanceof IWsContext && context.isInConvert()) {
                return new Pair<>(ResultType.HANDLED, context);
            }
            IPContext acting = context;
            while(acting.isProxy() || acting instanceof IWsContext) {
                if(acting instanceof IWsContext && acting.isInConvert()) {
                    return new Pair<>(ResultType.HANDLED, acting);
                }
                else if(acting.isProxy()) {
                    acting = ((IProxyContext<?>) acting).getActingContext();
                }
                else {break;}
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> I pipeEncode(IPContext context, O output)
    {
        return (I) encode((T) context, (WsControl) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((T) context, (WsFrame) input);
    }

}
