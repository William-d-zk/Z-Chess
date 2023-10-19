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

package com.isahl.chess.rook.storage.db.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.message.InnerProtocol;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static jakarta.persistence.TemporalType.TIMESTAMP;

/**
 * @author william.d.zk
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(value = { "created_at",
                                "updated_at",
                                "created_by_id",
                                "updated_by_id" },
                      allowGetters = true)
public abstract class AuditModel
        extends InnerProtocol
{
    @Column(name = "created_at",
            nullable = false,
            updatable = false)
    @CreatedDate
    @Convert(converter = Jsr310JpaConverters.LocalDateTimeConverter.class)
    @Temporal(TIMESTAMP)
    private LocalDateTime createdAt;

    @Column(name = "updated_at",
            nullable = false)
    @LastModifiedDate
    @Convert(converter = Jsr310JpaConverters.LocalDateTimeConverter.class)
    @Temporal(TIMESTAMP)
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public LocalDateTime getCreatedAt()
    {
        return createdAt;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public void setCreatedAt(LocalDateTime createdAt)
    {
        this.createdAt = createdAt;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public LocalDateTime getUpdatedAt()
    {
        return updatedAt;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public void setUpdatedAt(LocalDateTime updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    @Column(name = "created_by_id",
            nullable = false)
    private Long createdById;
    @Column(name = "updated_by_id",
            nullable = false)
    private Long updatedById;

    public Long getCreatedById()
    {
        return createdById;
    }

    public Long getUpdatedById()
    {
        return updatedById;
    }

    public void setCreatedById(Long createdById)
    {
        this.createdById = createdById;
    }

    public void setUpdatedById(Long updatedById)
    {
        this.updatedById = updatedById;
    }

    public AuditModel()
    {
        super();
    }

    public AuditModel(Operation operation, Strategy strategy)
    {
        super(operation, strategy);
    }

    public AuditModel(ByteBuf input)
    {
        super(input);
    }

    @Override
    public String toString()
    {
        return String.format("  create@ %s update@ %s", getCreatedAt(), getUpdatedAt());
    }

    @Override
    public int length()
    {
        return super.length() + // inner-protocol.length
               8 + // created_at
               8 + // updated_at
               8 + // created_by_id
               8; // updated_by_id
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        setCreatedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(input.getLong()), ZoneId.systemDefault()));
        setUpdatedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(input.getLong()), ZoneId.systemDefault()));
        setCreatedById(input.getLong());
        setUpdatedById(input.getLong());
        return remain - 32;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(createdAt.toInstant(ZoneOffset.of(ZoneId.systemDefault()
                                                                     .getId()))
                                      .toEpochMilli())
                    .putLong(updatedAt.toInstant(ZoneOffset.of(ZoneId.systemDefault()
                                                                     .getId()))
                                      .toEpochMilli())
                    .putLong(createdById)
                    .putLong(updatedById);
    }

}