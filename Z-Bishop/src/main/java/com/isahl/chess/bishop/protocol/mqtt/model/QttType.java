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

package com.isahl.chess.bishop.protocol.mqtt.model;

import com.isahl.chess.bishop.io.Direction;

import static com.isahl.chess.bishop.io.Direction.CLIENT_TO_SERVER;
import static com.isahl.chess.bishop.io.Direction.SERVER_TO_CLIENT;

public enum QttType
{

    CONNECT(1, "Client request to connect to Server", CLIENT_TO_SERVER),
    CONNACK(2, "Connect acknowledgment", SERVER_TO_CLIENT),
    PUBLISH(3, "Publish message", CLIENT_TO_SERVER, SERVER_TO_CLIENT),
    PUBACK(4, "Publish acknowledgment", CLIENT_TO_SERVER, SERVER_TO_CLIENT),
    PUBREC(5, "Publish received (assured delivery part 1)", CLIENT_TO_SERVER, SERVER_TO_CLIENT),
    PUBREL(6, "Publish release (assured delivery part 2)", CLIENT_TO_SERVER, SERVER_TO_CLIENT),
    PUBCOMP(7, "Publish complete (assured delivery part 3)", CLIENT_TO_SERVER, SERVER_TO_CLIENT),
    SUBSCRIBE(8, "Client subscribe request", CLIENT_TO_SERVER),
    SUBACK(9, "Subscribe acknowledgment", SERVER_TO_CLIENT),
    UNSUBSCRIBE(10, "Unsubscribe request", CLIENT_TO_SERVER),
    UNSUBACK(11, "Unsubscribe acknowledgment", SERVER_TO_CLIENT),
    PINGREQ(12, "PING request", CLIENT_TO_SERVER),
    PINGRESP(13, "PING response", SERVER_TO_CLIENT),
    DISCONNECT(14, "Client is disconnecting", CLIENT_TO_SERVER),
    AUTH(15, "Client auth method", CLIENT_TO_SERVER);

    final int         _Value;
    final String      _Description;
    final Direction[] _Directions;

    QttType(int code, String description, Direction... directions)
    {
        _Value = code << 4;
        _Directions = directions;
        _Description = description;
    }

    public final int getValue()
    {
        return _Value;
    }

    public Direction[] getDirections()
    {
        return _Directions;
    }

    public String getDescription()
    {
        return _Description;
    }

    public static QttType valueOf(int head)
    {
        return switch(head & 240) {
            case 1 << 4 -> CONNECT;
            case 2 << 4 -> CONNACK;
            case 3 << 4 -> PUBLISH;
            case 4 << 4 -> PUBACK;
            case 5 << 4 -> PUBREC;
            case 6 << 4 -> PUBREL;
            case 7 << 4 -> PUBCOMP;
            case 8 << 4 -> SUBSCRIBE;
            case 9 << 4 -> SUBACK;
            case 10 << 4 -> UNSUBSCRIBE;
            case 11 << 4 -> UNSUBACK;
            case 12 << 4 -> PINGREQ;
            case 13 << 4 -> PINGRESP;
            case 14 << 4 -> DISCONNECT;
            case 15 << 4 -> AUTH;
            default -> throw new IllegalArgumentException();
        };
    }
}
