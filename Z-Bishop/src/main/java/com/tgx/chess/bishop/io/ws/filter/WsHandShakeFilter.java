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

package com.tgx.chess.bishop.io.ws.filter;

import static com.tgx.chess.queen.io.core.inf.IContext.DECODE_FRAME;
import static com.tgx.chess.queen.io.core.inf.IContext.ENCODE_FRAME;

import java.nio.ByteBuffer;

import com.tgx.chess.bishop.io.ws.bean.WsContext;
import com.tgx.chess.bishop.io.ws.bean.WsHandshake;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.io.core.async.AioFilterChain;
import com.tgx.chess.queen.io.core.async.AioPacket;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author William.d.zk
 */
public class WsHandShakeFilter
        extends
        AioFilterChain<WsContext,
                       WsHandshake,
                       IPacket>
{
    private final static String CRLF = "\r\n";

    private final ISort _Sort;

    public WsHandShakeFilter(ISort sort)
    {
        super("web-socket-header-zfilter-");
        _Sort = sort;
    }

    @Override
    public ResultType preEncode(WsContext context, IProtocol output)
    {
        return preHandShakeEncode(context, output);
    }

    @Override
    public IPacket encode(WsContext context, WsHandshake output)
    {
        AioPacket encoded = new AioPacket(ByteBuffer.wrap(output.encode()));
        context.setOutState(ENCODE_FRAME);
        return encoded;
    }

    @Override
    public ResultType preDecode(WsContext context, IPacket input)
    {
        ResultType result = preHandShakeDecode(context, input);
        if (ResultType.HANDLED.equals(result)) {
                ByteBuffer recvBuf = input.getBuffer();
                ByteBuffer cRvBuf = context.getRvBuffer();
            int lack = context.lack();
            switch (context.position()){
                case -1:
                case 0:
                case 1:
                    default:

            }


            int position = context.position();
                byte c;
                while (recvBuf.hasRemaining()) {
                    c = recvBuf.get();
                    cRvBuf.put(c);
                    context.lackLength(1, context.position() + 1);
                    if (c == '\n') {
                        String x = new String(cRvBuf.array(), , cRvBuf.position());
                        _Logger.info(x);
                        switch (_Sort.getType())
                        {
                            case SERVER:
                                String[] split = x.split(" ", 2);
                                String httpKey = split[0].toUpperCase();
                                switch (httpKey)
                                {
                                    case "GET":
                                        split = x.split(" ");
                                        if (!split[2].equalsIgnoreCase("HTTP/1.1\r\n")) {
                                            _Logger.warning("http protocol version is low than 1.1");
                                            return ResultType.ERROR;
                                        }
                                        context.updateHandshakeState(WsContext.HS_State_GET);
                                        break;
                                    case "UPGRADE:":
                                        if (!split[1].equalsIgnoreCase("websocket\r\n")) {
                                            _Logger.warning("upgrade no web-socket");
                                            return ResultType.ERROR;
                                        }
                                        context.updateHandshakeState(WsContext.HS_State_UPGRADE);
                                        break;
                                    case "CONNECTION:":
                                        if (!split[1].equalsIgnoreCase("Upgrade\r\n")) {
                                            _Logger.warning("connection no upgrade");
                                            return ResultType.ERROR;
                                        }
                                        context.updateHandshakeState(WsContext.HS_State_CONNECTION);
                                        break;
                                    case "SEC-WEBSOCKET-PROTOCOL:":
                                        if (!split[1].contains("z-push") && !split[1].contains("z-chat")) {
                                            _Logger.warning("sec-websokcet-protocol failed");
                                            return ResultType.ERROR;
                                        }
                                        context.updateHandshakeState(WsContext.HS_State_SEC_PROTOCOL);
                                        break;
                                    case "SEC-WEBSOCKET-VERSION:":
                                        if (split[1].contains("7") || split[1].contains("8")) {
                                            _Logger.info("sec-websokcet-version : %s, ignore", split[1]);
                                        }
                                        if (!split[1].contains("13")) {
                                            _Logger.warning("sec-websokcet-version to low");
                                            return ResultType.ERROR;
                                        }
                                        else {
                                            // just support version 13
                                            context.updateHandshakeState(WsContext.HS_State_SEC_VERSION);
                                        }
                                        break;
                                    case "HOST:":
                                        context.updateHandshakeState(WsContext.HS_State_HOST);
                                        break;
                                    case "ORIGIN:":
                                        context.updateHandshakeState(WsContext.HS_State_ORIGIN);
                                        break;
                                    case "SEC-WEBSOCKET-KEY:":
                                        String sec_key = split[1].replace(CRLF, "");
                                        String sec_accept_expect = context.getSecAccept(sec_key);
                                        handshake.append(String.format("Sec-WebSocket-Accept: %s\r\n",
                                                                       sec_accept_expect));
                                        context.updateHandshakeState(WsContext.HS_State_SEC_KEY);
                                        break;
                                    case CRLF:
                                        handshake.ahead(context.checkState(WsContext.HS_State_CLIENT_OK) ? "HTTP/1.1 101 Switching Protocols\r\n"
                                                                                                         : "HTTP/1.1 400 Bad Request\r\n")
                                                 .append(CRLF);
                                        return ResultType.HANDLED;
                                    default:
                                        break;
                                }
                                break;
                            case CONSUMER:
                                split = x.split(" ", 2);
                                httpKey = split[0].toUpperCase();
                                switch (httpKey)
                                {
                                    case "HTTP/1.1":
                                        if (!split[1].contains("101 Switching Protocols\r\n")) {
                                            _Logger.warning("handshake error !:");
                                            return ResultType.ERROR;
                                        }
                                        context.updateHandshakeState(WsContext.HS_State_HTTP_101);
                                        break;
                                    case "UPGRADE:":
                                        if (!"websocket\r\n".equalsIgnoreCase(split[1])) {
                                            _Logger.warning("upgrade no web-socket");
                                            return ResultType.ERROR;
                                        }
                                        context.updateHandshakeState(WsContext.HS_State_UPGRADE);
                                        break;
                                    case "CONNECTION:":
                                        if (!"Upgrade\r\n".equalsIgnoreCase(split[1])) {
                                            _Logger.warning("connection no upgrade");
                                            return ResultType.ERROR;
                                        }
                                        context.updateHandshakeState(WsContext.HS_State_CONNECTION);
                                        break;
                                    case "SEC-WEBSOCKET-ACCEPT:":
                                        if (!split[1].startsWith(context.getSecAcceptExpect())) {
                                            _Logger.warning("key error: expect-> "
                                                            + context.getSecAcceptExpect()
                                                            + " | result-> "
                                                            + split[1]);
                                            return ResultType.ERROR;
                                        }
                                        context.updateHandshakeState(WsContext.HS_State_SEC_ACCEPT);
                                        break;
                                    case CRLF:
                                        if (context.checkState(WsContext.HS_State_ACCEPT_OK)) {
                                            return ResultType.HANDLED;
                                        }
                                        _Logger.warning("client handshake error!");
                                        return ResultType.ERROR;
                                    default:
                                        break;
                                }
                            default:
                                break;

                        }

                    }
                    if (!recvBuf.hasRemaining()) { return ResultType.NEED_DATA; }
                }
                return ResultType.NEED_DATA;
            
        }
        return result;
    }

    @Override
    public WsHandshake decode(WsContext context, IPacket input)
    {
        WsHandshake handshake = context.getHandshake();
        context.setInState(DECODE_FRAME);
        context.finish();
        return handshake;
    }

}
