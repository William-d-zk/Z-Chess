/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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
package com.tgx.z.queen.io.external.websokcet.bean.control;

import com.tgx.z.queen.io.external.websokcet.WsHandshake;

/**
 * @author William.d.zk
 */
public class X101_HandShake
        extends
        WsHandshake
{
    public final static int COMMAND = 0x101;

    public X101_HandShake(String host, String secKey, int version) {
        super(String.format("GET /ws_service HTTP/1.1\r\nHost: %s\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: %s\r\nOrigin: http://%s\r\nSec-WebSocket-Protocol: z-push, z-chat\r\nSec-WebSocket-Version: %s\r\n",
                            host,
                            secKey,
                            host,
                            version));
    }

    public X101_HandShake(String handshake) {
        super(handshake);
    }

    public X101_HandShake() {
        super(null);
    }

    @Override
    public int getSerial() {
        return COMMAND;
    }

}
