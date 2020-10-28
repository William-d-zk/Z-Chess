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

package com.isahl.chess.bishop.io.ws.filter;

import java.nio.ByteBuffer;

import com.isahl.chess.bishop.io.ws.IWsContext;
import com.isahl.chess.bishop.io.ws.WsFrame;
import com.isahl.chess.bishop.io.ws.WsProxyContext;
import com.isahl.chess.queen.io.core.async.AioFilterChain;
import com.isahl.chess.queen.io.core.async.AioPacket;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IPacket;
import com.isahl.chess.queen.io.core.inf.IProtocol;
import com.isahl.chess.queen.io.core.inf.IProxyContext;

/**
 * @author william.d.zk
 * 
 * @date 2019-05-07
 */
public class WsProxyFilter<A extends IPContext<A>>
        extends
        AioFilterChain<WsProxyContext<A>,
                       IPacket,
                       WsFrame>
{

    public WsProxyFilter()
    {
        super("ws_proxy");

    }

    @Override
    public ResultType seek(WsProxyContext<A> context, IPacket protocol)
    {
        return context.isOutConvert() ? ResultType.NEXT_STEP
                                      : ResultType.IGNORE;
    }

    @Override
    public ResultType peek(WsProxyContext<A> context, WsFrame input)
    {
        return context.isInConvert() && !input.isCtrl() ? ResultType.NEXT_STEP
                                                        : ResultType.IGNORE;
    }

    @Override
    public WsFrame encode(WsProxyContext<A> context, IPacket output)
    {
        WsFrame frame = new WsFrame();
        frame.setPayload(output.getBuffer()
                               .array());
        frame.setCtrl(WsFrame.frame_op_code_no_ctrl_bin);
        return frame;
    }

    @Override
    public IPacket decode(WsProxyContext<A> context, WsFrame input)
    {
        return new AioPacket(ByteBuffer.wrap(input.getPayload()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends IPContext<C>,
            O extends IProtocol> ResultType pipeSeek(C context, O output)
    {
        if (checkType(output, IProtocol.PACKET_SERIAL)) {
            if (context.isProxy() && context instanceof IWsContext) {
                return seek((WsProxyContext<A>) context, (IPacket) output);
            }
            else if (context.isProxy()) {//SSL|ZLS
                IPContext<?> acting = ((IProxyContext<?>) context).getActingContext();
                if (acting.isProxy() && acting instanceof IWsContext) {
                    return seek((WsProxyContext<A>) acting, (IPacket) output);
                }
            }
        }
        return ResultType.IGNORE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends IPContext<C>,
            I extends IProtocol> ResultType pipePeek(C context, I input)
    {
        if (checkType(input, IProtocol.FRAME_SERIAL)) {
            if (context.isProxy() && context instanceof IWsContext) {
                return peek((WsProxyContext<A>) context, (WsFrame) input);
            }
            else if (context.isProxy()) {
                IPContext<?> acting = ((IProxyContext<?>) context).getActingContext();
                if (acting.isProxy() && acting instanceof IWsContext) {
                    return peek((WsProxyContext<A>) acting, (WsFrame) input);
                }
            }
        }
        return ResultType.IGNORE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends IPContext<C>,
            O extends IProtocol,
            I extends IProtocol> I pipeEncode(C context, O output)
    {

        if (context.isProxy() && context instanceof IWsContext) {
            return (I) encode((WsProxyContext<A>) context, (IPacket) output);
        }
        else if (context.isProxy()) {
            IPContext<?> acting = ((IProxyContext<?>) context).getActingContext();
            return (I) encode((WsProxyContext<A>) acting, (IPacket) output);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends IPContext<C>,
            O extends IProtocol,
            I extends IProtocol> O pipeDecode(C context, I input)
    {
        if (context.isProxy() && context instanceof IWsContext) {
            return (O) decode((WsProxyContext<A>) context, (WsFrame) input);
        }
        else if (context.isProxy()) {
            IPContext<?> acting = ((IProxyContext<?>) context).getActingContext();
            return (O) decode((WsProxyContext<A>) acting, (WsFrame) input);
        }
        return null;
    }

}
