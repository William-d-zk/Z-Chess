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

package com.isahl.chess.knight.raft.model;

/**
 * @author william.d.zk
 * @date 2019/12/9
 */
public enum RaftState
{
    CLIENT((byte) 0),
    FOLLOWER((byte) 1),
    ELECTOR((byte) 2),
    CANDIDATE((byte) 3),
    LEADER((byte) 4),
    JOINT((byte) 8),
    GATE((byte) 16),
    OUTSIDE((byte) 128);

    private final byte _Code;

    RaftState(byte code)
    {
        _Code = code;
    }

    public byte getCode()
    {
        return _Code;
    }

    public static RaftState valueOf(int code)
    {
        return switch(code) {
            case 0 -> CLIENT;
            case 1 -> FOLLOWER;
            case 2 -> ELECTOR;
            case 3 -> CANDIDATE;
            case 4 -> LEADER;
            case 8 -> JOINT;
            case 16 -> GATE;
            case 128 -> OUTSIDE;
            default -> throw new IllegalArgumentException();
        };
    }
}
