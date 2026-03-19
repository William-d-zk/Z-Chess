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

import static jakarta.persistence.TemporalType.TIMESTAMP;

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
import com.isahl.chess.queen.message.InnerProtocol;
import jakarta.persistence.*;
import java.io.Serial;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

/**
 * 日期视图实体，用于消息时间的多对一关联 对应视图: icv_date (基于 zc_id_scal-date 表)
 *
 * <p>视图结构: - id: bigint (主键) - notice: varchar(255) - d_notice: varchar(255) - v_notice:
 * varchar(255) - date: timestamp with time zone (核心日期时间字段) - created_by_id: bigint - updated_by_id:
 * bigint
 *
 * <p>注意: 这是一个数据库视图，系统启动时不会自动创建表
 *
 * @author william.d.zk
 * @since 2024
 */
@Entity(name = "icv_date")
@Immutable
@Subselect("SELECT * FROM isahl.icv_date")
@Table(name = "icv_date", schema = "isahl")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = ISerial.STORAGE_ROOK_DB_SERIAL)
public class DateEntity extends InnerProtocol {

  @Serial private static final long serialVersionUID = -1L;

  @Transient private Long id;

  @Transient private String notice;

  @Transient private String dNotice;

  @Transient private String vNotice;

  @Transient private LocalDateTime date;

  @Transient private Long createdById;

  @Transient private Long updatedById;

  public DateEntity() {
    super();
  }

  public DateEntity(ByteBuf input) {
    super(input);
  }

  @Id
  @JsonIgnore
  @Column(name = "id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Column(name = "notice", length = 255)
  public String getNotice() {
    return notice;
  }

  public void setNotice(String notice) {
    this.notice = notice;
  }

  @Column(name = "d_notice", length = 255)
  public String getDNotice() {
    return dNotice;
  }

  public void setDNotice(String dNotice) {
    this.dNotice = dNotice;
  }

  @Column(name = "v_notice", length = 255)
  public String getVNotice() {
    return vNotice;
  }

  public void setVNotice(String vNotice) {
    this.vNotice = vNotice;
  }

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  @Column(name = "date", nullable = false)
  @Temporal(TIMESTAMP)
  public LocalDateTime getDate() {
    return date;
  }

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  public void setDate(LocalDateTime date) {
    this.date = date;
  }

  /** 兼容性方法，等同于 getDate() */
  @Transient
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  public LocalDateTime getDateTime() {
    return date;
  }

  /** 兼容性方法，等同于 setDate() */
  @Transient
  public void setDateTime(LocalDateTime dateTime) {
    this.date = dateTime;
  }

  @Column(name = "created_by_id")
  public Long getCreatedById() {
    return createdById;
  }

  public void setCreatedById(Long createdById) {
    this.createdById = createdById;
  }

  @Column(name = "updated_by_id")
  public Long getUpdatedById() {
    return updatedById;
  }

  public void setUpdatedById(Long updatedById) {
    this.updatedById = updatedById;
  }

  @Override
  public String toString() {
    return String.format(
        "DateEntity{id=%d, date=%s, notice='%s', dNotice='%s', vNotice='%s'}",
        id, date, notice, dNotice, vNotice);
  }

  @Override
  public int length() {
    return super.length()
        + 8
        + // id
        8
        + // date as epoch milli
        8
        + // createdById
        8
        + // updatedById
        (notice != null ? notice.getBytes().length : 0)
        + (dNotice != null ? dNotice.getBytes().length : 0)
        + (vNotice != null ? vNotice.getBytes().length : 0)
        + 12; // 3个字符串的长度开销
  }

  @Override
  public int prefix(ByteBuf input) {
    int remain = super.prefix(input);
    id = input.getLong();
    date = LocalDateTime.ofInstant(Instant.ofEpochMilli(input.getLong()), ZoneOffset.UTC);
    createdById = input.getLong();
    updatedById = input.getLong();
    remain -= 32;

    int nl = input.vLength();
    notice = input.readUTF(nl);
    remain -= nl;

    nl = input.vLength();
    dNotice = input.readUTF(nl);
    remain -= nl;

    nl = input.vLength();
    vNotice = input.readUTF(nl);
    remain -= nl;

    return remain;
  }

  @Override
  public ByteBuf suffix(ByteBuf output) {
    return super.suffix(output)
        .putLong(id != null ? id : 0L)
        .putLong(date != null ? date.toInstant(ZoneOffset.UTC).toEpochMilli() : 0L)
        .putLong(createdById != null ? createdById : 0L)
        .putLong(updatedById != null ? updatedById : 0L)
        .putUTF(notice != null ? notice : "")
        .putUTF(dNotice != null ? dNotice : "")
        .putUTF(vNotice != null ? vNotice : "");
  }
}
