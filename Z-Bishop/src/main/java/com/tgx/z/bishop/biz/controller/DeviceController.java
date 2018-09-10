package com.tgx.z.bishop.biz.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tgx.z.bishop.biz.db.dto.Device;
import com.tgx.z.bishop.biz.db.service.DeviceService;
import com.tgx.z.queen.base.util.IoUtil;

@Controller
public class DeviceController
{
    private final DeviceService _DeviceService;
    private final Random        _Random = new Random();

    @Autowired
    public DeviceController(DeviceService deviceService) {
        this._DeviceService = deviceService;
    }

    @GetMapping("/client/register")
    public Object registrationDevice(@RequestParam(name = "sn") String sn) {
        Device device = new Device();
        device.setSn(sn);
        byte[] password = new byte[6];
        _Random.nextBytes(password);
        device.setPassword(IoUtil.bin2Hex(password));
        return _DeviceService.saveDevice(device);
    }

}
