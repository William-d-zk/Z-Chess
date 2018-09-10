package com.tgx.z.bishop.biz.db.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tgx.z.bishop.biz.db.dto.Device;
import com.tgx.z.bishop.biz.db.repository.DeviceRepository;

@Service
public class DeviceService
{
    private final DeviceRepository deviceRepository;

    @Autowired
    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public Device findBySn(String sn) {
        return deviceRepository.findBySn(sn);
    }

    public List<Device> findAll() {
        return deviceRepository.findAll();
    }

    public Device saveDevice(Device device) {
        return deviceRepository.save(device);
    }
}
