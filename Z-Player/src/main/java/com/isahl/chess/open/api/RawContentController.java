/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.open.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isahl.chess.king.base.response.ZResponse;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.MessageBody;
import com.isahl.chess.pawn.endpoint.spring.device.spi.IMessageService;

/**
 * @author william.d.zk
 * 
 * @date 2019/11/3
 */
@RestController
public class RawContentController
{

    private final IMessageService _MessageService;

    @Autowired
    public RawContentController(IMessageService messageService)
    {
        _MessageService = messageService;
    }

    @GetMapping("/message/topic")
    public ZResponse<List<MessageBody>> getMessageByTopic(@RequestParam(name = "topic") String topic,
                                                          @RequestParam(name = "limit",
                                                                        defaultValue = "1",
                                                                        required = false) int limit)
    {
        return ZResponse.success(_MessageService.listByTopic(topic, limit));
    }

}
