
/*
 * MIT License
 *
 * Copyright (c) 2016~2022. Z-Chess
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

package com.isahl.chess.king.base.util;

/**
 * @author william.d.zk
 * @date 2022-3-25
 */
public enum ValidType
{
    string("string"),
    number("number"),
    integer("integer"),
    bool("boolean"),
    object("object"),
    array("array"),
    nil("null");

    private final String _JsonType;

    ValidType(String jsonType) {_JsonType = jsonType;}

    public String getJsonType() {return _JsonType;}

    public static ValidType of(String type)
    {
        return switch(type.toLowerCase()) {
            case "string" -> string;
            case "number" -> number;
            case "integer" -> integer;
            case "bool", "boolean" -> bool;
            case "object" -> object;
            case "array" -> array;
            case "nil", "null" -> nil;
            default -> throw new IllegalArgumentException(type);
        };
    }
}
