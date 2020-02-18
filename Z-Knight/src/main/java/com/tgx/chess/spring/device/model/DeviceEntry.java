/*
 * MIT License
 *
 * Copyright (c) 2016~2020 Z-Chess
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

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgx.chess.queen.db.inf.IStorage;

public class DeviceEntry
        implements
        IStorage
{
    private final static int    DEVICE_ENTRY_SERIAL = DB_SERIAL + 1;
    private static ObjectMapper JsonMapper          = new ObjectMapper();

    @Override
    public int dataLength()
    {
        return length;
    }

    @Override
    public byte[] encode()
    {
        try {
            byte[] payload = JsonMapper.writer()
                                       .writeValueAsBytes(this);
            length = payload.length;
            return payload;
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int decode(byte[] data)
    {
        try {
            JsonNode json = JsonMapper.readTree(data);
            deviceId = json.get("primary_key")
                           .asLong();
            token = json.get("token")
                        .asText();
            invalidTime = json.get("invalid_time")
                              .asLong();
            sn = json.get("sn")
                     .asText();
            username = json.get("username")
                           .asText();
            password = json.get("password")
                           .asText();
            passwordId = json.get("password_id")
                             .asInt();
            length = data.length;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
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

    private boolean   exist;
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

    public boolean isExist()
    {
        return exist;
    }

    public void setExist(boolean exist)
    {
        this.exist = exist;
    }
}