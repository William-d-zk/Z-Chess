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

package com.tgx.chess.bishop.io.ws.filter;

import java.nio.ByteBuffer;

import com.tgx.chess.bishop.io.ws.WsFrame;
import com.tgx.chess.bishop.io.ws.WsProxy;
import com.tgx.chess.bishop.io.ws.WsProxyContext;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.king.base.exception.ZException;
import com.tgx.chess.queen.io.core.async.AioFilterChain;
import com.tgx.chess.queen.io.core.async.AioPacket;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IFilter;
import com.tgx.chess.queen.io.core.inf.IFilterChain;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author william.d.zk
 * @date 2019-05-07
 */
public class WsProxyFilter
        extends
        AioFilterChain<ZContext,
                       IProtocol,
                       WsFrame>
{

    public <T extends IProtocol> WsProxyFilter(AioFilterChain<ZContext,
                                                              T,
                                                              IPacket> filterChain)
    {
        super("ws_proxy");
        _ActualFilterChain = filterChain;
    }

    private final AioFilterChain<ZContext,
                                 ? extends IProtocol,
                                 IPacket> _ActualFilterChain;

    @Override
    public boolean checkType(IProtocol protocol)
    {
        return checkType(protocol, IPacket.FRAME_SERIAL);
    }

    @Override
    public ResultType preEncode(ZContext context, IProtocol output)
    {
        return preProxyEncode(context, output);
    }

    @Override
    public WsFrame encode(ZContext context, IProtocol output)
    {
        WsProxyContext proxyContext = (WsProxyContext) context;
        ZContext actualContext = proxyContext.getProxyContext();
        IFilter.ResultType resultType;
        IFilterChain<ZContext> previous = _ActualFilterChain.getChainTail();
        IProtocol toWrite = output;
        while (previous != null) {
            resultType = previous.getFilter()
                                 .preEncode(actualContext, toWrite);
            switch (resultType)
            {
                case ERROR:
                case NEED_DATA:
                    return null;
                case NEXT_STEP:
                    toWrite = previous.getFilter()
                                      .encode(actualContext, toWrite);
                    break;
                default:
                    break;
            }
            previous = previous.getPrevious();
        }
        IPacket packet = (IPacket) toWrite;
        WsFrame frame = new WsFrame();
        frame.setPayload(packet.getBuffer()
                               .array());
        frame.setCtrl(WsFrame.frame_op_code_no_ctrl_bin);
        return frame;
    }

    @Override
    public ResultType preDecode(ZContext context, WsFrame input)
    {
        return preProxyDecode(context, input);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IProtocol decode(ZContext context, WsFrame input)
    {
        WsProxyContext proxyContext = (WsProxyContext) context;
        ZContext actualContext = proxyContext.getProxyContext();
        if (input == null || input.getPayload() == null) { return null; }
        ByteBuffer buf = ByteBuffer.wrap(input.getPayload());
        IPacket packet = new AioPacket(buf);
        IFilter.ResultType resultType;
        IProtocol protocol = packet;
        WsProxy<ZContext> proxy = null;
        for (IFilterChain<ZContext> next = _ActualFilterChain;; next = _ActualFilterChain, protocol = packet) {
            Chain:
            while (next != null) {
                resultType = next.getFilter()
                                 .checkType(protocol) ? next.getFilter()
                                                            .preDecode(actualContext, protocol)
                                                      : IFilter.ResultType.IGNORE;
                switch (resultType)
                {
                    case ERROR:
                        throw new ZException(String.format("filter:%s error", next.getName()));
                    case NEED_DATA:
                        return proxy;
                    case NEXT_STEP:
                        protocol = next.getFilter()
                                       .decode(actualContext, protocol);
                        break;
                    case HANDLED:
                        IControl<ZContext> cmd = (IControl<ZContext>) next.getFilter()
                                                                          .decode(actualContext, protocol);
                        if (cmd != null) {
                            if (proxy == null) {
                                proxy = new WsProxy<>();
                            }
                            proxy.add(cmd);
                        }
                        break Chain;
                    default:
                        break;
                }
                next = next.getNext();
            }
        }
    }
}
