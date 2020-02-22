/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2020 Z-Chess
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

package com.tgx.chess.spring.device.api;

import static com.tgx.chess.king.base.util.IoUtil.isBlank;

import java.time.Instant;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.spring.device.model.DeviceDo;
import com.tgx.chess.spring.device.model.DeviceStatus;

/**
 * @author william.d.zk
 */
@RestController
public class DeviceController
{
    private final Logger         _Logger = Logger.getLogger(getClass().getSimpleName());
    private final IDeviceService _DeviceService;

    @Autowired
    public DeviceController(IDeviceService deviceService)
    {
        _DeviceService = deviceService;
    }

    @PostMapping("/device/register")
    public @ResponseBody DeviceDo registerDevice(@RequestBody DeviceDo deviceDo)
    {
        return _DeviceService.saveDevice(deviceDo);
    }

    @GetMapping("/device/query")
    public @ResponseBody IPair queryDevice(@RequestParam(required = false) String token,
                                           @RequestParam(required = false) String sn,
                                           @RequestParam(required = false) Long id)
    {
        if (!isBlank(token) || !isBlank(sn)) {
            DeviceDo deviceDo = new DeviceDo();
            deviceDo.setToken(token);
            deviceDo.setSn(sn);
            deviceDo = _DeviceService.findDevice(deviceDo);
            if (Objects.nonNull(deviceDo)) {
                if (deviceDo.getInvalidAt()
                            .isBefore(Instant.now()))
                {
                    return new Pair<>(DeviceStatus.INVALID, deviceDo);
                }
                else {
                    return new Pair<>(DeviceStatus.AVAILABLE, deviceDo);
                }
            }
        }
        else {
            DeviceDo deviceDo = new DeviceDo();
            deviceDo.setId(id == null ? 0
                                      : id);
            deviceDo = _DeviceService.findDevice(deviceDo);
            if (deviceDo != null) { return new Pair<>(DeviceStatus.AVAILABLE, deviceDo); }
        }
        return new Pair<>(DeviceStatus.MISS, null);
    }

    @GetMapping("/message")
    public @ResponseBody Object getMessage(@RequestParam(name = "id") long id) throws JsonProcessingException
    {
        return _DeviceService.getMessageById(id);
    }
}
