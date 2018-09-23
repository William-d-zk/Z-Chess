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

package com.tgx.chess.device.api;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.spring.device.service.DeviceService;
import com.tgx.chess.spring.device.model.Device;

@RestController
public class DeviceController
{
    private final Logger        _Log    = Logger.getLogger(getClass().getName());
    private final DeviceService _DeviceService;
    private final Random        _Random = new Random();

    @Autowired
    public DeviceController(DeviceService deviceService) {
        this._DeviceService = deviceService;
    }

    @GetMapping("/client/register")
    public Object registrationDevice(@RequestParam(name = "sn") String sn) {
        Device device = new Device();
        device.setSn(IoUtil.hex2bin(sn));
        byte[] password = new byte[6];
        _Random.nextBytes(password);
        device.setPassword(IoUtil.bin2Hex(password));
        return _DeviceService.saveDevice(device);
    }

    @GetMapping("/client/device")
    public Object getDevice() {
        List<Device> list = _DeviceService.findAll();
        for (Device device : list) {
            String sn = IoUtil.bin2Hex(device.getSn(), ".");
            _Log.info("device sn %s", sn);
        }
        return list;
    }

}
