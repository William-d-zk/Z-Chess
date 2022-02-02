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

package com.isahl.chess.referee.security.jpa.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.model.ListSerial;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.rook.storage.db.model.AuditModel;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

import static com.isahl.chess.referee.security.jpa.model.Status.COMMON;
import static com.isahl.chess.referee.security.jpa.model.Status.DISABLED;

/**
 * @author william.d.zk
 * @date 2021/3/5
 */
@Entity(name = "user")
@Table(schema = "z_chess_security",
       indexes = { @Index(name = "username_idx",
                          columnList = "username") })
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = IProtocol.STORAGE_ROOK_DB_SERIAL)
public class UserEntity
        extends AuditModel
        implements UserDetails
{
    @Serial
    private static final long serialVersionUID = -9149289160528408957L;

    @Transient
    private String                 mUsername;
    @Transient
    private String                 mPassword;
    @Column(name = "invalid_at",
            nullable = false)
    @Convert(converter = Jsr310JpaConverters.LocalDateTimeConverter.class)
    private LocalDateTime          invalidAt;
    @Transient
    private Status                 mStatus = COMMON;
    @Transient
    private ListSerial<RoleEntity> mAuthorities;

    public void setId(long id)
    {
        pKey = id;
    }

    @Id
    @GeneratedValue(generator = "user_seq")
    @SequenceGenerator(name = "user_seq",
                       schema = "z_chess_security",
                       sequenceName = "user_sequence")
    public long getId()
    {
        return primaryKey();
    }

    @Column(nullable = false,
            updatable = false)
    public String getUsername()
    {
        return mUsername;
    }

    @Override
    @Transient
    public boolean isAccountNonExpired()
    {
        return mStatus != Status.INVALID;
    }

    @Override
    @Transient
    public boolean isAccountNonLocked()
    {
        return mStatus != Status.LOCKED;
    }

    @Override
    @Transient
    public boolean isCredentialsNonExpired()
    {
        return LocalDateTime.now()
                            .isBefore(invalidAt);
    }

    @Override
    @Transient
    public boolean isEnabled()
    {
        return mStatus != DISABLED;
    }

    public void setUsername(String username)
    {
        mUsername = username;
    }

    @Column(nullable = false)
    public String getPassword()
    {
        return mPassword;
    }

    public void setPassword(String password)
    {
        mPassword = password;
    }

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
               schema = "z_chess_security",
               joinColumns = @JoinColumn(name = "user_id"),
               inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Override
    public List<RoleEntity> getAuthorities()
    {
        return mAuthorities;
    }

    public void setAuthorities(List<RoleEntity> authorities)
    {
        mAuthorities = authorities == null ? new ListSerial<>(RoleEntity::new)
                                           : new ListSerial<>(authorities, RoleEntity::new);
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public LocalDateTime getInvalidAt()
    {
        return invalidAt;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public void setInvalidAt(LocalDateTime invalidAt)
    {
        this.invalidAt = invalidAt;
    }

    @Column(nullable = false)
    public Status getStatus()
    {
        return mStatus;
    }

    public void setStatus(Status status)
    {
        this.mStatus = status;
    }

    public UserEntity()
    {
        super();
    }

    public UserEntity(ByteBuf input)
    {
        super(input);
    }
}
