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

package com.isahl.chess.pawn.endpoint.device.db.central.model;

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
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.pawn.endpoint.device.db.legacy.LegacyBinaryType;
import com.isahl.chess.queen.io.core.features.model.routes.ITraceable;
import com.isahl.chess.rook.storage.db.model.AuditModel;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static com.isahl.chess.king.base.content.ByteBuf.vSizeOf;
import static com.isahl.chess.queen.db.model.IStorage.Operation.OP_INSERT;
import static jakarta.persistence.TemporalType.TIMESTAMP;
import static java.lang.String.format;

/**
 * @author william.d.zk
 * @version 2024-03-18
 * @since 2019-07-22
 */
@Entity(name = "zc_id_msgs-zchat")
@Table(indexes = { @Index(name = "idx_z_chat_fk_origin",
                          columnList = "fk_origin"),
                   @Index(name = "idx_z_chat_topic",
                          columnList = "topic")})
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = ISerial.STORAGE_ROOK_DB_SERIAL)
public class MessageEntity
        extends AuditModel
        implements ITraceable
{
    @Serial
    private static final long serialVersionUID = -6502547239976531057L;

    @Transient
    private long                   mOrigin;
    @Transient
    private String                 mTopic;
    @Transient
    private LocalDateTime          mNetAt;
    @Transient
    private String                 mComments;
    @Transient
    private String                 mNotice;
    @Transient
    private String                 mVNotice;
    @Transient
    private Set<MsgDeliveryStatus> mDeliveryStatus;
    @Transient
    private long                   mId;
    @Transient
    private Long                   dkScene;
    @Transient
    private Long                   dkFactor;
    @Transient
    private Long                   dkFunction;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId()
    {
        return mId;
    }

    @Override
    @JsonIgnore
    public long origin()
    {
        return mOrigin;
    }

    @Column(name = "fk_origin",
            updatable = false,
            nullable = false)
    public long getOrigin()
    {
        return mOrigin;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @Column(name = "v_net_at",
            nullable = false,
            updatable = false)
    @Temporal(TIMESTAMP)
    public LocalDateTime getNetAt()
    {
        return mNetAt;
    }

    @Column(name = "topic",
            length = 511,
            nullable = false,
            updatable = false)
    public String getTopic()
    {
        return mTopic;
    }

    @Lob
    @Column(name = "message")
    @Type(LegacyBinaryType.class)
    public byte[] getMessage()
    {
        return payload();
    }

    public void setId(long id)
    {
        mId = id;
    }

    @Column(nullable = false, name = "message_id")
    public long getMessageId()
    {
        return pKey;
    }

    public void setMessageId(long _Id)
    {
        pKey = _Id;
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

    public void setMessage(byte[] data)
    {
        withSub(data);
        if(data == null) return;
        setComments(new String(data, StandardCharsets.UTF_8));
    }

    @Column(name = "comments",
            columnDefinition = "text")
    public String getComments()
    {
        return mComments;
    }

    public void setComments(String comments)
    {
        mComments = comments;
    }

    @Column(name = "notice")
    public String getNotice() {return mNotice;}

    public void setNotice(String notice) {mNotice = notice;}

    public void genSummary()
    {
        mVNotice = format("%d→@%s size:(%d)", mOrigin, getTopic(), length());
    }

    @Column(name = "v_notice")
    public String getVNotice() {return mVNotice;}

    public void setVNotice(String vNotice) {mVNotice = vNotice;}

    @Column(name = "dk_scene")
    public long getDkScene() {return dkScene;}

    public void setDkScene(long dkScene) {this.dkScene = dkScene;}

    @Column(name = "dk_factor")
    public long getDkFactor() {return dkFactor;}

    public void setDkFactor(long dkFactor) {this.dkFactor = dkFactor;}

    @Column(name = "dk_function")
    public long getDkFunction() {return dkFunction;}
    
    public void setDkFunction(long dkFunction) {this.dkFunction = dkFunction;}
    
    @PrePersist
    public void dataCoord(){
        if(dkScene == null) dkScene = 12317L; // 数据传输
        if(dkFactor == null) dkFactor = 8261L; // 载荷:通讯内容
        if(dkFunction == null) dkFunction = 9321L; // 数据:信息承载
    }

    @Override
    public String toString()
    {
        return format("MessageEntity{ id=%s, message_id=%#x, origin=%#x, topic:%s, msg:%s, netAt:%s[%s], rk_origin=%s}",
                    getId(),
                    primaryKey(),
                    origin(),
                    getTopic(),
                    getComments(),
                    getNetAt(),
                    origin(),
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
        mNetAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(input.getLong()), ZoneOffset.UTC);
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
                    .putLong(mNetAt.toInstant(ZoneOffset.UTC)
                                   .toEpochMilli())
                    .putUTF(mTopic);
    }

    @ManyToMany
    @JoinTable(name = "zc_id_lifecycle_r_primary-status",
               joinColumns = @JoinColumn(name = "ref_left"),
               inverseJoinColumns = @JoinColumn(name = "ref_right"),
               foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT),
               inverseForeignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    public Set<MsgDeliveryStatus> getDeliveryStatusSet() {return mDeliveryStatus;}

    @Transient
    public MsgDeliveryStatus getDeliveryStatus()
    {
        return mDeliveryStatus.stream()
                              .findFirst()
                              .orElseThrow(()->new ZException("DeliveryStatus not found"));
    }

    public void setDeliveryStatusSet(Set<MsgDeliveryStatus> status) {mDeliveryStatus = status;}

    public void setDeliveryStatus(MsgDeliveryStatus status) {
        mDeliveryStatus = Set.of(status);
    }
}
