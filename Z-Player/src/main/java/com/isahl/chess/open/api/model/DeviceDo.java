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

package com.isahl.chess.open.api.model;

import java.time.Instant;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * @author william.d.zk
 * 
 * @date 2019-06-15
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class DeviceDo
{
    private long    id;
    private String  sn;
    private String  username;
    private String  password;
    private String  token;
    private String  wifiMac;
    private String  sensorMac;
    private Instant invalidAt;

    public String getSn()
    {
        return sn;
    }

    public void setSn(@NonNull String sn)
    {
        this.sn = sn.toUpperCase();
    }

    public void setPassword(@NonNull String password)
    {
        this.password = password;
    }

    public String getPassword()
    {
        return password;
    }

    public void setToken(@NonNull String token)
    {
        this.token = token.toUpperCase();
    }

    @JsonIgnore
    public String getToken()
    {
        return token;
    }

    public String getClientId()
    {
        return getToken();
    }

    public Instant getInvalidAt()
    {
        return invalidAt;
    }

    public void setInvalidAt(Instant invalidAt)
    {
        this.invalidAt = invalidAt;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }
}
