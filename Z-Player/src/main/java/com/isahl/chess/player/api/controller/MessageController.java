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
import com.isahl.chess.king.config.KingCode;
import com.isahl.chess.pawn.endpoint.device.db.central.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.resource.features.IDeviceService;
import com.isahl.chess.player.api.model.MessageDo;
import com.isahl.chess.player.api.service.MessageOpenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author william.d.zk
 * {@code @date} 2019/11/3
 */
@RestController
@RequestMapping("message")
public class MessageController
{
    private final Logger             _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());
    private final MessageOpenService _MessageService;
    private final IDeviceService     _DeviceService;

    @Autowired
    public MessageController(MessageOpenService messageService, IDeviceService deviceService)
    {
        _MessageService = messageService;
        _DeviceService = deviceService;
    }

    @PostMapping("submit")
    public ZResponse<?> submit(
            @RequestParam(name = "token")
            String token,
            @RequestBody
            MessageDo body)
    {
        DeviceEntity device = _DeviceService.findByToken(token);
        if(device == null) {
            return ZResponse.error(KingCode.MISS, "origin device not found");
        }
        _MessageService.submit(device.getId(), body);
        return ZResponse.success("submit succeed");
    }
}
