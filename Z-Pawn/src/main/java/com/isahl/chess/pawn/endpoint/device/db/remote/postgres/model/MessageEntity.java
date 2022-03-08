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

package com.isahl.chess.pawn.endpoint.device.db.remote.postgres.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.model.routes.ITraceable;
import com.isahl.chess.rook.storage.db.model.AuditModel;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import static com.isahl.chess.king.base.content.ByteBuf.vSizeOf;
import static com.isahl.chess.queen.db.model.IStorage.Operation.OP_INSERT;
import static java.lang.String.format;

/**
 * @author william.d.zk
 * @date 2019-07-22
 */
@Entity(name = "message")
@Table(schema = "z_chess_pawn",
       indexes = { @Index(name = "origin_idx",
                          columnList = "origin"),
                   @Index(name = "topic_idx",
                          columnList = "topic") })
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = ISerial.STORAGE_ROOK_DB_SERIAL)
public class MessageEntity
        extends AuditModel
        implements ITraceable
{
    @Serial
    private static final long serialVersionUID = -6502547239976531057L;

    @Transient
    private long          mOrigin;
    @Transient
    private String        mTopic;
    @Transient
    private LocalDateTime mNetAt;

    public MessageEntity()
    {
        super(OP_INSERT, Strategy.RETAIN);
    }

    public MessageEntity(ByteBuf input)
    {
        super(input);
    }

    @Id
    @JsonIgnore
    @GeneratedValue(generator = "ZMessageGenerator")
    @GenericGenerator(name = "ZMessageGenerator",
                      strategy = "com.isahl.chess.pawn.endpoint.device.db.generator.ZMessageGenerator")
    public long getId()
    {
        return primaryKey();
    }

    @Override
    public long origin()
    {
        return mOrigin;
    }

    @Column(updatable = false,
            nullable = false)
    public long getOrigin()
    {
        return mOrigin;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @Column(name = "net_at",
            nullable = false,
            updatable = false)
    public LocalDateTime getNetAt()
    {
        return mNetAt;
    }

    @Column(length = 511,
            nullable = false,
            updatable = false)
    public String getTopic()
    {
        return mTopic;
    }

    @Lob
    @Column(name = "content")
    @Type(type = "org.hibernate.type.BinaryType")
    public byte[] getContent()
    {
        return payload();
    }

    public void setId(long id)
    {
        pKey = id;
    }

    public void setOrigin(long origin)
    {
        mOrigin = origin;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public void setNetAt(LocalDateTime netAt)
    {
        mNetAt = netAt;
    }

    public void setTopic(String topic)
    {
        mTopic = topic;
    }

    public void setContent(byte[] data)
    {
        withSub(data);
    }

    @Override
    public String toString()
    {
        return format("MessageEntity{ id=%s, origin=%#x, topic:%s,msg:%s,netAt:%s[%s]}",
                      getId(),
                      getOrigin(),
                      getTopic(),
                      new String(getContent(), StandardCharsets.UTF_8),
                      getNetAt(),
                      super.toString());
    }

    @Override
    public int length()
    {
        return super.length() + // content.length +  id.length
               8 +// origin.length
               8 + // net-at.length
               vSizeOf(mTopic.getBytes(StandardCharsets.UTF_8).length); // topic.length
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mOrigin = input.getLong();
        mNetAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(input.getLong()),
                                         TimeZone.getDefault()
                                                 .toZoneId());
        remain -= 16;
        int tl = input.vLength();
        mTopic = input.readUTF(tl);
        remain -= vSizeOf(tl);
        return remain;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(mOrigin)
                    .putLong(mNetAt.toInstant(ZoneOffset.of(ZoneOffset.systemDefault()
                                                                      .getId()))
                                   .toEpochMilli())
                    .putUTF(mTopic);
    }
}