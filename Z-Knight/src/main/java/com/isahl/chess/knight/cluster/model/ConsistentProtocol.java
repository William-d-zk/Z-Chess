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

package com.isahl.chess.knight.cluster.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.model.TextSerial;

/**
 * @author william.d.zk
 * @date 2020/4/25
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = ISerial.CORE_KING_JSON_SERIAL)
public class ConsistentProtocol
        extends TextSerial
{

    private final long _ClientId;

    @JsonCreator
    public ConsistentProtocol(
            @JsonProperty("text")
                    String text,
            @JsonProperty("client_id")
                    long id)
    {
        _ClientId = id;
        setText(text);
    }

    public long getClientId()
    {
        return _ClientId;
    }

    public int length()
    {
        return 8 + super.length();
    }
}
