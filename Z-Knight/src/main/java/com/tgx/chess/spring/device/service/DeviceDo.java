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

package com.tgx.chess.spring.device.service;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author william.d.zk
 * @date 2019-06-15
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DeviceDo
{

    private String imei;
    private String mac;
    private String sn;
    private String user;
    private String password;
    private String token;
    private String phone;
    private String imsi;

    public String getImei()
    {
        return imei;
    }

    public void setImei(@NonNull String imei)
    {
        this.imei = imei.toUpperCase();
    }

    public String getMac()
    {
        return mac;
    }

    public void setMac(@NonNull String mac)
    {
        this.mac = mac.toUpperCase();
    }

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

    public String getPhone()
    {
        return phone;
    }

    public String getClientId()
    {
        return getToken();
    }

    public void setPhone(@NonNull String phone)
    {
        this.phone = phone;
    }

    public String getImsi()
    {
        return imsi;
    }

    public void setImsi(@NonNull String imsi)
    {
        this.imsi = imsi.toUpperCase();
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }
}
