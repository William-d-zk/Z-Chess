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

package com.isahl.chess.bishop.io;

/**
 * @author william.d.zk
 * @date 2019-07-31
 */
public enum Direction
{

    SERVER_TO_CLIENT("S->C"),
    CLIENT_TO_SERVER("C->S"),
    LEADER_BR_FOLLOWER("L->F"),
    FOLLOWER_RP_LEADER("F->L"),
    PROPOSER_BR_ACCEPTOR("P->A"),
    ELECTOR_RP_CANDIDATE("E->C");

    private final String _Abbreviation;

    Direction(String abbreviation)
    {
        _Abbreviation = abbreviation;
    }

    public String getShort()
    {
        return _Abbreviation;
    }

    public static Direction parseShort(String value)
    {
        return switch(value) {
            case "S->C" -> SERVER_TO_CLIENT;
            case "C->S" -> CLIENT_TO_SERVER;
            case "L->F" -> LEADER_BR_FOLLOWER;
            case "F->L" -> FOLLOWER_RP_LEADER;
            case "P->A" -> PROPOSER_BR_ACCEPTOR;
            case "E->C" -> ELECTOR_RP_CANDIDATE;
            default -> throw new IllegalArgumentException();
        };
    }

    public final static String OWNER_CLIENT = "CLIENT";
    public final static String OWNER_SERVER = "SERVER";
}
