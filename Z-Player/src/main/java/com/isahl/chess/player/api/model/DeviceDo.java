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

package com.isahl.chess.player.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.pawn.endpoint.device.jpa.remote.postgres.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.api.model.DeviceProfile;
import org.springframework.lang.NonNull;

/**
 * @author william.d.zk
 * 
 * @date 2019-06-15
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeviceDo
{
    private String        mSn;
    private String        mUsername;
    private String        mPassword;
    private String        mToken;
    private DeviceProfile mProfile;

    public String getSn()
    {
        return mSn;
    }

    public void setSn(@NonNull String sn)
    {
        this.mSn = sn.toUpperCase();
    }

    public void setPassword(@NonNull String password)
    {
        this.mPassword = password;
    }

    public String getPassword()
    {
        return mPassword;
    }

    public void setToken(@NonNull String token)
    {
        this.mToken = token.toUpperCase();
    }

    @JsonIgnore
    public String getToken()
    {
        return mToken;
    }

    public String getClientId()
    {
        return getToken();
    }

    public String getUsername()
    {
        return mUsername;
    }

    public void setUsername(String username)
    {
        this.mUsername = username;
    }

    public DeviceProfile getProfile()
    {
        return mProfile;
    }

    public void setProfile(DeviceProfile profile)
    {
        this.mProfile = profile;
    }

    public static DeviceDo of(DeviceEntity entity)
    {
        DeviceDo deviceDo = new DeviceDo();
        deviceDo.setUsername(entity.getUsername());
        deviceDo.setPassword(entity.getPassword());
        deviceDo.setSn(entity.getSn());
        deviceDo.setToken(entity.getToken());
        deviceDo.setProfile(entity.getProfile());
        return deviceDo;
    }
}
