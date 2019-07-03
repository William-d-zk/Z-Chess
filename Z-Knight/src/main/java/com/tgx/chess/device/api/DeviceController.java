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
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.bishop.biz.db.dao.DeviceEntry;
import com.tgx.chess.bishop.biz.db.dao.DeviceStatus;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.spring.device.model.DeviceEntity;
import com.tgx.chess.spring.device.service.DeviceDo;
import com.tgx.chess.spring.device.service.DeviceService;
import com.tgx.chess.spring.device.service.ResponseDo;
import com.tgx.chess.spring.jpa.generator.ZGenerator;

/**
 * @author william.d.zk
 */
@RestController
public class DeviceController
{
    private final Logger        _Logger     = Logger.getLogger(getClass().getName());
    private final DeviceService _DeviceService;
    private final ZGenerator    _ZGenerator = new ZGenerator();

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
            entity = _DeviceService.findDeviceBySn(mac);
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

    @GetMapping("/client/devices")
    public @ResponseBody Object getDevices()
    {
        List<DeviceEntry> list = _DeviceService.findAll();
        return Objects.nonNull(list) && !list.isEmpty() ? list
                                                        : "no devices";
    }

    @GetMapping("/client/device")
    public @ResponseBody DeviceEntity getDevice(@RequestParam(name = "mac") String deviceMac)
    {
        return _DeviceService.findDeviceByMac(deviceMac);
    }
    /*
    
    @GetMapping("/client/close/all")
    public String closeAll()
    {
        List<DeviceEntry> list = _DeviceService.findAll();
        StringBuffer sb = new StringBuffer();
        for (DeviceEntry device : list) {
            _Logger.info("device mac %s", device.getToken());
            sb.append(String.format("token:%s\n", device.getToken()));
            _DeviceService.localBizClose(device.getPrimaryKey());
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
    public String x30Broadcast(@RequestParam(name = "msg") String msg,
                               @RequestParam(name = "ctrl", defaultValue = "0") int ctrl)
    {
        List<DeviceEntry> list = _DeviceService.findAll();
        StringBuffer sb = new StringBuffer();
        for (DeviceEntry device : list) {
            _Logger.info("device mac %s", device.getToken());
            sb.append(String.format("token:%s\n", device.getToken()));
            sendX30(device.getToken(), device.getPrimaryKey(), msg, ctrl);
        }
        return sb.toString();
    }
    
    @GetMapping("/event/x30/push")
    public String x30(@RequestParam(name = "msg") String msg,
                      @RequestParam(name = "token") String token,
                      @RequestParam(name = "ctrl", defaultValue = "0") int ctrl)
    {
        DeviceEntity device = _DeviceService.findDeviceByToken(token.toUpperCase());
        if (Objects.nonNull(device)) {
            try {
                sendX30(device.getToken(), device.getId(), msg, ctrl);
            }
            catch (NullPointerException e) {
                return String.format("device %s not login", token);
            }
            return String.format("push %s -> device %s", msg, token);
        }
        return String.format("not found device %s", token);
    }
    
    private void sendX30(String token, long deviceId, String msg, int ctrl)
    {
        X30_EventMsg x30 = new X30_EventMsg(_ZGenerator.next());
        x30.setCtrl(X30_EventMsg.CTRL_TEXT);
        x30.setToken(token.toUpperCase());
        x30.setPayload(msg.getBytes(StandardCharsets.UTF_8));
        _DeviceService.localBizSend(deviceId, x30);
    }
     */
}
