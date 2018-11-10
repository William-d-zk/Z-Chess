/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.spring.auth.model;

import com.tgx.chess.spring.jpa.model.AuditModel;
import org.hibernate.validator.constraints.Length;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.util.Set;

@Entity(name = "Account")
@Table(indexes = { @Index(name = "account_idx_email", columnList = "email"),
                   @Index(name = "account_idx_name", columnList = "name") })
public class AccountEntity
        extends
        AuditModel
{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int             id;

    @Email(message = "{valid.email}")
    @NotEmpty(message = "{field.not.empty}")
    @Length(max = 64, message = "{size.email.over_length}")
    @Column(length = 64)
    private String          email;

    @Length(min = 8, max = 32, message = "{size.account.form.password}")
    @NotEmpty(message = "{field.not.empty}")
    @Column(length = 32)
    private String          password;

    @NotEmpty(message = "{field.not.empty}")
    @Length(min = 3, max = 32, message = "{size.account.form.name}")
    @Column(length = 32)
    private String          name;

    @Column
    private int             active;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "account_role", joinColumns = @JoinColumn(name = "account_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "account")
    private ProfileEntity   profile;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setActive(int active)
    {
        this.active = active;
    }

    public int getActive()
    {
        return active;
    }

    public Set<RoleEntity> getRoles()
    {
        return roles;
    }

    public void setRoles(Set<RoleEntity> roles)
    {
        this.roles = roles;
    }

    public ProfileEntity getProfile()
    {
        return profile;
    }

    public void setProfile(ProfileEntity profile)
    {
        this.profile = profile;
    }

}