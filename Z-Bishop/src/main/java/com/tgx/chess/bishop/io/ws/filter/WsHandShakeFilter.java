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

import static com.tgx.chess.queen.io.core.inf.IContext.DECODE_FRAME;
import static com.tgx.chess.queen.io.core.inf.IContext.ENCODE_FRAME;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.tgx.chess.bishop.io.ws.WsContext;
import com.tgx.chess.bishop.io.ws.WsHandshake;
import com.tgx.chess.bishop.io.ws.control.X101_HandShake;
import com.tgx.chess.bishop.io.zfilter.ZContext;
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
        AioFilterChain<ZContext,
                       WsHandshake,
                       IPacket>
{
    private final static String CRLF     = "\r\n";
    private final static String CRLFCRLF = CRLF + CRLF;

    public WsHandShakeFilter()
    {
        super("ws_header");
    }

    @Override
    public ResultType preEncode(ZContext context, IProtocol output)
    {
        return preHandShakeEncode(context, output);
    }

    @Override
    public IPacket encode(ZContext context, WsHandshake output)
    {
        AioPacket encoded = new AioPacket(ByteBuffer.wrap(output.encode()));
        context.setOutState(ENCODE_FRAME);
        return encoded;
    }

    @Override
    public boolean checkType(IProtocol protocol)
    {
        return checkType(protocol, IPacket.PACKET_SERIAL);
    }

    @Override
    public ResultType preDecode(ZContext context, IPacket input)
    {
        ResultType result = preHandShakeDecode(context, input);
        WsContext wsContext = (WsContext) context;
        ISort<ZContext> sort = context.getSort();
        if (ResultType.HANDLED.equals(result)) {
            ByteBuffer recvBuf = input.getBuffer();
            ByteBuffer cRvBuf = context.getRvBuffer();
            while (recvBuf.hasRemaining()) {
                byte c = recvBuf.get();
                cRvBuf.put(c);
                if (c == '\n' && cRvBuf.position() > 4) {
                    String x = new String(cRvBuf.array(), cRvBuf.position() - 4, 4);
                    if (CRLFCRLF.equals(x)) {
                        cRvBuf.flip();
                        x = new String(cRvBuf.array(), cRvBuf.position(), cRvBuf.limit(), StandardCharsets.UTF_8);
                        _Logger.debug("receive handshake\r\n%s", x);
                        String[] split = x.split(CRLF);
                        StringBuilder response = new StringBuilder();
                        for (String row : split) {
                            String[] rowSplit = row.split("\\s+", 2);
                            String httpKey = rowSplit[0].toUpperCase();
                            switch (sort.getType())
                            {
                                case SERVER:
                                    switch (httpKey)
                                    {
                                        case "GET":
                                            rowSplit = row.split("\\s+");
                                            if (!"HTTP/1.1".equalsIgnoreCase(rowSplit[2])) {
                                                _Logger.warning("http protocol version is low than 1.1");
                                                return ResultType.ERROR;
                                            }
                                            wsContext.updateHandshakeState(WsContext.HS_State_GET);
                                            break;
                                        case "UPGRADE:":
                                            if (!"websocket".equalsIgnoreCase(rowSplit[1])) {
                                                _Logger.warning("upgrade no web-socket");
                                                return ResultType.ERROR;
                                            }
                                            wsContext.updateHandshakeState(WsContext.HS_State_UPGRADE);
                                            response.append(row)
                                                    .append(CRLF);
                                            break;
                                        case "CONNECTION:":
                                            if (!"Upgrade".equalsIgnoreCase(rowSplit[1])) {
                                                _Logger.warning("connection no upgrade");
                                                return ResultType.ERROR;
                                            }
                                            wsContext.updateHandshakeState(WsContext.HS_State_CONNECTION);
                                            response.append(row)
                                                    .append(CRLF);
                                            break;
                                        case "SEC-WEBSOCKET-PROTOCOL:":
                                            if (!rowSplit[1].contains("z-push")
                                                && !rowSplit[1].contains("z-chat")
                                                && !rowSplit[1].contains("mqtt"))
                                            {
                                                _Logger.warning("sec-websokcet-protocol failed");
                                                return ResultType.ERROR;
                                            }
                                            wsContext.updateHandshakeState(WsContext.HS_State_SEC_PROTOCOL);
                                            response.append(row)
                                                    .append(CRLF);
                                            break;
                                        case "SEC-WEBSOCKET-VERSION:":
                                            if (rowSplit[1].contains("7") || rowSplit[1].contains("8")) {
                                                _Logger.debug("sec-websokcet-version : %s, ignore", rowSplit[1]);
                                            }
                                            if (!rowSplit[1].contains("13")) {
                                                _Logger.warning("sec-websokcet-version to low");
                                                return ResultType.ERROR;
                                            }
                                            else {
                                                // just support version 13
                                                wsContext.updateHandshakeState(WsContext.HS_State_SEC_VERSION);
                                                response.append(row)
                                                        .append(CRLF);
                                            }
                                            break;
                                        case "HOST:":
                                            wsContext.updateHandshakeState(WsContext.HS_State_HOST);
                                            response.append(row)
                                                    .append(CRLF);
                                            break;
                                        case "ORIGIN:":
                                            wsContext.updateHandshakeState(WsContext.HS_State_ORIGIN);
                                            response.append(row)
                                                    .append(CRLF);
                                            break;
                                        case "SEC-WEBSOCKET-KEY:":
                                            wsContext.updateHandshakeState(WsContext.HS_State_SEC_KEY);
                                            response.append(String.format("Sec-WebSocket-Accept: %s\r\n",
                                                                          wsContext.getSecAccept(rowSplit[1])));
                                            break;
                                        default:
                                            _Logger.warning("unchecked httpKey and content: [%s %s]",
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
                                            wsContext.updateHandshakeState(WsContext.HS_State_HTTP_101);
                                            break;
                                        case "UPGRADE:":
                                            if (!"websocket".equalsIgnoreCase(rowSplit[1])) {
                                                _Logger.warning("upgrade no web-socket");
                                                return ResultType.ERROR;
                                            }
                                            wsContext.updateHandshakeState(WsContext.HS_State_UPGRADE);
                                            break;
                                        case "CONNECTION:":
                                            if (!"Upgrade".equalsIgnoreCase(rowSplit[1])) {
                                                _Logger.warning("connection no upgrade");
                                                return ResultType.ERROR;
                                            }
                                            wsContext.updateHandshakeState(WsContext.HS_State_CONNECTION);
                                            break;
                                        case "SEC-WEBSOCKET-ACCEPT:":
                                            if (!rowSplit[1].startsWith(wsContext.getSecAcceptExpect())) {
                                                _Logger.warning("key error: expect-> "
                                                                + wsContext.getSecAcceptExpect()
                                                                + " | result-> "
                                                                + rowSplit[1]);
                                                return ResultType.ERROR;
                                            }
                                            wsContext.updateHandshakeState(WsContext.HS_State_SEC_ACCEPT);
                                            break;
                                        default:
                                            _Logger.debug("unchecked httpKey and content: [%s %s]",
                                                          httpKey,
                                                          rowSplit[1]);
                                            break;
                                    }
                                    break;
                                default:
                                    _Logger.warning("cluster handshake ? session initialize error!");
                                    return ResultType.ERROR;
                            }
                        }
                        if (ISort.Type.SERVER.equals(sort.getType())) {
                            wsContext.setHandshake(new X101_HandShake((wsContext.checkState(WsContext.HS_State_CLIENT_OK) ? "HTTP/1.1 101 Switching Protocols\r\n"
                                                                                                                          : "HTTP/1.1 400 Bad Request\r\n")
                                                                      + response.toString()
                                                                      + CRLF));
                            return ResultType.HANDLED;
                        }
                        else if (ISort.Type.CONSUMER.equals(sort.getType())) {
                            if (wsContext.checkState(WsContext.HS_State_ACCEPT_OK)) {
                                wsContext.setHandshake(new X101_HandShake(x));
                                return ResultType.HANDLED;
                            }
                            _Logger.warning("client handshake error!");
                            return ResultType.ERROR;
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
    public WsHandshake decode(ZContext context, IPacket input)
    {
        WsHandshake handshake = ((WsContext) context).getHandshake();
        context.setInState(DECODE_FRAME);
        context.finish();
        return handshake;
    }
}