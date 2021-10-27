/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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

package com.isahl.chess.audience.client.api;

import com.isahl.chess.audience.client.component.ClientPool;
import com.isahl.chess.audience.client.model.Client;
import com.isahl.chess.bishop.io.sort.ZSortHolder;
import com.isahl.chess.bishop.io.ws.ctrl.X102_Close;
import com.isahl.chess.bishop.io.ws.zchat.model.command.X20_SignUp;
import com.isahl.chess.bishop.io.ws.zchat.model.command.X22_SignIn;
import com.isahl.chess.bishop.io.ws.zchat.model.command.X50_DeviceMsg;
import com.isahl.chess.bishop.io.ws.zchat.model.ctrl.zls.X01_EncryptRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * @author william.d.zk
 */
@RestController("audience/client")
public class ConsumerController
{
    private final ClientPool _ClientPool;

    @Autowired
    ConsumerController(ClientPool client)
    {
        _ClientPool = client;
    }

    @PostMapping("ws/start")
    public String wsStart(
            @RequestBody
                    Client client) throws IOException
    {
        _ClientPool.connect(ZSortHolder.WS_ZCHAT_CONSUMER, client);
        return "async commit ws_start consumer request";
    }

    @PostMapping("qtt/start")
    public String qttStart(
            @RequestBody
                    Client client) throws IOException
    {
        _ClientPool.connect(ZSortHolder.QTT_SYMMETRY, client);
        return "async commit qtt_start consumer request";
    }

    @GetMapping("ws/end")
    public String wsEnd(
            @RequestParam(name = "client_id")
                    long clientId)
    {
        _ClientPool.close(clientId);
        return "consumer wsEnd";
    }

    @GetMapping("ws/close")
    public String wsClose(
            @RequestParam(name = "client_id")
                    long clientId)
    {
        _ClientPool.sendLocal(clientId, new X102_Close("client ws_close".getBytes()));
        return "consumer ws_close";
    }

    @GetMapping("ws/heartbeat")
    public String wsHeartbeat(
            @RequestParam(name = "msg",
                          defaultValue = "client ws_heartbeat",
                          required = false)
                    String msg,
            @RequestParam(name = "client_id")
                    long clientId)
    {
        _ClientPool.wsHeartbeat(clientId, msg);
        return "ws_heartbeat";
    }

    @GetMapping("ws/x50")
    public String wsX50(
            @RequestParam(name = "msg",
                          defaultValue = "test",
                          required = false)
                    String msg,
            @RequestParam(name = "client_id")
                    long clientId)
    {
        X50_DeviceMsg x50 = new X50_DeviceMsg(System.currentTimeMillis());
        x50.putPayload(msg.getBytes());
        _ClientPool.sendLocal(clientId, x50);
        return "ws_x50";
    }

    @GetMapping("ws/sign-up")
    public String wsX20(
            @RequestParam(name = "password",
                          defaultValue = "password",
                          required = false)
                    String password,
            @RequestParam(name = "sn")
                    String sn,
            @RequestParam(name = "session_id")
                    long sessionId)
    {

        X20_SignUp x20 = new X20_SignUp();
        x20.setSn(sn);
        x20.setPassword(password);
        _ClientPool.sendLocal(sessionId, x20);
        return String.format("send ws_x20 to sign up, sn{ %s }password{ %s }", sn, password);
    }

    @GetMapping("ws/sign-in")
    public String wsX22(
            @RequestParam(name = "password",
                          defaultValue = "password",
                          required = false)
                    String password,
            @RequestParam(name = "token")
                    String token,
            @RequestParam(name = "session_id")
                    long sessionId)
    {
        X22_SignIn x22 = new X22_SignIn();
        // if (Objects.nonNull(_DeviceClient.getToken())
        // && !IoUtil.bin2Hex(_DeviceClient.getToken())
        // .equals(token)) throw new IllegalStateException(String.format("client already login with %s ",
        // IoUtil.bin2Hex(_DeviceClient.getToken())));
        // _DeviceClient.setToken(token);
        // x22.setToken(_DeviceClient.getToken());
        // x22.setPassword(password);
        // _DeviceClient.sendLocal(sessionId, x22);
        return String.format("login %s : %s", "", password);
    }

    @GetMapping("ws/ztls")
    public String wsZtls(
            @RequestParam(name = "client_id")
                    long clientId)
    {
        X01_EncryptRequest x01 = new X01_EncryptRequest();
        _ClientPool.sendLocal(clientId, x01);
        return "ws_ztls ws_start";
    }

}
