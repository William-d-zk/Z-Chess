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

package com.tgx.chess.rook.biz.device.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.bishop.io.ws.control.X103_Close;
import com.tgx.chess.bishop.io.zprotocol.device.X20_SignUp;
import com.tgx.chess.bishop.io.zprotocol.device.X22_SignIn;
import com.tgx.chess.bishop.io.zprotocol.device.X50_DeviceMsg;
import com.tgx.chess.bishop.io.zprotocol.ztls.X01_EncryptRequest;
import com.tgx.chess.rook.biz.device.client.DeviceConsumer;
import com.tgx.chess.rook.biz.device.client.ZClient;
import com.tgx.chess.rook.io.ConsumerZSort;

/**
 * @author william.d.zk
 */
@RestController
public class ConsumerController
{
    private final DeviceConsumer _DeviceClient;

    @Autowired
    ConsumerController(DeviceConsumer client)
    {
        _DeviceClient = client;
    }

    @PostMapping("/consumer/ws/start")
    public String wsStart(@RequestBody ZClient zClient) throws IOException
    {
        _DeviceClient.connect(ConsumerZSort.WS_CONSUMER, zClient);
        return "async commit ws_start consumer request";
    }

    @PostMapping("/consumer/qtt/start")
    public String qttStart(@RequestBody ZClient zClient) throws IOException
    {
        _DeviceClient.connect(ConsumerZSort.QTT_SYMMETRY, zClient);
        return "async commit qtt_start consumer request";
    }

    @GetMapping("/consumer/ws/end")
    public String wsEnd(@RequestParam(name = "client_id") long clientId)
    {
        _DeviceClient.close(clientId);
        return "consumer wsEnd";
    }

    @GetMapping("/consumer/ws/close")
    public String wsClose(@RequestParam(name = "client_id") long clientId)
    {
        _DeviceClient.sendLocal(clientId, new X103_Close("client ws_close".getBytes()));
        return "consumer ws_close";
    }

    @GetMapping("/consumer/ws/heartbeat")
    public String wsHeartbeat(@RequestParam(name = "msg",
                                            defaultValue = "client ws_heartbeat",
                                            required = false) String msg,
                              @RequestParam(name = "client_id") long clientId)
    {
        _DeviceClient.wsHeartbeat(clientId, msg);
        return "ws_heartbeat";
    }

    @GetMapping("/consumer/ws/x50")
    public String wsX50(@RequestParam(name = "msg", defaultValue = "test", required = false) String msg,
                        @RequestParam(name = "client_id") long clientId)
    {
        X50_DeviceMsg x50 = new X50_DeviceMsg(System.currentTimeMillis());
        x50.setPayload(msg.getBytes());
        _DeviceClient.sendLocal(clientId, x50);
        return "ws_x50";
    }

    @GetMapping("/consumer/ws/sign-up")
    public String wsX20(@RequestParam(name = "password", defaultValue = "password", required = false) String password,
                        @RequestParam(name = "sn") String sn,
                        @RequestParam(name = "session_id") long sessionId)
    {

        X20_SignUp x20 = new X20_SignUp();
        x20.setSn(sn);
        x20.setPassword(password);
        _DeviceClient.sendLocal(sessionId, x20);
        return String.format("send ws_x20 to sign up, sn{ %s }password{ %s }", sn, password);
    }

    @GetMapping("/consumer/ws/sign-in")
    public String wsX22(@RequestParam(name = "password", defaultValue = "password", required = false) String password,
                        @RequestParam(name = "token") String token,
                        @RequestParam(name = "session_id") long sessionId)
    {
        X22_SignIn x22 = new X22_SignIn();
        //        if (Objects.nonNull(_DeviceClient.getToken())
        //            && !IoUtil.bin2Hex(_DeviceClient.getToken())
        //                      .equals(token)) throw new IllegalStateException(String.format("client already login with %s ", IoUtil.bin2Hex(_DeviceClient.getToken())));
        //        _DeviceClient.setToken(token);
        //        x22.setToken(_DeviceClient.getToken());
        //        x22.setPassword(password);
        //        _DeviceClient.sendLocal(sessionId, x22);
        return String.format("login %s : %s", "", password);
    }

    @GetMapping("/consumer/ws/ztls")
    public String wsZtls(@RequestParam(name = "client_id") long clientId)
    {
        X01_EncryptRequest x01 = new X01_EncryptRequest();
        _DeviceClient.sendLocal(clientId, x01);
        return "ws_ztls ws_start";
    }

}
