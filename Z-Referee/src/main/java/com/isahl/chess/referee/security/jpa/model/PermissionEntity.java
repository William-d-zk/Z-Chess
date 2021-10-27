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
import com.isahl.chess.rook.storage.db.model.AuditModel;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * @author william.d.zk
 * @date 2021/3/5
 */
@Entity(name = "permission")
@Table(schema = "z_chess_security",
       indexes = {
               @Index(name = "name_idx",
                      columnList = "name"),
               @Index(name = "url_idx",
                      columnList = "url")
       })
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PermissionEntity
        extends AuditModel
        implements Serializable
{
    @Id
    @GeneratedValue(generator = "permission_seq")
    @SequenceGenerator(name = "permission_seq",
                       schema = "z_chess_security",
                       sequenceName = "permission_sequence")
    private long             id;
    @Column(nullable = false,
            unique = true)
    private String           name;
    @Column(nullable = false,
            unique = true)
    private String           url;
    @Column(nullable = false)
    private String           description;
    private int              priority;
    @ManyToMany(mappedBy = "permissions")
    private List<RoleEntity> roles;

    public void setId(long id)
    {
        this.id = id;
    }

    public long getId()
    {
        return id;
    }

    public static int SERIAL_PERMISSION = RoleEntity.SERIAL_ROLE + 1;

    @Override
    public int serial()
    {
        return SERIAL_PERMISSION;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    public List<RoleEntity> getRoles()
    {
        return roles;
    }

    public void setRoles(List<RoleEntity> roles)
    {
        this.roles = roles;
    }
}
