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
import java.nio.charset.StandardCharsets;

import com.tgx.chess.bishop.io.ws.bean.WsContext;
import com.tgx.chess.bishop.io.ws.bean.WsHandshake;
import com.tgx.chess.bishop.io.ws.control.X101_HandShake;
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
    private final static String CRLF     = "\r\n";
    private final static String CRLFCRLF = CRLF + CRLF;

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
            while (recvBuf.hasRemaining()) {
                byte c = recvBuf.get();
                cRvBuf.put(c);
                if (c == '\n') {
                    String x = new String(cRvBuf.array(), cRvBuf.position() - 4, cRvBuf.position());
                    if (CRLFCRLF.equals(x)) {
                        cRvBuf.flip();
                        x = new String(cRvBuf.array(), cRvBuf.position(), cRvBuf.limit(), StandardCharsets.UTF_8);
                        _Logger.info("receive handshake %s", x);
                        String[] split = x.split(CRLF);
                        StringBuilder response = new StringBuilder();
                        for (String row : split) {
                            String[] rowSplit = row.split("\\s*", 2);
                            String httpKey = rowSplit[0].toUpperCase();
                            switch (_Sort.getType())
                            {
                                case SERVER:
                                    switch (httpKey)
                                    {
                                        case "GET":
                                            rowSplit = row.split("\\s");
                                            if (!"HTTP/1.1".equalsIgnoreCase(rowSplit[2])) {
                                                _Logger.warning("http protocol version is low than 1.1");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_GET);
                                            break;
                                        case "UPGRADE:":
                                            if (!"websocket".equalsIgnoreCase(rowSplit[1])) {
                                                _Logger.warning("upgrade no web-socket");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_UPGRADE);
                                            response.append(row)
                                                    .append(CRLF);
                                            break;
                                        case "CONNECTION:":
                                            if (!"Upgrade".equalsIgnoreCase(rowSplit[1])) {
                                                _Logger.warning("connection no upgrade");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_CONNECTION);
                                            response.append(row)
                                                    .append(CRLF);
                                            break;
                                        case "SEC-WEBSOCKET-PROTOCOL:":
                                            if (!rowSplit[1].contains("z-push") && !rowSplit[1].contains("z-chat")) {
                                                _Logger.warning("sec-websokcet-protocol failed");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_SEC_PROTOCOL);
                                            response.append(row)
                                                    .append(CRLF);
                                            break;
                                        case "SEC-WEBSOCKET-VERSION:":
                                            if (rowSplit[1].contains("7") || rowSplit[1].contains("8")) {
                                                _Logger.info("sec-websokcet-version : %s, ignore", rowSplit[1]);
                                            }
                                            if (!rowSplit[1].contains("13")) {
                                                _Logger.warning("sec-websokcet-version to low");
                                                return ResultType.ERROR;
                                            }
                                            else {
                                                // just support version 13
                                                context.updateHandshakeState(WsContext.HS_State_SEC_VERSION);
                                                response.append(row)
                                                        .append(CRLF);
                                            }
                                            break;
                                        case "HOST:":
                                            context.updateHandshakeState(WsContext.HS_State_HOST);
                                            response.append(row)
                                                    .append(CRLF);
                                            break;
                                        case "ORIGIN:":
                                            context.updateHandshakeState(WsContext.HS_State_ORIGIN);
                                            response.append(row)
                                                    .append(CRLF);
                                            break;
                                        case "SEC-WEBSOCKET-KEY:":
                                            String sec_key = rowSplit[1];
                                            String sec_accept_expect = context.getSecAccept(sec_key);
                                            context.updateHandshakeState(WsContext.HS_State_SEC_KEY);
                                            response.append(String.format("Sec-WebSocket-Accept: %s\r\n",
                                                                          sec_accept_expect));
                                            break;
                                        case "":
                                            context.setHandshake(new X101_HandShake((context.checkState(WsContext.HS_State_CLIENT_OK) ? "HTTP/1.1 101 Switching Protocols\r\n"
                                                                                                                                      : "HTTP/1.1 400 Bad Request\r\n")
                                                                                    + response.toString()
                                                                                    + CRLF));
                                            return ResultType.HANDLED;
                                        default:
                                            _Logger.info("unchecked httpKey and content: [%s %s]",
                                                         httpKey,
                                                         rowSplit[1]);
                                            break;
                                    }
                                    break;
                                case CONSUMER:
                                    switch (httpKey)
                                    {
                                        case "HTTP/1.1":
                                            if (!rowSplit[1].contains("101 Switching Protocols")) {
                                                _Logger.warning("handshake error !:");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_HTTP_101);
                                            break;
                                        case "UPGRADE:":
                                            if (!"websocket".equalsIgnoreCase(rowSplit[1])) {
                                                _Logger.warning("upgrade no web-socket");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_UPGRADE);
                                            break;
                                        case "CONNECTION:":
                                            if (!"Upgrade".equalsIgnoreCase(rowSplit[1])) {
                                                _Logger.warning("connection no upgrade");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_CONNECTION);
                                            break;
                                        case "SEC-WEBSOCKET-ACCEPT:":
                                            if (!rowSplit[1].startsWith(context.getSecAcceptExpect())) {
                                                _Logger.warning("key error: expect-> "
                                                                + context.getSecAcceptExpect()
                                                                + " | result-> "
                                                                + rowSplit[1]);
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_SEC_ACCEPT);
                                            break;
                                        case "":
                                            if (context.checkState(WsContext.HS_State_ACCEPT_OK)) {
                                                context.setHandshake(new X101_HandShake(x));
                                                return ResultType.HANDLED;
                                            }
                                            _Logger.warning("client handshake error!");
                                            return ResultType.ERROR;
                                        default:
                                            _Logger.info("unchecked httpKey and content: [%s %s]",
                                                         httpKey,
                                                         rowSplit[1]);
                                            break;
                                    }
                                default:
                                    break;
                            }
                        }

                    }
                    if (!recvBuf.hasRemaining()) { return ResultType.NEED_DATA; }
                }
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