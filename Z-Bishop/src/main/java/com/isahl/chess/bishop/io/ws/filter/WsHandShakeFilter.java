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

import static com.isahl.chess.bishop.io.ws.IWsContext.HS_State_ACCEPT_OK;
import static com.isahl.chess.bishop.io.ws.IWsContext.HS_State_CLIENT_OK;
import static com.isahl.chess.bishop.io.ws.IWsContext.HS_State_ERROR;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.isahl.chess.bishop.io.ZContext;
import com.isahl.chess.bishop.io.ws.IWsContext;
import com.isahl.chess.bishop.io.ws.WsContext;
import com.isahl.chess.bishop.io.ws.WsHandshake;
import com.isahl.chess.bishop.io.ws.control.X101_HandShake;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.event.inf.ISort;
import com.isahl.chess.queen.io.core.async.AioFilterChain;
import com.isahl.chess.queen.io.core.async.AioPacket;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IPacket;
import com.isahl.chess.queen.io.core.inf.IProtocol;
import com.isahl.chess.queen.io.core.inf.IProxyContext;

/**
 * @author William.d.zk
 */
public class WsHandShakeFilter<T extends ZContext & IWsContext>
        extends
        AioFilterChain<T,
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
    public IPacket encode(T context, WsHandshake output)
    {
        if (output.isClientOk() || output.isServerAccept()) {
            context.updateOut();
        }
        return new AioPacket(ByteBuffer.wrap(output.encode()));
    }

    @Override
    public ResultType peek(T context, IPacket input)
    {
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
                        switch (context.getType())
                        {
                            case SERVER:
                                switch (httpKey)
                                {
                                    case "GET" ->
                                        {
                                            rowSplit = row.split("\\s+");
                                            if (!"HTTP/1.1".equalsIgnoreCase(rowSplit[2])) {
                                                _Logger.warning("http protocol version is low than 1.1");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_GET);
                                        }
                                    case "UPGRADE:" ->
                                        {
                                            if (!"websocket".equalsIgnoreCase(rowSplit[1])) {
                                                _Logger.warning("upgrade no web-socket");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_UPGRADE);
                                            response.append(row)
                                                    .append(CRLF);
                                        }
                                    case "CONNECTION:" ->
                                        {
                                            if (!"Upgrade".equalsIgnoreCase(rowSplit[1])) {
                                                _Logger.warning("connection no upgrade");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_CONNECTION);
                                            response.append(row)
                                                    .append(CRLF);
                                        }
                                    case "SEC-WEBSOCKET-PROTOCOL:" ->
                                        {
                                            if (!rowSplit[1].contains("z-push")
                                                && !rowSplit[1].contains("z-chat")
                                                && !rowSplit[1].contains("mqtt"))
                                            {
                                                _Logger.warning("sec-websokcet-protocol failed");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_SEC_PROTOCOL);
                                            response.append(row)
                                                    .append(CRLF);
                                        }
                                    case "SEC-WEBSOCKET-VERSION:" ->
                                        {
                                            if (rowSplit[1].contains("7") || rowSplit[1].contains("8")) {
                                                _Logger.debug("sec-websokcet-version : %s, ignore", rowSplit[1]);
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
                                        }
                                    case "HOST:" ->
                                        {
                                            context.updateHandshakeState(WsContext.HS_State_HOST);
                                            response.append(row)
                                                    .append(CRLF);
                                        }
                                    case "ORIGIN:" ->
                                        {
                                            context.updateHandshakeState(WsContext.HS_State_ORIGIN);
                                            response.append(row)
                                                    .append(CRLF);
                                        }
                                    case "SEC-WEBSOCKET-KEY:" ->
                                        {
                                            context.updateHandshakeState(WsContext.HS_State_SEC_KEY);
                                            response.append(String.format("Sec-WebSocket-Accept: %s\r\n",
                                                                          context.getSecAccept(rowSplit[1])));
                                        }
                                    default -> _Logger.warning("unchecked httpKey and content: [%s %s]",
                                                               httpKey,
                                                               rowSplit[1]);
                                }
                                break;
                            case CONSUMER:
                                switch (httpKey)
                                {
                                    case "HTTP/1.1" ->
                                        {
                                            if (!rowSplit[1].contains("101 Switching Protocols")) {
                                                _Logger.warning("handshake error !:");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_HTTP_101);
                                        }
                                    case "UPGRADE:" ->
                                        {
                                            if (!"websocket".equalsIgnoreCase(rowSplit[1])) {
                                                _Logger.warning("upgrade no web-socket");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_UPGRADE);
                                        }
                                    case "CONNECTION:" ->
                                        {
                                            if (!"Upgrade".equalsIgnoreCase(rowSplit[1])) {
                                                _Logger.warning("connection no upgrade");
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_CONNECTION);
                                        }
                                    case "SEC-WEBSOCKET-ACCEPT:" ->
                                        {
                                            if (!rowSplit[1].startsWith(context.getSecAcceptExpect())) {
                                                _Logger.warning("key error: expect-> "
                                                                + context.getSecAcceptExpect()
                                                                + " | result-> "
                                                                + rowSplit[1]);
                                                return ResultType.ERROR;
                                            }
                                            context.updateHandshakeState(WsContext.HS_State_SEC_ACCEPT);
                                        }
                                    default -> _Logger.debug("unchecked httpKey and content: [%s %s]",
                                                             httpKey,
                                                             rowSplit[1]);
                                }
                                break;
                            default:
                                _Logger.warning("cluster handshake ? session initialize error!");
                                return ResultType.ERROR;
                        }
                    }
                    if (ISort.Type.SERVER == context.getType()) {
                        context.setHandshake(new X101_HandShake((context.checkState(HS_State_CLIENT_OK) ? "HTTP/1.1 101 Switching Protocols\r\n"
                                                                                                        : "HTTP/1.1 400 Bad Request\r\n")
                                                                + response.toString()
                                                                + CRLF,
                                                                context.checkState(HS_State_CLIENT_OK) ? HS_State_CLIENT_OK
                                                                                                       : HS_State_ERROR));
                        return ResultType.HANDLED;
                    }
                    else if (ISort.Type.CONSUMER == context.getType()) {
                        if (context.checkState(HS_State_ACCEPT_OK)) {
                            context.setHandshake(new X101_HandShake(x, HS_State_ACCEPT_OK));
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

    @Override
    public WsHandshake decode(T context, IPacket input)
    {
        WsHandshake handshake = context.getHandshake();
        context.finish();
        if (handshake.isClientOk() || handshake.isServerAccept()) {
            context.updateIn();
        }
        return handshake;
    }

    @Override
    public <O extends IProtocol> Pair<ResultType,
                                      IPContext> pipeSeek(IPContext context, O output)
    {
        //WsHandshake 继承自WsControl
        if (checkType(output, IProtocol.CONTROL_SERIAL) && output instanceof WsHandshake) {
            if (context instanceof IWsContext && context.isOutFrame()) {
                return new Pair<>(ResultType.NEXT_STEP, context);
            }
            IPContext acting = context;
            while (acting.isProxy()) {
                if (acting instanceof IWsContext && acting.isOutFrame()) {
                    return new Pair<>(ResultType.NEXT_STEP, acting);
                }
                acting = ((IProxyContext<?>) acting).getActingContext();
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends IProtocol> Pair<ResultType,
                                      IPContext> pipePeek(IPContext context, I input)
    {
        if (checkType(input, IProtocol.PACKET_SERIAL)) {
            if (context instanceof IWsContext && context.isInFrame()) {
                return new Pair<>(peek((T) context, (IPacket) input), context);
            }
            IPContext acting = context;
            while (acting.isProxy()) {
                if (acting instanceof IWsContext && acting.isInFrame()) {
                    return new Pair<>(peek((T) acting, (IPacket) input), acting);
                }
                acting = ((IProxyContext<?>) acting).getActingContext();
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol,
            I extends IProtocol> I pipeEncode(IPContext context, O output)
    {
        /*
         * SSL->WS->MQTT
         * WS->MQTT 
         * 代理结构时，需要区分 context 是否为IWsContext
         */
        return (I) encode((T) context, (WsHandshake) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol,
            I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((T) context, (IPacket) input);
    }
}