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

package com.isahl.chess.pawn.endpoint.device.jpa.remote.postgres.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.pawn.endpoint.device.model.MessageBody;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.rook.storage.jpa.model.AuditModel;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.io.Serial;

import static com.isahl.chess.pawn.endpoint.device.jpa.PawnConstants.DB_SERIAL_REMOTE_MSG_ENTITY;

/**
 * @author william.d.zk
 * @date 2019-07-22
 */
@Entity(name = "message")
@TypeDef(name = "jsonb",
         typeClass = JsonBinaryType.class)
@Table(indexes = { @Index(name = "origin_idx",
                          columnList = "origin"),
                   @Index(name = "destination_idx",
                          columnList = "destination"),
                   @Index(name = "topic_idx",
                          columnList = "topic")
})
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MessageEntity
        extends AuditModel
        implements IStorage
{
    @Serial
    private static final long serialVersionUID = -6502547239976531057L;

    @Id
    @JsonIgnore
    @GeneratedValue(generator = "ZMessageGenerator")
    @GenericGenerator(name = "ZMessageGenerator",
                      strategy = "com.isahl.chess.pawn.endpoint.device.jpa.generator.ZMessageGenerator")
    private long        id;
    @Column(updatable = false,
            nullable = false)
    private long        origin;
    @Column(updatable = false,
            nullable = false)
    private long        destination;
    @Column(length = 511,
            nullable = false,
            updatable = false)
    private String      topic;
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private MessageBody body;

    @JsonIgnore
    @Transient
    private IStorage.Operation mOperation = IStorage.Operation.OP_NULL;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public MessageBody getBody()
    {
        return body;
    }

    public void setBody(MessageBody body)
    {
        this.body = body;
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

    @Override
    public int serial()
    {
        return DB_SERIAL_REMOTE_MSG_ENTITY;
    }

    @Override
    public long primaryKey()
    {
        return id;
    }

    @Override
    public IStorage.Operation operation()
    {
        return mOperation;
    }

    @JsonIgnore
    public void setOperation(IStorage.Operation operation)
    {
        mOperation = operation;
    }

    @Override
    public IStorage.Strategy strategy()
    {
        return IStorage.Strategy.RETAIN;
    }

    public String getTopic()
    {
        return topic;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }
}
