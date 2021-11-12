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

package com.isahl.chess.board.base;

/**
 * @author william.d.zk
 * @date 2021-11-11
 */
public interface ISerial
{
    int IO_QUEEN_PACKET_SERIAL           = 0x007;// 0x08~0x0F(8~15)
    int PROTOCOL_BISHOP_COMMAND_SERIAL   = 0x0FF;// 0x10~0xFE(16~254)
    int PROTOCOL_BISHOP_CONTROL_SERIAL   = 0x100;// 0x101~0x1FE(257~510)
    int PROTOCOL_BISHOP_FRAME_SERIAL     = 0x200;// 0x201~0x2FE(513~766)
    int CORE_KING_INTERNAL_SERIAL        = 0x400;// 0x401~0x4FE(1025~1278)
    int CORE_KING_JSON_SERIAL            = 0x800;// 0xA01~0xAFE(2561~2814)
    int STORAGE_ROOK_DB_SERIAL           = 0xA00;// 0x801~0x8FE(2049~2302)
    int CLUSTER_KNIGHT_CONSISTENT_SERIAL = 0xE00;// 0xE01~0xEFE(3584~3838)
    int BIZ_PLAYER_API_SERIAL            = 0xF00;// 0xF01~0xFFE(3841~4094)`

    default int serial()
    {
        return -1;
    }

    default int _super()
    {
        return -1;
    }

    default int _sub()
    {
        return -1;
    }

    /**
     * @return max in encoding min in decoding
     */
    default int length()
    {
        return 0;
    }

    default byte[] payload()
    {
        return null;
    }

    default void put(byte[] payload)
    {

    }
}
