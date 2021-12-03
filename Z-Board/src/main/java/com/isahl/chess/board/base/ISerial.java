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

import java.nio.ByteBuffer;

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
    int CLUSTER_KNIGHT_RAFT_SERIAL       = 0xB00;// 0xB01~0xBFE(2817~3055)
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

    default int length()
    {
        return 2;
    }

    default void put(byte[] payload)
    {

    }

    default ByteBuffer payload()
    {
        return null;
    }

    default void decode(ByteBuffer input)
    {
        int length = vLength(input);
        if(input.remaining() > length) {
            input.limit(input.position() + length);
        }
        int serial = input.getShort() & 0xFFFF;
        if(serial != serial()) {
            throw new IllegalArgumentException("serial unequals");
        }
    }

    default void finish(ByteBuffer input)
    {
        input.limit(input.capacity());
    }

    default ByteBuffer encode()
    {
        return vLengthBuffer(length()).putShort((short) serial());
    }

    static int whois(ByteBuffer input)
    {
        int length = vLength(input);
        if(input.remaining() < length) {
            throw new IllegalStateException(String.format("need more data input, serial object data miss %d",
                                                          length - input.remaining()));
        }
        int serial = input.getShort() & 0xFFFF;
        input.clear();
        return serial;
    }

    default int size()
    {
        if(length() <= 0) {
            return 1;
        }
        else if(length() < 128) {
            return length() + 1;
        }
        else if(length() < 16384) {
            return length() + 2;
        }
        else if(length() < 2097152) {
            return length() + 3;
        }
        else if(length() < 268435456) {
            return length() + 4;
        }
        throw new ArrayIndexOutOfBoundsException("malformed length");
    }

    private static int vLength(ByteBuffer buf)
    {
        int length = 0;
        int cur, pos = 0;
        if(buf.hasRemaining()) {
            do {
                cur = buf.get();
                length += (cur & 0x7F) << (pos * 7);
                pos++;
            }
            while((cur & 0x80) != 0 && buf.hasRemaining());
        }
        return length;
    }

    private static ByteBuffer vLengthBuffer(int length)
    {
        if(length <= 0) {
            return ByteBuffer.allocate(1)
                             .put((byte) 0);
        }
        else if(length < 128) {
            return ByteBuffer.allocate(1 + length)
                             .put((byte) length);
        }
        else if(length < 16384) {
            return ByteBuffer.allocate(2 + length)
                             .put((byte) (0x80 | (length & 0x7F)))
                             .put((byte) (length >>> 7));
        }
        else if(length < 2097152) {
            return ByteBuffer.allocate(3 + length)
                             .put((byte) (0x80 | (length & 0x7F)))
                             .put((byte) (0x80 | (length & 0x7F80) >>> 7))
                             .put((byte) (length >>> 14));
        }
        else if(length < 268435456) {
            return ByteBuffer.allocate(4 + length)
                             .put((byte) (0x80 | (length & 0x7F)))
                             .put((byte) (0x80 | (length & 0x7F80) >>> 7))
                             .put((byte) (0x80 | (length & 0x3FC000) >>> 14))
                             .put((byte) (length >>> 21));
        }
        throw new ArrayIndexOutOfBoundsException("malformed length");
    }

}
