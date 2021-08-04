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
import com.isahl.chess.rook.storage.jpa.model.AuditModel;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import static com.isahl.chess.referee.security.jpa.model.Status.COMMON;
import static com.isahl.chess.referee.security.jpa.model.Status.DISABLED;

/**
 * @author william.d.zk
 * @date 2021/3/5
 */
@Entity(name = "user")
@Table(indexes = { @Index(name = "username_idx",
                          columnList = "username")
})
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserEntity
        extends AuditModel
        implements UserDetails,
                   Serializable
{
    @Serial
    private static final long serialVersionUID = -9149289160528408957L;

    @Id
    @GeneratedValue
    private long          id;
    @Column(nullable = false,
            updatable = false)
    private String        username;
    @Column(nullable = false)
    private String        password;
    @Column(name = "invalid_at",
            nullable = false)
    private LocalDateTime invalidAt;
    @Column(nullable = false)
    private Status        status = COMMON;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<RoleEntity> authorities;

    public void setId(long id)
    {
        this.id = id;
    }

    public long getId()
    {
        return id;
    }

    public static int SERIAL_USER = AUDIT_MODEL_SERIAL + 1000;

    @Override
    public int serial()
    {
        return SERIAL_USER;
    }

    public String getUsername()
    {
        return username;
    }

    @Override
    public boolean isAccountNonExpired()
    {
        return status != Status.INVALID;
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return status != Status.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired()
    {
        return LocalDateTime.now()
                            .isBefore(invalidAt);
    }

    @Override
    public boolean isEnabled()
    {
        return status != DISABLED;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    @Override
    public List<RoleEntity> getAuthorities()
    {
        return authorities;
    }

    public void setAuthorities(List<RoleEntity> authorities)
    {
        this.authorities = authorities;
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

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }
}
