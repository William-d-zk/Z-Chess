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

import com.isahl.chess.king.base.features.ICode;

public enum CodeMqtt
        implements ICode
{

    OK(0, 0),
    QOS_LEVEL_1(-1, 1),
    QOS_LEVEL_2(-1, 2),
    DIS_WILL(-1, 4),
    TOPIC_NO_MATCH(-1, 0x10),
    SUBSCRIBE_NO_EXIST(-1, 0x11),
    AUTH_CONTINUE(-1, 0x18),
    AUTH_AGAIN(-1, 0x19),
    FAILED_UNKNOWN(-1, 0x80),
    REJECT_MALFORMED_PACKET(-1, 0x81),
    REJECT_FAILED_PROTOCOL(-1, 0x82),
    REJECT_IMPLEMENTS_PROTOCOL(-1, 0x83),
    REJECT_UNSUPPORTED_VERSION_PROTOCOL(1, 0x84),
    REJECT_IDENTIFIER(2, 0x85),
    REJECT_SERVER_UNAVAILABLE(3, 0x88),
    REJECT_BAD_USER_OR_PASSWORD(4, 0x86),
    REJECT_NOT_AUTHORIZED(5, 0x87),
    REJECT_SERVER_BUSY(-1, 0x89),
    REJECT_BANNED(-1, 0x8A),
    DIS_SERVICE_SHUTTING_DOWN(-1, 0x8B),
    REJECT_BAD_AUTHENTICATION_METHOD(-1, 0x8C),
    DIS_KEEP_ALIVE_TIMEOUT(-1, 0x8D),
    DIS_SESSION_TAKEN_OVER(-1, 0x8E),
    TOPIC_FILTER_PATTERN(-1, 0x8F),
    TOPIC_NAME_INVALID(-1, 0x90),
    PACKET_IDENTIFIER_IN_USE(-1, 0x91),
    PACKET_IDENTIFIER_NOT_FOUND(-1, 0x92),
    RECEIVE_MAXIMUM_EXCEEDED(-1, 0x93),
    TOPIC_ALIAS_INVALID(-1, 0x94),
    PACKET_TOO_LARGE(-1, 0x95),
    MESSAGE_RATE_TOO_HIGH(-1, 0x96),
    QUOTA_EXCEEDED(-1, 0x97),
    ADMINISTRATIVE_ACTION(-1, 0x98),
    PAYLOAD_FORMAT_INVALID(-1, 0x99),
    RETAIN_UNSUPPORTED(-1, 0x9A),
    QOS_UNSUPPORTED(-1, 0x9B),
    USE_ANOTHER_SERVER(-1, 0x9C),
    SERVER_MOVED(-1, 0x9D),
    SHARED_SUBSCRIPTION_UNSUPPORTED(-1, 0x9E),
    CONNECTION_RATE_EXCEEDED(-1, 0x9F),
    MAXIMUM_CONNECT_TIME(-1, 0xA0),
    SUBSCRIPTION_IDENTIFIERS_UNSUPPORTED(-1, 0xA1),
    WILDCARD_SUBSCRIPTION_UNSUPPORTED(-1, 0xA2);

    private final int _V3Code;
    private final int _V5Code;

    CodeMqtt(int v3, int v5)
    {
        _V3Code = v3;
        _V5Code = v5;
    }

    @Override
    public int getCode(Object... condition)
    {
        int version = (Integer) condition[0];
        return switch(version) {
            case MqttProtocol.VERSION_V3_1_1 -> _V3Code;
            case MqttProtocol.VERSION_V5_0 -> _V5Code;
            default -> throw new UnsupportedOperationException("Unsupported version");
        };
    }

    @Override
    public String format(Object... args)
    {
        return null;
    }

    @Override
    public String formatter()
    {
        return null;
    }

    public static CodeMqtt valueOf(int code, int version)
    {
        return switch(version) {
            case MqttProtocol.VERSION_V3_1_1 -> switch(code) {
                case 0 -> OK;
                case 1 -> REJECT_UNSUPPORTED_VERSION_PROTOCOL;
                case 2 -> REJECT_IDENTIFIER;
                case 3 -> REJECT_SERVER_UNAVAILABLE;
                case 4 -> REJECT_BAD_USER_OR_PASSWORD;
                case 5 -> REJECT_NOT_AUTHORIZED;
                default -> throw new IllegalArgumentException("6-255 Reserved");
            };
            case MqttProtocol.VERSION_V5_0 -> switch(code) {
                case 0 -> OK;
                case 1 -> QOS_LEVEL_1;
                case 2 -> QOS_LEVEL_2;
                case 4 -> DIS_WILL;
                case 0x10 -> TOPIC_NO_MATCH;
                case 0x11 -> SUBSCRIBE_NO_EXIST;
                case 0x18 -> AUTH_CONTINUE;
                case 0x19 -> AUTH_AGAIN;
                case 0x80 -> FAILED_UNKNOWN;
                case 0x81 -> REJECT_MALFORMED_PACKET;
                case 0x82 -> REJECT_FAILED_PROTOCOL;
                case 0x83 -> REJECT_IMPLEMENTS_PROTOCOL;
                case 0x84 -> REJECT_UNSUPPORTED_VERSION_PROTOCOL;
                case 0x85 -> REJECT_IDENTIFIER;
                case 0x86 -> REJECT_BAD_USER_OR_PASSWORD;
                case 0x87 -> REJECT_NOT_AUTHORIZED;
                case 0x88 -> REJECT_SERVER_UNAVAILABLE;
                case 0x89 -> REJECT_SERVER_BUSY;
                case 0x8A -> REJECT_BANNED;
                case 0x8B -> DIS_SERVICE_SHUTTING_DOWN;
                case 0x8C -> REJECT_BAD_AUTHENTICATION_METHOD;
                case 0x8D -> DIS_KEEP_ALIVE_TIMEOUT;
                case 0x8E -> DIS_SESSION_TAKEN_OVER;
                case 0x8F -> TOPIC_FILTER_PATTERN;
                case 0x90 -> TOPIC_NAME_INVALID;
                case 0x91 -> PACKET_IDENTIFIER_IN_USE;
                case 0x92 -> PACKET_IDENTIFIER_NOT_FOUND;
                case 0x93 -> RECEIVE_MAXIMUM_EXCEEDED;
                case 0x94 -> TOPIC_ALIAS_INVALID;
                case 0x95 -> PACKET_TOO_LARGE;
                case 0x96 -> MESSAGE_RATE_TOO_HIGH;
                case 0x97 -> QUOTA_EXCEEDED;
                case 0x98 -> ADMINISTRATIVE_ACTION;
                case 0x99 -> PAYLOAD_FORMAT_INVALID;
                case 0x9A -> RETAIN_UNSUPPORTED;
                case 0x9B -> QOS_UNSUPPORTED;
                case 0x9C -> USE_ANOTHER_SERVER;
                case 0x9D -> SERVER_MOVED;
                case 0x9E -> SHARED_SUBSCRIPTION_UNSUPPORTED;
                case 0x9F -> CONNECTION_RATE_EXCEEDED;
                case 0xA0 -> MAXIMUM_CONNECT_TIME;
                case 0xA1 -> SUBSCRIPTION_IDENTIFIERS_UNSUPPORTED;
                case 0xA2 -> WILDCARD_SUBSCRIPTION_UNSUPPORTED;
                default -> throw new IllegalArgumentException(String.format("code: %d Reserved", code));
            };
            default -> throw new UnsupportedOperationException("Unsupported version");
        };
    }
}
