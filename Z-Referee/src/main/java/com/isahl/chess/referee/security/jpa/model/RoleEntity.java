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
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.*;
import java.io.Serial;
import java.util.List;

/**
 * @author william.d.zk
 * @date 2021/3/5
 */
@Entity(name = "role")
@Table(schema = "z_chess_security",
       indexes = { @Index(name = "role_name_idx",
                          columnList = "name") })
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = IProtocol.STORAGE_ROOK_DB_SERIAL)
public class RoleEntity
        extends AuditModel
        implements GrantedAuthority
{

    @Serial
    private static final long serialVersionUID = -8748613422660526254L;

    @Transient
    private long                         mId;
    @Transient
    private String                       mName;
    @Transient
    private ListSerial<PermissionEntity> mPermissions;
    @Transient
    private ListSerial<UserEntity>       mUsers;

    public void setId(long id)
    {
        mId = id;
    }

    @Id
    @GeneratedValue(generator = "role_seq")
    @SequenceGenerator(name = "role_seq",
                       schema = "z_chess_security",
                       sequenceName = "role_sequence")
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

    @Override
    @Transient
    public String getAuthority()
    {
        return mName;
    }

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_permission",
               schema = "z_chess_security",
               joinColumns = @JoinColumn(name = "role_id"),
               inverseJoinColumns = @JoinColumn(name = "permission_id"))
    public List<PermissionEntity> getPermissions()
    {
        return mPermissions;
    }

    public void setPermissions(List<PermissionEntity> permissions)
    {
        mPermissions = permissions == null ? new ListSerial<>(PermissionEntity::new)
                                           : new ListSerial<>(permissions, PermissionEntity::new);
    }

    @ManyToMany(mappedBy = "authorities")
    public List<UserEntity> getUsers()
    {
        return mUsers;
    }

    public void setUsers(List<UserEntity> users)
    {
        mUsers = users == null ? new ListSerial<>(UserEntity::new) : new ListSerial<>(users, UserEntity::new);
    }

    public RoleEntity()
    {
        super();
    }

    public RoleEntity(ByteBuf input)
    {
        super(input);
    }
}
