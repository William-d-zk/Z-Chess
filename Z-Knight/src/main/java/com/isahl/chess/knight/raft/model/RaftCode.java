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

package com.isahl.chess.knight.raft.model;

public enum RaftCode
{
    SUCCESS(0, "success"),
    LOWER_TERM(1, "term < current,reject"),
    CONFLICT(2, "pre-log-index&pre-log-term inconsistent,reject"),
    ILLEGAL_STATE(3, "illegal state,reject"),
    SPLIT_CLUSTER(4, "split cluster,reject"),
    ALREADY_VOTE(5, "already vote,reject"),
    OBSOLETE(6, "index obsolete,reject");

    private final int    _Code;
    private final String _Description;

    RaftCode(int code, String des)
    {
        _Code = code;
        _Description = des;
    }

    public int getCode()
    {
        return _Code;
    }

    public String getDescription()
    {
        return _Description;
    }

    public static RaftCode valueOf(int code)
    {
        return switch (code) {
            case 0 -> SUCCESS;
            case 1 -> LOWER_TERM;
            case 2 -> CONFLICT;
            case 3 -> ILLEGAL_STATE;
            case 4 -> SPLIT_CLUSTER;
            case 5 -> ALREADY_VOTE;
            case 6 -> OBSOLETE;
            default -> throw new UnsupportedOperationException();
        };
    }
}
