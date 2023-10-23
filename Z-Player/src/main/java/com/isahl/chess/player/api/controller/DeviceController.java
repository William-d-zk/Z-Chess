/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.isahl.chess.player.api.controller;

import com.isahl.chess.king.base.content.ZResponse;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.config.CodeKing;
import com.isahl.chess.pawn.endpoint.device.db.central.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.resource.features.IStateService;
import com.isahl.chess.player.api.model.DeviceDo;
import com.isahl.chess.player.api.service.MixOpenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import static com.isahl.chess.king.base.util.IoUtil.isBlank;

/**
 * @author william.d.zk
 * {@code @date} 2019/05/01
 */
@RestController
@RequestMapping("device")
public class DeviceController
{
    private final Logger _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());

    private final MixOpenService _MixOpenService;

    @Autowired
    public DeviceController(MixOpenService mixOpenService)
    {
        _MixOpenService = mixOpenService;
    }

    @PostMapping("register")
    public @ResponseBody ZResponse<?> registerDevice(
            @RequestBody
            DeviceDo deviceDo)
    {
        DeviceEntity deviceEntity = new DeviceEntity();
        deviceEntity.setNumber(deviceDo.getNumber());
        deviceEntity.setUsername(deviceDo.getUsername());
        deviceEntity.setProfile(deviceDo.getProfile());
        deviceEntity.setCreatedById(deviceDo.getCreatedById());
        return ZResponse.success(_MixOpenService.newDevice(deviceEntity));
    }

    @GetMapping("open/query")
    public @ResponseBody ZResponse<?> queryDeviceByToken(
            @RequestParam("token")
            String token)
    {
        if(!isBlank(token)) {
            DeviceEntity exist = _MixOpenService.findByToken(token);
            if(exist != null) {return ZResponse.success(exist);}
        }
        return ZResponse.error(CodeKing.MISS.getCode(), "device miss");
    }

    @GetMapping("manager/query")
    public @ResponseBody ZResponse<?> queryDeviceByNumber(
            @RequestParam("number")
            String number)
    {
        if(!isBlank(number)) {
            DeviceEntity exist = _MixOpenService.findByNumber(number);
            if(exist != null) {return ZResponse.success(exist);}
        }
        return ZResponse.error(CodeKing.MISS.getCode(), "device miss");
    }

    @GetMapping("online/all")
    public @ResponseBody ZResponse<?> listOnlineDevices(
            @RequestParam(value = "page",
                          defaultValue = "0",
                          required = false)
            Integer page,
            @RequestParam(value = "size",
                          defaultValue = "20",
                          required = false)
            Integer size)
    {
        size = size < 1 ? 10 : size > 50 ? 50 : size;
        page = page < 0 ? 0 : page;
        return ZResponse.success(_MixOpenService.getOnlineDevice(PageRequest.of(page, size)));
    }

    @GetMapping("online/stored")
    public @ResponseBody ZResponse<?> listStored(
            @RequestParam(value = "page",
                          required = false,
                          defaultValue = "0")
            Integer page,
            @RequestParam(value = "size",
                          defaultValue = "20",
                          required = false)
            Integer size)
    {
        size = size < 1 ? 10 : size > 50 ? 50 : size;
        page = page < 0 ? 0 : page;
        return ZResponse.success(_MixOpenService.getStorageDevice(PageRequest.of(page, size)));
    }
}
