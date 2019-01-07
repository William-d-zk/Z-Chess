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

package com.tgx.chess.queen.config;

import com.tgx.chess.king.config.ConfigKey;

public interface QueenConfigKey
        extends
        ConfigKey
{

    String OWNER_QUEEN_POWER      = "power";
    String KEY_POWER_SERVER       = "server";
    String KEY_POWER_CLUSTER      = "cluster";
    String KEY_POWER_INTERNAL     = "internal";
    String KEY_POWER_CLIENT       = "client";
    String KEY_POWER_LINK         = "link";
    String KEY_POWER_ERROR        = "error";
    String KEY_POWER_LOGIC        = "logic";

    String OWNER_PIPELINE_CORE    = "core";
    String KEY_CORE_SERVER        = "server";
    String KEY_CORE_CLUSTER       = "cluster";
    String KEY_CORE_DECODER       = "decoder";
    String KEY_CORE_LOGIC         = "logic";
    String KEY_CORE_ENCODER       = "encoder";

    String OWNER_SOCKET_OPTION    = "option";
    String KEY_OPTION_SNF         = "snf";
    String KEY_OPTION_RCV         = "rcv";
    String KEY_OPTION_MTU         = "mtu";
    String KEY_OPTION_KEEP_ALIVE  = "keep_alive";
    String KEY_OPTION_TCP_NODELAY = "tcp_nodelay";
    String KEY_OPTION_SO_LINGER   = "so_linger";

    String OWNER_SOCKET_SEND      = "send";
    String KEY_SEND_QUEUE_SIZE    = "queue_size";
    String OWNER_SOCKET_IN        = "in";
    String KEY_IN_MINUTE          = "minute";
    String OWNER_SOCKET_OUT       = "out";
    String KEY_OUT_SECOND         = "second";

    String OWNER_PROTOCOL_CRYPT   = "crypt";
    String KEY_CRYPT_ENABLE       = "enable";
}
