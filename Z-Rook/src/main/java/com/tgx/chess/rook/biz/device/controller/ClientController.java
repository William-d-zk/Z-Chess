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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.bishop.io.ws.control.X103_Close;
import com.tgx.chess.bishop.io.zprotocol.device.X20_SignUp;
import com.tgx.chess.bishop.io.zprotocol.device.X22_SignIn;
import com.tgx.chess.bishop.io.zprotocol.device.X50_DeviceMsg;
import com.tgx.chess.bishop.io.zprotocol.ztls.X01_EncryptRequest;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.rook.biz.device.client.DeviceClient;
import com.tgx.chess.rook.io.ClientZSort;

/**
 * @author william.d.zk
 */
@RestController
@PropertySource({ "classpath:ws.client.properties",
                  "classpath:qtt.client.properties" })
public class ClientController
{
    private final DeviceClient _DeviceClient;
    private final String       _WsHost;
    private final int          _WsPort;
    private final String       _QttHost;
    private final int          _QttPort;

    @Autowired
    ClientController(DeviceClient client,
                     @Value("${ws.client.target.host}") String wsHost,
                     @Value("${ws.client.target.port}") int wsPort,
                     @Value("${qtt.client.target.host}") String qttHost,
                     @Value("${qtt.client.target.port}") int qttPort)
    {
        _DeviceClient = client;
        _WsHost = wsHost;
        _WsPort = wsPort;
        _QttHost = qttHost;
        _QttPort = qttPort;
    }

    @GetMapping("/client/ws/start")
    public String wsStart(@RequestParam(name = "client_id") long clientId)
    {
        _DeviceClient.connect(_WsHost, _WsPort, ClientZSort.WS_CONSUMER, clientId);
        return "async commit ws_start client request";
    }

    @GetMapping("/client/qtt/start")
    public String qttStart(@RequestParam(name = "client_id") long clientId)
    {
        _DeviceClient.connect(_QttHost, _QttPort, ClientZSort.QTT_SYMMETRY, clientId);
        return "async commit qtt_start client request";
    }

    @GetMapping("/client/ws/end")
    public String wsEnd(@RequestParam(name = "client_id") long clientId)
    {
        _DeviceClient.close(clientId);
        return "client wsEnd";
    }

    @GetMapping("/client/ws/close")
    public String wsClose(@RequestParam(name = "client_id") long clientId)
    {
        _DeviceClient.sendLocal(clientId, new X103_Close("client ws_close".getBytes()));
        return "client ws_close";
    }

    @GetMapping("/client/ws/heartbeat")
    public String wsHeartbeat(@RequestParam(name = "msg",
                                            defaultValue = "client ws_heartbeat",
                                            required = false) String msg,
                              @RequestParam(name = "client_id") long clientId)
    {
        _DeviceClient.wsHeartbeat(clientId, msg);
        return "ws_heartbeat";
    }

    @GetMapping("/client/ws/x50")
    public String wsX50(@RequestParam(name = "msg", defaultValue = "test", required = false) String msg,
                        @RequestParam(name = "client_id") long clientId)
    {
        X50_DeviceMsg x50 = new X50_DeviceMsg(System.currentTimeMillis());
        x50.setPayload(msg.getBytes());
        _DeviceClient.sendLocal(clientId, x50);
        return "ws_x50";
    }

    @GetMapping("/client/ws/sign-up")
    public String wsX20(@RequestParam(name = "password", defaultValue = "password", required = false) String password,
                        @RequestParam(name = "mac", defaultValue = "AE:C3:33:44:56:09") String mac,
                        @RequestParam(name = "client_id") long clientId)
    {

        X20_SignUp x20 = new X20_SignUp();
        x20.setMac(IoUtil.writeMacRaw(mac));
        x20.setPassword(password);
        _DeviceClient.sendLocal(clientId, x20);
        return String.format("send ws_x20 to sign up, mac{ %s }password{ %s }", mac, password);
    }

    @GetMapping("/client/ws/sign-in")
    public String wsX22(@RequestParam(name = "password", defaultValue = "password", required = false) String password,
                        @RequestParam(name = "token") String token,
                        @RequestParam(name = "client_id") long clientId)
    {
        X22_SignIn x22 = new X22_SignIn();
        if (Objects.nonNull(_DeviceClient.getToken())
            && !IoUtil.bin2Hex(_DeviceClient.getToken())
                      .equals(token)) throw new IllegalStateException(String.format("client already login with %s ", IoUtil.bin2Hex(_DeviceClient.getToken())));
        _DeviceClient.setToken(token);
        x22.setToken(_DeviceClient.getToken());
        x22.setPassword(password);
        _DeviceClient.sendLocal(clientId, x22);
        return String.format("login %s : %s", IoUtil.bin2Hex(_DeviceClient.getToken()), password);
    }

    @GetMapping("/client/ws/ztls")
    public String wsZtls(@RequestParam(name = "client_id") long clientId)
    {
        X01_EncryptRequest x01 = new X01_EncryptRequest();
        _DeviceClient.sendLocal(clientId, x01);
        return "ws_ztls ws_start";
    }

}
