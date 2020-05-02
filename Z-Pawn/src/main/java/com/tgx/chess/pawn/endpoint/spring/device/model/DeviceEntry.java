/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.tgx.chess.pawn.endpoint.spring.device.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tgx.chess.knight.json.JsonUtil;
import com.tgx.chess.queen.db.inf.IStorage;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class DeviceEntry
        implements
        IStorage
{
    private final static int DEVICE_ENTRY_SERIAL = DB_SERIAL + 1;

    @Override
    public int dataLength()
    {
        return length;
    }

    @Override
    public byte[] encode()
    {
        byte[] payload = JsonUtil.writeValueAsBytes(this);
        Objects.requireNonNull(payload);
        length = payload.length;
        return payload;
    }

    @Override
    public int decode(byte[] data)
    {
        DeviceEntry json = JsonUtil.readValue(data, getClass());
        Objects.requireNonNull(json);
        deviceId = json.getPrimaryKey();
        token = json.getToken();
        invalidTime = json.getInvalidTime();
        sn = json.getSn();
        username = json.getUsername();
        password = json.getPassword();
        passwordId = json.getPasswordId();
        length = data.length;
        return length;
    }

    @Override
    public int serial()
    {
        return DEVICE_ENTRY_SERIAL;
    }

    private long   deviceId;
    private String token;
    private long   invalidTime;
    private String sn;
    private String username;
    private String password;
    private int    passwordId;

    @JsonIgnore
    private int       length;
    @JsonIgnore
    private Operation operation = Operation.OP_NULL;
    @JsonIgnore
    private Strategy  strategy  = Strategy.RETAIN;

    @Override
    public long getPrimaryKey()
    {
        return deviceId;
    }

    @Override
    public void setPrimaryKey(long key)
    {
        deviceId = key;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public long getInvalidTime()
    {
        return invalidTime;
    }

    public void setInvalidTime(long invalidTime)
    {
        this.invalidTime = invalidTime;
    }

    @Override
    @JsonIgnore
    public void setOperation(Operation op)
    {
        operation = op;
    }

    @JsonIgnore
    public Operation getOperation()
    {
        return operation;
    }

    @Override
    @JsonIgnore
    public Strategy getStrategy()
    {
        return strategy;
    }

    @Override
    @JsonIgnore
    public void setStrategy(Strategy strategy)
    {
        Objects.requireNonNull(strategy);
        this.strategy = strategy;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getPassword()
    {
        return password;
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

    public int getPasswordId()
    {
        return passwordId;
    }

    public void setPasswordId(int passwordId)
    {
        this.passwordId = passwordId;
    }

}