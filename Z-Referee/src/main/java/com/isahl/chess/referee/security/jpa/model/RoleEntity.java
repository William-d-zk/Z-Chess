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

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.rook.storage.jpa.model.AuditModel;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Entity(name = "role")
@Table(indexes = {@Index(name = "role_name_idx", columnList = "name")})
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoleEntity
        extends
        AuditModel
        implements
        GrantedAuthority,
        Serializable
{

    @Serial
    private static final long      serialVersionUID = -8748613422660526254L;
    @Id
    @GeneratedValue
    private long                   id;
    @Column(nullable = false, unique = true)
    private String                 name;
    @ManyToMany(fetch = FetchType.EAGER)
    private List<PermissionEntity> permissions;

    public void setId(long id)
    {
        this.id = id;
    }

    public long getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public static int SERIAL_ROLE = UserEntity.SERIAL_USER + 1;

    @Override
    public int serial()
    {
        return SERIAL_ROLE;
    }

    @Override
    public String getAuthority()
    {
        return name;
    }

    public List<PermissionEntity> getPermissions()
    {
        return permissions;
    }

    public void setPermissions(List<PermissionEntity> permissions)
    {
        this.permissions = permissions;
    }
}
