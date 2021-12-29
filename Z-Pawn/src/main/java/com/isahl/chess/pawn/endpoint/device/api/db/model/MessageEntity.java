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

package com.isahl.chess.pawn.endpoint.device.api.db.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.queen.db.model.IStorage;
import com.isahl.chess.queen.io.core.features.model.routes.ITraceable;
import com.isahl.chess.rook.storage.db.model.AuditModel;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serial;

/**
 * @author william.d.zk
 * @date 2019-07-22
 */
@Entity(name = "message")
@Table(schema = "z_chess_pawn",
       indexes = { @Index(name = "origin_idx",
                          columnList = "origin"),
                   @Index(name = "topic_idx",
                          columnList = "topic")
       })
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = ISerial.STORAGE_ROOK_DB_SERIAL)
public class MessageEntity
        extends AuditModel
        implements ITraceable
{
    @Serial
    private static final long serialVersionUID = -6502547239976531057L;

    @Id
    @JsonIgnore
    @GeneratedValue(generator = "ZMessageGenerator")
    @GenericGenerator(name = "ZMessageGenerator",
                      strategy = "com.isahl.chess.pawn.endpoint.device.db.generator.ZMessageGenerator")
    private long   id;
    @Column(updatable = false,
            nullable = false)
    private long   origin;
    @Column(length = 511,
            nullable = false,
            updatable = false)
    private String topic;
    @Lob
    @Column(name = "content")
    @Type(type = "org.hibernate.type.BinaryType")
    private byte[] content;

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

    public long getOrigin()
    {
        return origin;
    }

    public void setOrigin(long origin)
    {
        this.origin = origin;
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

    public void setContent(byte[] data)
    {
        content = data;
    }

    public byte[] getContent()
    {
        return content;
    }

}
