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
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.model.ListSerial;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.rook.storage.db.model.AuditModel;

import javax.persistence.*;
import java.io.Serial;
import java.util.List;

/**
 * @author william.d.zk
 * @date 2021/3/5
 */
@Entity(name = "permission")
@Table(schema = "z_chess_security",
       indexes = { @Index(name = "name_idx",
                          columnList = "name"),
                   @Index(name = "url_idx",
                          columnList = "url") })
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = IProtocol.STORAGE_ROOK_DB_SERIAL)
public class PermissionEntity
        extends AuditModel
{
    @Serial
    private static final long serialVersionUID = -4078887725794775246L;

    @Transient
    private long                   mId;
    @Transient
    private String                 mName;
    @Transient
    private String                 mUrl;
    @Transient
    private String                 mDescription;
    @Transient
    private int                    mPriority;
    @Transient
    private ListSerial<RoleEntity> mRoles;

    public void setId(long id)
    {
        mId = id;
    }

    @Id
    @GeneratedValue(generator = "permission_seq")
    @SequenceGenerator(name = "permission_seq",
                       schema = "z_chess_security",
                       sequenceName = "permission_sequence")
    public long getId()
    {
        return mId;
    }

    @Column(nullable = false,
            unique = true)
    public String getName()
    {
        return mName;
    }

    public void setName(String name)
    {
        mName = name;
    }

    @Column(nullable = false,
            unique = true)
    public String getUrl()
    {
        return mUrl;
    }

    public void setUrl(String url)
    {
        this.mUrl = url;
    }

    @Column(nullable = false)
    public String getDescription()
    {
        return mDescription;
    }

    public void setDescription(String description)
    {
        this.mDescription = description;
    }

    @Column
    public int getPriority()
    {
        return mPriority;
    }

    public void setPriority(int priority)
    {
        mPriority = priority;
    }

    @ManyToMany(mappedBy = "permissions")
    public List<RoleEntity> getRoles()
    {
        return mRoles;
    }

    public void setRoles(List<RoleEntity> roles)
    {
        mRoles = roles == null ? new ListSerial<>(RoleEntity::new) : new ListSerial<>(roles, RoleEntity::new);
    }

    public PermissionEntity()
    {
        super();
    }

    public PermissionEntity(ByteBuf input)
    {
        super(input);
    }
}
