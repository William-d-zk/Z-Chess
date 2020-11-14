/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.pawn.endpoint.spring.device.jpa.model;

import java.time.LocalDateTime;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.io.core.inf.IQoS;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

/**
 * @author william.d.zk
 */
@Entity(name = "Device")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(indexes = { @Index(name = "device_idx_token_pwd_id", columnList = "token,password,passwordId"),
                   @Index(name = "device_idx_token_pwd", columnList = "token,password"),
                   @Index(name = "device_idx_sn", columnList = "sn"),
                   @Index(name = "device_idx_token", columnList = "token"),
                   @Index(name = "device_idx_username", columnList = "username")
})
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class DeviceEntity
        extends
        AuditModel
        implements
        IStorage
{
    private static final long serialVersionUID = -6645586986057373344L;

    @Id
    @GeneratedValue(generator = "ZDeviceGenerator")
    @GenericGenerator(name = "ZDeviceGenerator",
                      strategy = "com.isahl.chess.pawn.endpoint.spring.device.jpa.generator.ZDeviceGenerator")
    private long            id;
    @Column(length = 32, nullable = false, updatable = false)
    private String          sn;
    @Column(length = 32, nullable = false)
    @Length(min = 17, max = 32, message = "*Your password must have at least 17 characters less than 33 characters")
    @NotBlank(message = "*Please provide your password")
    private String          password;
    @Column(length = 32, nullable = false)
    @Length(min = 8, max = 32, message = "* Your Username must have at least 8 characters less than 33 characters")
    @NotBlank(message = "*Please provide your username")
    private String          username;
    private int             passwordId;
    @Column(length = 64, nullable = false, unique = true)
    private String          token;
    @Column(name = "invalid_at", nullable = false)
    private LocalDateTime   invalidAt;
    @Column(name = "wifi_mac", length = 32)
    private String          wifiMac;
    @Column(name = "sensor_mac", length = 32)
    private String          sensorMac;
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private DeviceSubscribe subscribe;

    @Transient
    private Operation mOperation = Operation.OP_NULL;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
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

    public void increasePasswordId()
    {
        passwordId++;
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
        return String.format("device{id:%s,token:%s,user:%s,pwdId:%d,pwd:%s,sn:%s,create:%s,update:%s,invalid:%s,sensor-mac:%s,wifi-mac:%s}",
                             getId(),
                             getToken(),
                             getUsername(),
                             getPasswordId(),
                             getPassword(),
                             getSn(),
                             getCreatedAt(),
                             getUpdatedAt(),
                             getInvalidAt(),
                             getSensorMac(),
                             getWifiMac());
    }

    public int getPasswordId()
    {
        return passwordId;
    }

    public void setPasswordId(int passwordId)
    {
        this.passwordId = passwordId;
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

    public String getSn()
    {
        return sn;
    }

    public void setSn(String sn)
    {
        this.sn = sn;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getWifiMac()
    {
        return wifiMac;
    }

    public void setWifiMac(String wifiMac)
    {
        this.wifiMac = wifiMac;
    }

    public String getSensorMac()
    {
        return sensorMac;
    }

    public void setSensorMac(String sensorMac)
    {
        this.sensorMac = sensorMac;
    }

    public DeviceSubscribe getSubscribe()
    {
        return subscribe;
    }

    @JsonIgnore
    public Map<String,
               IQoS.Level> getSubscribes()
    {
        return subscribe == null ? null
                                 : subscribe.getSubscribes();
    }

    @JsonIgnore
    public void addSubscribes(String topic, IQoS.Level level)
    {
        if (subscribe != null) {
            subscribe.addSubscribes(topic, level);
        }
    }

    public void unsubscribe(String topic)
    {
        if (subscribe != null) {
            subscribe.unsubscribe(topic);
        }
    }

    public void setSubscribe(DeviceSubscribe subscribe)
    {
        this.subscribe = subscribe;
    }

    @Override
    public long primaryKey()
    {
        return id;
    }

    @Override
    public Operation operation()
    {
        return mOperation;
    }

    public void setOperation(Operation operation)
    {
        mOperation = operation;
    }

    @Override
    public Strategy strategy()
    {
        return Strategy.RETAIN;
    }

    @Override
    public int serial()
    {
        return DEVICE_ENTITY_SERIAL;
    }

    private final static int DEVICE_ENTITY_SERIAL = AUDIT_MODEL_SERIAL + 1;
}
