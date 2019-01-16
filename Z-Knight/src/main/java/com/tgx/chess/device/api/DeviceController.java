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

package com.tgx.chess.device.api;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.bishop.biz.db.dao.DeviceEntry;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X30_EventMsg;
import com.tgx.chess.spring.device.model.DeviceEntity;
import com.tgx.chess.spring.device.service.DeviceService;
import com.tgx.chess.spring.jpa.generator.ZGenerator;

@RestController
public class DeviceController
{
    private final Logger        _Log        = Logger.getLogger(getClass().getName());
    private final DeviceService _DeviceService;
    private final Random        _Random     = new Random();
    private final ZGenerator    _ZGenerator = new ZGenerator();

    @Autowired
    public DeviceController(DeviceService deviceService)
    {
        _DeviceService = deviceService;
    }

    @GetMapping("/client/devices")
    public @ResponseBody Object getDevices()
    {
        List<DeviceEntry> list = _DeviceService.findAll();
        return Objects.nonNull(list) ? list
                                     : "no devices";
    }

    @GetMapping("/client/device")
    public @ResponseBody DeviceEntity getDevice(@RequestParam(name = "mac") String deviceMac)
    {
        return _DeviceService.findDeviceByMac(deviceMac);
    }

    @GetMapping("/client/close/all")
    public String closeAll()
    {
        List<DeviceEntry> list = _DeviceService.findAll();
        StringBuffer sb = new StringBuffer();
        for (DeviceEntry device : list) {
            _Log.info("device mac %s", device.getToken());
            sb.append(String.format("token:%s\n", device.getToken()));
            _DeviceService.localBizClose(device.getDeviceUID());
        }
        return sb.toString();
    }

    @GetMapping("/client/close/device")
    public String close(@RequestParam(name = "token") String token)
    {
        DeviceEntity device = _DeviceService.findDeviceByToken(token);
        if (Objects.nonNull(device)) {
            _DeviceService.localBizClose(device.getId());
        }
        return token;
    }

    @GetMapping("/event/x30/broadcast")
    public String x30Broadcast(@RequestParam(name = "msg") String msg, @RequestParam(name = "ctrl", defaultValue = "0") int ctrl)
    {
        List<DeviceEntry> list = _DeviceService.findAll();
        StringBuffer sb = new StringBuffer();
        for (DeviceEntry device : list) {
            _Log.info("device mac %s", device.getToken());
            sb.append(String.format("token:%s\n", device.getToken()));
            sendX30(device.getToken(), device.getDeviceUID(), msg, ctrl);
        }
        return sb.toString();
    }

    @GetMapping("/event/x30/push")
    public String x30(@RequestParam(name = "msg") String msg,
                      @RequestParam(name = "token") String token,
                      @RequestParam(name = "ctrl", defaultValue = "0") int ctrl)
    {
        DeviceEntity device = _DeviceService.findDeviceByToken(token);
        if (Objects.nonNull(device)) {
            sendX30(device.getToken(), device.getId(), msg, ctrl);
            return String.format("push %s -> device %s", msg, token);
        }
        return String.format("not found device %s", token);
    }

    private void sendX30(String token, long deviceId, String msg, int ctrl)
    {
        X30_EventMsg x30 = new X30_EventMsg(_ZGenerator.next());
        x30.setCtrl(X30_EventMsg.CTRL_TEXT);
        x30.setToken(token);
        x30.setPayload(msg.getBytes(StandardCharsets.UTF_8));
        _DeviceService.localBizSend(deviceId, x30);
    }
}
