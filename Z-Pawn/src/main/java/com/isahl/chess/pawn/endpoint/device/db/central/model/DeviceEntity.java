/*
 * MIT License
 *
 * Copyright (c) 2016~2022. Z-Chess
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

package com.isahl.chess.pawn.endpoint.device.db.central.model;

import static jakarta.persistence.TemporalType.TIMESTAMP;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.pawn.endpoint.device.resource.model.DeviceProfile;
import com.isahl.chess.rook.storage.db.model.AuditModel;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import java.time.LocalDateTime;
import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.Length;

/**
 * @author william.d.zk
 */
@Entity(name = "zc_id_devi-chess")
@Table(indexes = {
        @Index(name = "idx_z_chess_token_pwd_id",
            columnList = "token,password,password_id"),
        @Index(name = "idx_z_chess_token_pwd",
            columnList = "token,password"),
        @Index(name = "idx_z_chess_token",
            columnList = "token"),
        @Index(name = "idx_z_chess_username",
            columnList = "username"),
        @Index(name = "idx_z_chess_code",
            columnList = "code")
    })
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = ISerial.STORAGE_ROOK_DB_SERIAL)
public class DeviceEntity
        extends AuditModel
{
    @Serial
    private static final long serialVersionUID = -6645586986057373344L;

    @Transient
    private String        mCode;
    @Transient
    private String        mPassword;
    @Transient
    private String        mUsername;
    @Transient
    private long          mPasswordId;
    @Transient
    private String        mToken;
    @Transient
    private LocalDateTime mInvalidAt;
    @Transient
    private DeviceProfile mProfile;
    @Transient
    private String        mNotice;
    @Transient
    private String        mVNotice;
    @Transient
    private long          mId;
    @Transient
    private Long          dkScene;
    @Transient
    private Long          dkFactor;
    @Transient
    private Long          dkFunction;

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId()
    {
        return mId;
    }

    public void setId(long id)
    {
        mId = id;
    }


    @Column(nullable = false, name = "device_id")
    public long getDeviceId()
    {
        return pKey;
    }

    public void setDeviceId(long deviceId)
    {
        pKey = deviceId;
    }

    @Column(nullable = false)
    @Length(min = 17,
            max = 32,
            message = "*Your password must have at least 17 characters less than 33 characters")
    @NotBlank(message = "*Please provide your password")
    public String getPassword()
    {
        return mPassword;
    }

    public void setPassword(String password)
    {
        mPassword = password;
    }

    public void increasePasswordId()
    {
        mPasswordId++;
    }

    @Column(nullable = false,
            unique = true)
    public String getToken()
    {
        return mToken;
    }

    public void setToken(String token)
    {
        mToken = token;
    }

    @JsonIgnore
    @Column(name = "password_id")
    public long getPasswordId()
    {
        return mPasswordId;
    }

    public void setPasswordId(long passwordId)
    {
        mPasswordId = passwordId;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @Column(name = "invalid_at",
            nullable = false)
    @Temporal(TIMESTAMP)
    public LocalDateTime getInvalidAt()
    {
        return mInvalidAt;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public void setInvalidAt(LocalDateTime invalidAt)
    {
        mInvalidAt = invalidAt;
    }

    @Column(nullable = false,
            updatable = false)
    public String getCode()
    {
        return mCode;
    }

    public void setCode(String code)
    {
        mCode = code;
    }

    @Column(nullable = false)
    @Length(min = 8,
            max = 32,
            message = "* Your Username must have at least 8 characters less than 33 characters")
    @NotBlank(message = "*Please provide your username")
    public String getUsername()
    {
        return mUsername;
    }

    public void setUsername(String username)
    {
        mUsername = username;
    }

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    public DeviceProfile getProfile()
    {
        return mProfile;
    }

    public void setProfile(DeviceProfile profile)
    {
        mProfile = profile;
    }

    @Column(nullable = false)
    public String getNotice()
    {
        return mNotice;
    }

    public void setNotice(String notice)
    {
        mNotice = notice;
    }

    @Column(nullable = false, name = "v_notice")
    public String getVNotice()
    {
        return mVNotice;
    }

    public void setVNotice(String notice)
    {
        mNotice = mVNotice = notice;
    }

    @Column(name = "dk_scene")
    public long getDkScene() {return dkScene;}

    public void setDkScene(long dkScene) {this.dkScene = dkScene;}

    @Column(name = "dk_factor")
    public long getDkFactor() {return dkFactor;}

    public void setDkFactor(long dkFactor) {this.dkFactor = dkFactor;}

    @Column(name = "dk_function")
    public long getDkFunction() {return dkFunction;}
    
    public void setDkFunction(long dkFunction) {this.dkFunction = dkFunction;}

    @PrePersist
    public void dataCoord(){
        if(dkScene == null) dkScene = 12329L; // 终端交互
        if(dkFactor == null) dkFactor = 8262L; // 设备:信息终端
        if(dkFunction == null) dkFunction = 9278L; // 标识:身份标识
    }

    @Override
    public String toString()
    {
        return String.format(
                "device{id:%s,device:%s,token:%s,user:%s,pwdId:%d,pwd:%s,No.:%s,profile:%s,createdAt:%s,updatedAt:%s,invalidAt:%s}",
                getId(),
                getDeviceId(),
                getToken(),
                getUsername(),
                getPasswordId(),
                getPassword(),
                getNotice(),
                getProfile(),
                getCreatedAt(),
                getUpdatedAt(),
                getInvalidAt());
    }

    public DeviceEntity()
    {
        super();
    }

    public DeviceEntity(ByteBuf input)
    {
        super(input);
    }
}
