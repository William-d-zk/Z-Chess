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

import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgx.chess.bishop.biz.db.dao.DeviceStatus;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.spring.jpa.device.dao.DeviceEntity;
import com.tgx.chess.spring.device.model.MessageBody;
import com.tgx.chess.spring.device.model.DeviceDo;
import com.tgx.chess.spring.device.service.DeviceService;

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
        deviceEntity.setSn(deviceDo.getSn());
        deviceEntity = _DeviceService.saveDevice(deviceEntity);
        deviceDo.setPassword(deviceEntity.getPassword());
        deviceDo.setToken(deviceEntity.getToken());
        return deviceDo;
    }

    private DeviceDo convertEntity2Do(DeviceEntity entity)
    {
        DeviceDo deviceDo = new DeviceDo();
        deviceDo.setToken(entity.getToken());
        deviceDo.setSn(entity.getSn());
        deviceDo.setPassword(entity.getPassword());
        return deviceDo;
    }

    @GetMapping("/device/query")
    public @ResponseBody IPair queryDevice(@RequestParam(required = false) String token,
                                           @RequestParam(required = false) String sn)
    {
        DeviceEntity entity = null;
        if (!isBlank(token)) {
            token = token.toUpperCase();
            entity = _DeviceService.findDeviceByToken(token);
        }
        else if (!isBlank(sn)) {
            sn = sn.toUpperCase();
            entity = _DeviceService.findDeviceBySn(sn);
        }
        if (Objects.nonNull(entity)) {
            DeviceDo deviceDo = convertEntity2Do(entity);
            if (entity.getInvalidAt()
                      .toInstant()
                      .isBefore(Instant.now()))
            {
                return new Pair<>(DeviceStatus.INVALID, deviceDo);
            }
            else {
                return new Pair<>(DeviceStatus.AVAILABLE, deviceDo);
            }
        }
        return new Pair<>(DeviceStatus.MISS, null);
    }

    @GetMapping("/message")
    public @ResponseBody Object getMessage(@RequestParam(name = "id") long id) throws JsonProcessingException
    {
        MessageBody result = _DeviceService.getMessageById(id);
        return new ObjectMapper().writer()
                                 .writeValueAsString(result);
    }
}
