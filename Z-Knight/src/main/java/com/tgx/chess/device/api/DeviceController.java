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

import static com.tgx.chess.king.base.util.IoUtil.isBlank;

import java.time.Instant;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.bishop.biz.db.dao.DeviceStatus;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.spring.device.model.DeviceEntity;
import com.tgx.chess.spring.device.service.DeviceDo;
import com.tgx.chess.spring.device.service.DeviceService;
import com.tgx.chess.spring.device.service.ResponseDo;

/**
 * @author william.d.zk
 */
@RestController
public class DeviceController
{
    private final Logger        _Logger = Logger.getLogger(getClass().getName());
    private final DeviceService _DeviceService;

    @Autowired
    public DeviceController(DeviceService deviceService)
    {
        _DeviceService = deviceService;
    }

    @PostMapping("/device/register")
    public @ResponseBody DeviceDo registerDevice(@RequestBody DeviceDo deviceDo)
    {
        DeviceEntity deviceEntity = new DeviceEntity();
        deviceEntity.setImei(deviceDo.getImei());
        deviceEntity.setMac(deviceDo.getMac());
        deviceEntity.setSn(deviceDo.getSn());
        deviceEntity = _DeviceService.saveDevice(deviceEntity);
        deviceDo.setUserName(deviceEntity.getUserName());
        deviceDo.setPassword(deviceEntity.getPassword());
        deviceDo.setToken(deviceEntity.getToken());
        return deviceDo;
    }

    private DeviceDo convertEntity2Do(DeviceEntity entity)
    {
        DeviceDo deviceDo = new DeviceDo();
        deviceDo.setImei(entity.getImei());
        deviceDo.setMac(entity.getMac());
        deviceDo.setToken(entity.getToken());
        deviceDo.setSn(entity.getSn());
        deviceDo.setPassword(entity.getPassword());
        deviceDo.setPhone(entity.getPhone());
        deviceDo.setImsi(entity.getImsi());
        return deviceDo;
    }

    @GetMapping("/device/query")
    public @ResponseBody ResponseDo queryDevice(@RequestParam(required = false) String token,
                                                @RequestParam(required = false) String sn,
                                                @RequestParam(required = false) String mac,
                                                @RequestParam(required = false) String imei,
                                                @RequestParam(required = false) String imsi,
                                                @RequestParam(required = false) String phone)
    {
        ResponseDo responseDo = new ResponseDo();
        responseDo.setStatus(DeviceStatus.MISS);
        DeviceEntity entity = null;
        if (!isBlank(token)) {
            token = token.toUpperCase();
            entity = _DeviceService.findDeviceByToken(token);
        }
        else if (!isBlank(sn)) {
            sn = sn.toUpperCase();
            entity = _DeviceService.findDeviceBySn(sn);
        }
        else if (!isBlank(mac)) {
            mac = mac.toUpperCase();
            entity = _DeviceService.findDeviceByMac(mac);
        }
        else if (!isBlank(imei)) {
            imei = imei.toUpperCase();
            entity = _DeviceService.findDeviceByImei(imei);
        }
        else if (!isBlank(imsi)) {
            imsi = imsi.toUpperCase();
            entity = _DeviceService.findDeviceByImsi(imsi);
        }
        else if (!isBlank(phone)) {
            phone = phone.toUpperCase();
            entity = _DeviceService.findDeviceByPhone(phone);
        }

        if (Objects.nonNull(entity)) {
            responseDo.setDevice(convertEntity2Do(entity));
            if (entity.getInvalidAt()
                      .toInstant()
                      .isBefore(Instant.now()))
            {
                responseDo.setStatus(DeviceStatus.INVALID);
            }
            else if (isBlank(entity.getMac()) && isBlank(entity.getImei())) {
                responseDo.setStatus(DeviceStatus.INCOMPLETE);
            }
            else {
                responseDo.setStatus(DeviceStatus.AVAILABLE);
            }
        }
        return responseDo;
    }

    @GetMapping("/client/device")
    public @ResponseBody DeviceEntity getDevice(@RequestParam(name = "mac") String deviceMac)
    {
        return _DeviceService.findDeviceByMac(deviceMac);
    }

}
