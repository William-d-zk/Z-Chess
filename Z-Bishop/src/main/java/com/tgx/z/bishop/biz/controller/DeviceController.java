package com.tgx.z.bishop.biz.controller;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.z.bishop.biz.db.dto.Device;
import com.tgx.z.bishop.biz.db.service.DeviceService;
import com.tgx.z.queen.base.log.Logger;
import com.tgx.z.queen.base.util.IoUtil;

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
