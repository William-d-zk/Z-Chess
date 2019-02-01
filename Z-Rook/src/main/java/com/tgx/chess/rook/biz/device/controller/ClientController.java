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

package com.tgx.chess.rook.biz.device.controller;

import java.util.Objects;

import com.tgx.chess.bishop.io.control.X103_Close;
import com.tgx.chess.bishop.io.device.X20_SignUp;
import com.tgx.chess.bishop.io.device.X22_SignIn;
import com.tgx.chess.bishop.io.device.X50_DeviceMsg;
import com.tgx.chess.bishop.io.ztls.X01_EncryptRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.king.base.util.CryptUtil;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.rook.biz.device.client.DeviceClient;

@RestController

public class ClientController
{
    private final DeviceClient _Client;
    private final CryptUtil    _CryptUtil = new CryptUtil();

    @Autowired
    ClientController(DeviceClient client)
    {
        _Client = client;
    }

    @GetMapping("/client/start")
    public String start()
    {
        _Client.connect();
        return "async commit start client request";
    }

    @GetMapping("/client/end")
    public String end()
    {
        _Client.close();
        return "client end";
    }

    @GetMapping("/client/close")
    public String close()
    {
        _Client.sendLocal(new X103_Close("client close".getBytes()));
        return "client close";
    }

    @GetMapping("/client/heartbeat")
    public String heartbeat(@RequestParam(name = "msg", defaultValue = "client heartbeat", required = false) String msg)
    {
        _Client.heartbeat(msg);
        return "heartbeat";
    }

    @GetMapping("/client/x50")
    public String x50(@RequestParam(name = "msg", defaultValue = "test", required = false) String msg)
    {
        X50_DeviceMsg x50 = new X50_DeviceMsg(System.currentTimeMillis());
        x50.setPayload(msg.getBytes());
        _Client.sendLocal(x50);
        return "x50";
    }

    @GetMapping("/client/sign-up")
    public String x20(@RequestParam(name = "password", defaultValue = "password", required = false) String password,
                      @RequestParam(name = "mac", defaultValue = "AE:C3:33:44:56:09") String mac)
    {

        X20_SignUp x20 = new X20_SignUp();
        x20.setMac(IoUtil.writeMacRaw(mac));
        x20.setPassword(password);
        _Client.sendLocal(x20);
        return String.format("send x20 to sign up, mac{ %s }password{ %s }", mac, password);
    }

    @GetMapping("/client/sign-in")
    public String x22(@RequestParam(name = "password", defaultValue = "password", required = false) String password, @RequestParam(name = "token") String token)
    {
        X22_SignIn x22 = new X22_SignIn();
        if (Objects.nonNull(_Client.getToken())
            && !IoUtil.bin2Hex(_Client.getToken())
                      .equals(token)) throw new IllegalStateException(String.format("client already login with %s ", IoUtil.bin2Hex(_Client.getToken())));
        _Client.setToken(token);
        x22.setToken(_Client.getToken());
        x22.setPassword(password);
        _Client.sendLocal(x22);
        return String.format("login %s : %s", IoUtil.bin2Hex(_Client.getToken()), password);
    }

    @GetMapping("/client/handshake")
    public String handshake()
    {
        _Client.handshake();
        return "handshake";
    }

    @GetMapping("/client/ztls")
    public String ztls()
    {
        X01_EncryptRequest x01 = new X01_EncryptRequest();
        _Client.sendLocal(x01);
        return "ztls start";
    }

}
