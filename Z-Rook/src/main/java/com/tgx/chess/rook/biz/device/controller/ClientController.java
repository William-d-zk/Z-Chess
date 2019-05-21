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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.bishop.io.ws.control.X103_Close;
import com.tgx.chess.bishop.io.zprotocol.device.X20_SignUp;
import com.tgx.chess.bishop.io.zprotocol.device.X22_SignIn;
import com.tgx.chess.bishop.io.zprotocol.device.X50_DeviceMsg;
import com.tgx.chess.bishop.io.zprotocol.ztls.X01_EncryptRequest;
import com.tgx.chess.king.base.util.CryptUtil;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.rook.biz.device.client.QttZClient;
import com.tgx.chess.rook.biz.device.client.WsZClient;

/**
 * @author william.d.zk
 */
@RestController
public class ClientController
{
    private final WsZClient  _WsClient;
    private final QttZClient _QttClient;
    private final CryptUtil  _CryptUtil = new CryptUtil();

    @Autowired
    ClientController(WsZClient wsZClient,
                     QttZClient qttZClient)
    {
        _WsClient = wsZClient;
        _QttClient = qttZClient;
    }

    @GetMapping("/client/ws/start")
    public String ws_start()
    {
        _WsClient.connect();
        return "async commit ws_start client request";
    }

    @GetMapping("/client/qtt/start")
    public String qtt_start()
    {
        _QttClient.connect();
        return "async commit qtt_start client request";
    }

    @GetMapping("/client/ws/end")
    public String ws_end()
    {
        _WsClient.close();
        return "client ws_end";
    }

    @GetMapping("/client/ws/close")
    public String ws_close()
    {
        _WsClient.sendLocal(new X103_Close("client ws_close".getBytes()));
        return "client ws_close";
    }

    @GetMapping("/client/ws/heartbeat")
    public String ws_heartbeat(@RequestParam(name = "msg",
                                             defaultValue = "client ws_heartbeat",
                                             required = false) String msg)
    {
        _WsClient.heartbeat(msg);
        return "ws_heartbeat";
    }

    @GetMapping("/client/ws/x50")
    public String ws_x50(@RequestParam(name = "msg", defaultValue = "test", required = false) String msg)
    {
        X50_DeviceMsg x50 = new X50_DeviceMsg(System.currentTimeMillis());
        x50.setPayload(msg.getBytes());
        _WsClient.sendLocal(x50);
        return "ws_x50";
    }

    @GetMapping("/client/ws/sign-up")
    public String ws_x20(@RequestParam(name = "password", defaultValue = "password", required = false) String password,
                         @RequestParam(name = "mac", defaultValue = "AE:C3:33:44:56:09") String mac)
    {

        X20_SignUp x20 = new X20_SignUp();
        x20.setMac(IoUtil.writeMacRaw(mac));
        x20.setPassword(password);
        _WsClient.sendLocal(x20);
        return String.format("send ws_x20 to sign up, mac{ %s }password{ %s }", mac, password);
    }

    @GetMapping("/client/ws/sign-in")
    public String ws_x22(@RequestParam(name = "password", defaultValue = "password", required = false) String password,
                         @RequestParam(name = "token") String token)
    {
        X22_SignIn x22 = new X22_SignIn();
        if (Objects.nonNull(_WsClient.getToken())
            && !IoUtil.bin2Hex(_WsClient.getToken())
                      .equals(token)) throw new IllegalStateException(String.format("client already login with %s ", IoUtil.bin2Hex(_WsClient.getToken())));
        _WsClient.setToken(token);
        x22.setToken(_WsClient.getToken());
        x22.setPassword(password);
        _WsClient.sendLocal(x22);
        return String.format("login %s : %s", IoUtil.bin2Hex(_WsClient.getToken()), password);
    }

    @GetMapping("/client/ws/handshake")
    public String ws_handshake()
    {
        _WsClient.handshake();
        return "ws_handshake";
    }

    @GetMapping("/client/ws/ztls")
    public String ws_ztls()
    {
        X01_EncryptRequest x01 = new X01_EncryptRequest();
        _WsClient.sendLocal(x01);
        return "ws_ztls ws_start";
    }

}
