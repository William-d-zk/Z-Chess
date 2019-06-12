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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tgx.chess.spring.jpa.model.AuditModel;

/**
 * @author william.d.zk
 */
@Entity(name = "Device")
@Table(schema = "\"tgx-z-chess-device\"",
       indexes = { @Index(name = "device_idx_token_pwd", columnList = "token,password"),
                   @Index(name = "device_idx_token", columnList = "token"),
                   @Index(name = "device_idx_mac", columnList = "mac"),
                   @Index(name = "device_idx_imei", columnList = "imei"),
                   @Index(name = "device_idx_imsi", columnList = "imsi"),
                   @Index(name = "device_idx_sn", columnList = "sn"),
                   @Index(name = "device_idx_password_id", columnList = "password_id") })
public class DeviceEntity
        extends
        AuditModel
{
    private static final long serialVersionUID = -6645586986057373344L;

    @Id
    @GeneratedValue(generator = "ZGenerator")
    @GenericGenerator(name = "ZGenerator", strategy = "com.tgx.chess.spring.jpa.generator.ZGenerator")
    private Long id;

    @Column(length = 17)
    private String mac;

    @Column(length = 17)
    private String imei;

    @Column(length = 15)
    private String imsi;

    @Column(length = 64)
    private String sn;

    @Column(length = 32)
    @Length(min = 5, max = 32, message = "*Your password must have at least 5 characters less than 32 characters")
    @NotEmpty(message = "*Please provide your password")
    private String password;

    private long passwordId;

    @Column(length = 64)
    private String token;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "invalid_at", nullable = false)
    @JsonIgnore
    private Date invalidAt;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    @Override
    public String toString()
    {
        return String.format("device{id:%s,pass:%s,mac:%s,imei:%s,imsi:%s,sn:%s,create:%s,update:%s,token:%s,invalid:%s}",
                             getId(),
                             getPassword(),
                             getMac(),
                             getImei(),
                             getImsi(),
                             getSn(),
                             getCreatedAt(),
                             getUpdatedAt(),
                             getToken(),
                             getInvalidAt());
    }

    public long getPasswordId()
    {
        return passwordId;
    }

    public void setPasswordId(long passwordId)
    {
        this.passwordId = passwordId;
    }

    public Date getInvalidAt()
    {
        return invalidAt;
    }

    public void setInvalidAt(Date invalidAt)
    {
        this.invalidAt = invalidAt;
    }

    public String getMac()
    {
        return mac;
    }

    public void setMac(String mac)
    {
        this.mac = mac;
    }

    public String getImei()
    {
        return imei;
    }

    public void setImei(String imei)
    {
        this.imei = imei;
    }

    public String getImsi()
    {
        return imsi;
    }

    public void setImsi(String imsi)
    {
        this.imsi = imsi;
    }

    public String getSn()
    {
        return sn;
    }

    public void setSn(String sn)
    {
        this.sn = sn;
    }
}
