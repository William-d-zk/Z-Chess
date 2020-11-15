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

package com.isahl.chess.bishop.io.ws;

import java.util.Base64;

import com.isahl.chess.king.base.util.CryptUtil;

public interface IWsContext
{
    int HS_State_GET          = 1;
    int HS_State_HOST         = 1 << 1;
    int HS_State_UPGRADE      = 1 << 2;
    int HS_State_CONNECTION   = 1 << 3;
    int HS_State_SEC_KEY      = 1 << 4;
    int HS_State_ORIGIN       = 1 << 5;
    int HS_State_SEC_PROTOCOL = 1 << 6;
    // public final String mSecProtocol, mSubProtocol; //not support right now
    int HS_State_SEC_VERSION = 1 << 7;
    int HS_State_HTTP_101    = 1 << 8;
    int HS_State_SEC_ACCEPT  = 1 << 9;
    int HS_State_ACCEPT_OK   = HS_State_HTTP_101 | HS_State_SEC_ACCEPT | HS_State_UPGRADE | HS_State_CONNECTION;
    int HS_State_CLIENT_OK   = HS_State_GET
                               | HS_State_HOST
                               | HS_State_UPGRADE
                               | HS_State_CONNECTION
                               | HS_State_SEC_KEY
                               | HS_State_SEC_VERSION;
    int HS_State_ERROR       = -1 << 31;

    WsHandshake getHandshake();

    void setHandshake(WsHandshake handshake);

    int getMaxPayloadSize();

    default String getSecAccept(String secKey)
    {
        return Base64.getEncoder()
                     .encodeToString(CryptUtil.SHA1((secKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes()));
    }

    String getSeKey();

    byte[] getMask();

    default int getWsVersion()
    {
        return 13;
    }

    String getSecAcceptExpect();

    void updateHandshakeState(int state);

    boolean checkState(int state);
}
