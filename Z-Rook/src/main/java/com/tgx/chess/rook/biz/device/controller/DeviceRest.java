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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.queen.io.external.websokcet.bean.device.X50_DeviceMsg;
import com.tgx.chess.rook.biz.device.client.DeviceClient;

@RestController
public class DeviceRest
{
    @Autowired
    private DeviceClient _Client;

    @GetMapping("/client/start")
    public String start() {
        _Client.connect();
        return "async commit start client request";
    }

    @GetMapping("/client/end")
    public String end() {
        _Client.close();
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

    @GetMapping("/client/handshake")
    public String handshake() {
        _Client.handshake();
        return "handshake";
    }
}
