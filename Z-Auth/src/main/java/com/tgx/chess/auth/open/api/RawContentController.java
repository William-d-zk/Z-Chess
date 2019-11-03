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

package com.tgx.chess.auth.open.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgx.chess.spring.device.model.MessageBody;
import com.tgx.chess.spring.device.service.DeviceService;

/**
 * @author william.d.zk
 * @date 2019/11/3
 */
@RestController
public class RawContentController
{

    private final DeviceService _DeviceService;

    @Autowired
    public RawContentController(DeviceService deviceService)
    {
        _DeviceService = deviceService;
    }

    @GetMapping("/message/target")
    public @ResponseBody Object getMessage(@RequestParam(name = "target") long target,
                                           @RequestParam(name = "offset",
                                                         defaultValue = "" + Long.MIN_VALUE) long offset)
                                                                                                          throws JsonProcessingException
    {

        List<MessageBody> result = _DeviceService.listMessageByOriginAndMsgIdAfter(target, offset);
        return result;
//        return new ObjectMapper().writer()
//                                 .writeValueAsString(result);

    }
}
