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

package com.isahl.chess.pawn.endpoint.device.resource.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.model.BinarySerial;

import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.isahl.chess.king.base.content.ByteBuf.vSizeOf;

/**
 * @author william.d.zk
 * @date 2019-07-31
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@ISerialGenerator(parent = ISerial.BIZ_PLAYER_API_SERIAL)
public class MessageBody
        extends BinarySerial
{

    @Serial
    private static final long serialVersionUID = -8904730289818144372L;

    @JsonIgnore
    private String mTopic;

    @JsonCreator
    public MessageBody(
            @JsonProperty("topic")
            String topic,
            @JsonProperty("content")
            byte[] content)
    {
        mTopic = Objects.requireNonNull(topic);
        withSub(Objects.requireNonNull(content));
    }

    public MessageBody(ByteBuf input)
    {
        super(input);
    }

    public String getTopic()
    {
        return mTopic;
    }

    public byte[] getContent()
    {
        return payload();
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        int tl = input.vLength();
        mTopic = input.readUTF(tl);
        return remain - vSizeOf(tl);
    }

    @Override
    public int length()
    {
        return super.length() + vSizeOf(mTopic.getBytes(StandardCharsets.UTF_8).length);
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .vPut(mTopic.getBytes(StandardCharsets.UTF_8));
    }
}
