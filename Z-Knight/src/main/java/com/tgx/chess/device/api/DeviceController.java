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

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.spring.device.model.DeviceEntity;
import com.tgx.chess.spring.device.service.DeviceService;

@RestController
public class DeviceController
{
    private final Logger        _Log    = Logger.getLogger(getClass().getName());
    private final DeviceService _DeviceService;
    private final Random        _Random = new Random();

    @Autowired
    public DeviceController(DeviceService deviceService)
    {
        _DeviceService = deviceService;
    }

    @GetMapping("/client/devices")
    public @ResponseBody List<DeviceEntity> getDevices()
    {
        List<DeviceEntity> list = _DeviceService.findAll();
        for (DeviceEntity device : list) {
            _Log.info("device mac %s", device.getMac());
        }
        return list;
    }

    @GetMapping("/client/device")
    public @ResponseBody DeviceEntity getDevice(@RequestParam(name = "mac") String deviceMac)
    {
        return _DeviceService.findDevice(deviceMac);
    }
}
