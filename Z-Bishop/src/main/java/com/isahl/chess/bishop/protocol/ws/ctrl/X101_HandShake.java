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
package com.isahl.chess.bishop.protocol.ws.ctrl;

import com.isahl.chess.bishop.protocol.ws.WsContext;
import com.isahl.chess.bishop.protocol.ws.features.IWsContext;
import com.isahl.chess.bishop.protocol.ws.model.WsControl;
import com.isahl.chess.bishop.protocol.ws.model.WsFrame;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.session.ISort;

import java.nio.charset.StandardCharsets;

import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE;
import static java.lang.String.format;

/**
 * @author William.d.zk
 * @date 2017-01-12
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_CONTROL_SERIAL,
                  serial = 0x101)
public class X101_HandShake
        extends WsControl
{

    private static Logger _Logger = Logger.getLogger("bishop.protocol." + X101_HandShake.class.getSimpleName());

    public X101_HandShake()
    {
        super(WsFrame.frame_op_code_ctrl_handshake);
    }

    @Override
    public String toString()
    {
        return format("%s", new String(mPayload, StandardCharsets.UTF_8));
    }

    private int      mCode;
    private String[] mSupportedVersions;

    public boolean isClientOk()
    {
        return mCode == IWsContext.HS_State_CLIENT_OK;
    }

    public boolean isServerAccept()
    {
        return mCode == IWsContext.HS_State_ACCEPT_OK;
    }

    @Override
    public Level getLevel()
    {
        return ALMOST_ONCE;
    }

    public X101_HandShake(String host, String secKey, int version)
    {
        this();
        mPayload = format(REQUEST_TEMPLATE, host, secKey, host, version).getBytes(StandardCharsets.UTF_8);
        mCode = IWsContext.HS_State_CONNECTION;
    }

    public X101_HandShake(String response, int code)
    {
        this();
        mPayload = response.getBytes(StandardCharsets.UTF_8);
        mCode = code;
    }

    private final static String REQUEST_TEMPLATE = """
            GET /ws_service HTTP/1.1\r
            Host: %s\r
            Upgrade: websocket\r
            Connection: Upgrade\r
            Sec-WebSocket-Key: %s\r
            Origin: http://%s\r
            Sec-WebSocket-Protocol: z-push, z-chat\r
            Sec-WebSocket-Version: %s\r
            \r
            """;
    private final static int    CRLF_CRLF        = IoUtil.readInt(new byte[]{
            '\r',
            '\n',
            '\r',
            '\n'
    }, 0);
    private final static String CRLF             = "\r\n";

    @Override
    public int lack(ByteBuf input)
    {
        int remain = input.readableBytes();
        if(remain < 4) {
            return 4;
        }
        else {
            for(int offset = 0, end; input.isOffsetReadable(offset + 3); offset++) {
                end = input.get(offset);
                end = (end << 8) | input.get(offset + 1);
                end = (end << 8) | input.get(offset + 2);
                end = (end << 8) | input.get(offset + 3);
                if(end == CRLF_CRLF) {
                    input.markReader();
                    return 0;
                }
            }
            return 1;
        }
    }

    @Override
    public int prefix(ByteBuf input)
    {
        StringBuilder response = new StringBuilder();
        for(int mark = input.readerMark(); input.readerIdx() <= mark; ) {
            String row = input.readLine();
            String[] split = row.split("\\s+", 2);
            String httpKey = split[0].toUpperCase();
            switch(context().getType()) {
                case SERVER -> {
                    switch(httpKey) {
                        case "GET" -> {
                            context().updateHandshakeState(WsContext.HS_State_GET);
                            split = row.split("\\s+");
                            if(!"HTTP/1.1".equalsIgnoreCase(split[2])) {
                                context().updateHandshakeState(WsContext.HS_State_ERROR);
                            }
                        }
                        case "UPGRADE:" -> {
                            context().updateHandshakeState(WsContext.HS_State_UPGRADE);
                            if(!"websocket".equalsIgnoreCase(split[1])) {
                                context().updateHandshakeState(WsContext.HS_State_ERROR);
                            }
                            else {
                                response.append(row)
                                        .append(CRLF);
                            }
                        }
                        case "CONNECTION:" -> {
                            context().updateHandshakeState(WsContext.HS_State_CONNECTION);
                            if(!"Upgrade".equalsIgnoreCase(split[1])) {
                                context().updateHandshakeState(WsContext.HS_State_ERROR);
                            }
                            else {
                                response.append(row)
                                        .append(CRLF);
                            }
                        }
                        case "SEC-WEBSOCKET-PROTOCOL:" -> {
                            context().updateHandshakeState(WsContext.HS_State_SEC_PROTOCOL);
                            if(!split[1].contains("z-chat") && !split[1].contains("mqtt")) {
                                context().updateHandshakeState(WsContext.HS_State_ERROR);
                            }
                            else {
                                response.append(row)
                                        .append(CRLF);
                            }
                        }
                        case "SEC-WEBSOCKET-VERSION:" -> {
                            context().updateHandshakeState(WsContext.HS_State_SEC_VERSION);
                            split = split[1].split("\\s+,\\s+");
                            if(mSupportedVersions == null) {
                                mSupportedVersions = new String[split.length];
                                IoUtil.addArray(split, mSupportedVersions);
                            }
                            else {
                                String[] tmp = new String[split.length + mSupportedVersions.length];
                                IoUtil.addArray(mSupportedVersions, tmp, split);
                                mSupportedVersions = tmp;
                            }
                            response.append(row)
                                    .append(CRLF);
                        }
                        case "HOST:" -> {
                            context().updateHandshakeState(WsContext.HS_State_HOST);
                            response.append(row)
                                    .append(CRLF);
                        }
                        case "ORIGIN:" -> {
                            context().updateHandshakeState(WsContext.HS_State_ORIGIN);
                            response.append(row)
                                    .append(CRLF);
                        }
                        case "SEC-WEBSOCKET-KEY:" -> {
                            context().updateHandshakeState(WsContext.HS_State_SEC_KEY);
                            response.append(format("Sec-WebSocket-Accept: %s\r\n", context().getSecAccept(split[1])));
                        }
                        default -> {
                            _Logger.debug("server ignore default: %s", row);
                        }
                    }
                }
                case CLIENT -> {
                    switch(httpKey) {
                        case "HTTP/1.1" -> {
                            context().updateHandshakeState(WsContext.HS_State_HTTP_101);
                            if(!split[1].contains("101 Switching Protocols")) {
                                context().updateHandshakeState(WsContext.HS_State_ERROR);
                            }
                        }
                        case "UPGRADE:" -> {
                            context().updateHandshakeState(WsContext.HS_State_UPGRADE);
                            if(!"websocket".equalsIgnoreCase(split[1])) {
                                context().updateHandshakeState(WsContext.HS_State_ERROR);
                            }
                        }
                        case "CONNECTION:" -> {
                            context().updateHandshakeState(WsContext.HS_State_CONNECTION);
                            if(!"Upgrade".equalsIgnoreCase(split[1])) {
                                context().updateHandshakeState(WsContext.HS_State_ERROR);
                            }
                        }
                        case "SEC-WEBSOCKET-ACCEPT:" -> {
                            context().updateHandshakeState(WsContext.HS_State_SEC_ACCEPT);
                            if(!split[1].startsWith(context().getSecAcceptExpect())) {
                                context().updateHandshakeState(WsContext.HS_State_ERROR);
                            }
                        }
                        default -> {
                            _Logger.debug("client ignore default: %s", row);
                        }
                    }
                }
            }
        }
        if(ISort.Type.SERVER == context().getType()) {
            String toClient =
                    context().checkState(WsContext.HS_State_CLIENT_OK) ? "HTTP/1.1 101 Switching Protocols" + CRLF
                                                                       : "HTTP/1.1 400 Bad Request" + CRLF;
            context().setHandshake(new X101_HandShake(toClient + response.append(CRLF),
                                                      context().checkState(WsContext.HS_State_CLIENT_OK)
                                                      ? WsContext.HS_State_CLIENT_OK : WsContext.HS_State_ERROR));
        }
        else if(ISort.Type.CLIENT == context().getType()) {
            if(context().checkState(WsContext.HS_State_ACCEPT_OK)) {
                context().setHandshake(new X101_HandShake(response.toString(), WsContext.HS_State_ACCEPT_OK));
            }

        }
        int remain = input.readableBytes();
        if(remain > 0) {
            _Logger.warning("handshake! remain [%d]");
        }
        return 0;
    }
}
