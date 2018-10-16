/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.king.base.util.CryptUtil;
import com.tgx.chess.queen.io.external.websokcet.bean.control.X103_Close;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X20_SignUp;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X22_SignIn;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X50_DeviceMsg;
import com.tgx.chess.queen.io.external.websokcet.bean.ztls.X01_EncryptRequest;
import com.tgx.chess.rook.biz.device.client.DeviceClient;

@RestController
public class DeviceRest
{
    private final DeviceClient _Client;
    private final CryptUtil    _CryptUtil = new CryptUtil();

    @Autowired
    DeviceRest(DeviceClient client) {
        _Client = client;
    }

    @GetMapping("/client/start")
    public String start() {
        _Client.connect();
        return "async commit start client request";
    }

    @GetMapping("/client/end")
    public String end() {
        _Client.close();
        return "client end";
    }

    @GetMapping("/client/close")
    public String close() {
        _Client.sendLocal(new X103_Close("client close".getBytes()));
        return "client close";
    }

    @GetMapping("/client/heartbeat")
    public String heartbeat(@RequestParam(name = "msg", defaultValue = "client heartbeat", required = false) String msg) {
        _Client.heartbeat(msg);
        return "heartbeat";
    }

    @GetMapping("/client/x50")
    public String x50(@RequestParam(name = "msg", defaultValue = "test", required = false) String msg) {
        X50_DeviceMsg x50 = new X50_DeviceMsg(System.currentTimeMillis());
        x50.setPayload(msg.getBytes());
        _Client.sendLocal(x50);
        return "x50";
    }

    @GetMapping("/client/x20")
    public String x20(@RequestParam(name = "msg", defaultValue = "password", required = false) String msg) {
        X20_SignUp x20 = new X20_SignUp();
        x20.setMac(new byte[] { (byte) 0xAE,
                                (byte) 0xC3,
                                0x33,
                                0x44,
                                0x56,
                                0x09 });
        x20.setPassword(msg);
        _Client.sendLocal(x20);
        return "x20";
    }

    @GetMapping("/client/x22")
    public String x22(@RequestParam(name = "msg", defaultValue = "password", required = false) String msg) {
        Objects.requireNonNull(_Client.getToken());
        X22_SignIn x22 = new X22_SignIn();
        x22.setToken(_Client.getToken());
        x22.setPassword(msg);
        _Client.sendLocal(x22);
        return "x22";
    }

    @GetMapping("/client/handshake")
    public String handshake() {
        _Client.handshake();
        return "handshake";
    }

    @GetMapping("/client/ztls")
    public String ztls() {
        X01_EncryptRequest x01 = new X01_EncryptRequest();
        _Client.sendLocal(x01);
        return "ztls start";
    }

}