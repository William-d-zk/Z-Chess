/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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

package com.tgx.chess.spring.device.model;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.tgx.chess.spring.auth.model.AccountEntity;
import com.tgx.chess.spring.jpa.model.AuditModel;

/**
 * @author william.d.zk
 */
@Entity(name = "Client")
@Table(schema = "\"tgx-z-chess-device\"")
public class ClientEntity
        extends
        AuditModel
{

    private static final long serialVersionUID = 770148829699352836L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long   id;
    @Column(length = 32)
    private String userName;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(schema = "\"tgx-z-chess-device\"",
               name = "client_device",
               joinColumns = @JoinColumn(name = "client_id"),
               inverseJoinColumns = @JoinColumn(name = "device_id"))
    private Set<DeviceEntity> devices;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public Set<DeviceEntity> getDevices()
    {
        return devices;
    }

    public void setDevices(Set<DeviceEntity> devices)
    {
        this.devices = devices;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public AccountEntity getAccount()
    {
        return account;
    }

    public void setAccount(AccountEntity account)
    {
        this.account = account;
    }
}
