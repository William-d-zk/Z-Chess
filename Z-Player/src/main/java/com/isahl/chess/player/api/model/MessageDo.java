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

package com.isahl.chess.player.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.pawn.endpoint.device.resource.model.MessageBody;
import com.isahl.chess.queen.db.model.IStorage;

import java.nio.charset.StandardCharsets;

/**
 * @author william.d.zk
 * @date 2020/5/11
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MessageDo
{
    @JsonIgnore
    private IStorage.Operation mOperation;

    private long   mOrigin;
    private String mNumber;
    private String mTopic;
    private byte[] mPayload;
    private String mContent;
    private String mProtocol;

    @JsonIgnore
    public IStorage.Operation getOperation()
    {
        return mOperation;
    }

    @JsonIgnore
    public void setOperation(IStorage.Operation operation)
    {
        mOperation = operation;
    }

    public long getOrigin()
    {
        return mOrigin;
    }

    public void setOrigin(long origin)
    {
        mOrigin = origin;
    }

    public String getTopic()
    {
        return mTopic;
    }

    public void setTopic(String topic)
    {
        mTopic = topic;
    }

    public void setPayload(byte[] payload)
    {
        mPayload = payload;
    }

    public byte[] getPayload()
    {
        return mPayload;
    }

    public void setNumber(String number)
    {
        mNumber = number;
    }

    public String getNumber()
    {
        return mNumber;
    }

    public void setContent(String content)
    {
        mContent = content;

    }

    public String getContent()
    {
        return mContent;
    }

    public void setProtocol(String protocol)
    {
        mProtocol = protocol;
    }

    public String getProtocol()
    {
        return mProtocol;
    }

    public void afterEncode()
    {
        switch(mProtocol) {
            case "mqtt", "z-chat" -> {
                if(mContent == null && mPayload != null) {mContent = new String(mPayload, StandardCharsets.UTF_8);}
                else if(!IoUtil.isBlank(mContent) && mPayload == null) {
                    mPayload = mContent.getBytes(StandardCharsets.UTF_8);
                }
            }
            default -> throw new IllegalArgumentException("unsupported protocol:< " + mProtocol + '>');
        }
    }
}
