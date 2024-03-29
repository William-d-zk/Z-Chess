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
import com.isahl.chess.king.base.features.ICode;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.knight.cluster.features.IConsistencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author william.d.zk
 * @date 2019/12/01
 */
@RestController
@RequestMapping("api")
public class ConsistencyController
{
    private final Logger              _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());
    private final IConsistencyService _ConsistencyService;

    @Autowired
    public ConsistencyController(IConsistencyService consistentService)
    {
        _ConsistencyService = consistentService;
    }

    @PostMapping("/consistent")
    public @ResponseBody
    ZResponse<?> consistency(String input)
    {
        ICode result = _ConsistencyService.submit(input);
        return ZResponse.of(result, input, "consistency submit");
    }
}
