/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.open.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tgx.chess.pawn.endpoint.spring.device.jpa.model.MessageBody;
import com.tgx.chess.queen.db.inf.IStorage;

/**
 * @author william.d.zk
 * @date 2020/5/11
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class MessageDo
{
    @JsonIgnore
    private IStorage.Operation mOperation;

    private long        id;
    private String      mOwner;
    private long        origin;
    private long        destination;
    private long        msgId;
    private String      direction;
    private String      topic;
    private MessageBody content;

    public IStorage.Operation operation()
    {
        return mOperation;
    }

    @JsonIgnore
    public void setOperation(IStorage.Operation operation)
    {
        mOperation = operation;
    }

    public long getId()
    {
        return id;
    }

    public String getOwner()
    {
        return mOwner;
    }

    public void setOwner(String owner)
    {
        mOwner = owner;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public long getOrigin()
    {
        return origin;
    }

    public void setOrigin(long origin)
    {
        this.origin = origin;
    }

    public long getDestination()
    {
        return destination;
    }

    public void setDestination(long destination)
    {
        this.destination = destination;
    }

    public long getMsgId()
    {
        return msgId;
    }

    public void setMsgId(long msgId)
    {
        this.msgId = msgId;
    }

    public String getDirection()
    {
        return direction;
    }

    public void setDirection(String direction)
    {
        this.direction = direction;
    }

    public String getTopic()
    {
        return topic;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }

    public MessageBody getContent()
    {
        return content;
    }

    public void setContent(MessageBody content)
    {
        this.content = content;
    }
}
