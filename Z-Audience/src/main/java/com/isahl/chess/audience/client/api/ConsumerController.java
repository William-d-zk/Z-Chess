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

package com.isahl.chess.audience.client.api;

import com.isahl.chess.audience.client.component.ClientPool;
import com.isahl.chess.audience.client.model.Client;
import com.isahl.chess.bishop.protocol.zchat.model.command.X0D_PlainText;
import com.isahl.chess.bishop.sort.ZSortHolder;
import com.isahl.chess.king.base.content.ZResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * @author william.d.zk
 */
@RestController("audience/client")
public class ConsumerController
{
    private final ClientPool _ClientPool;
    private final Client     _Client;

    @Autowired
    ConsumerController(ClientPool pool, Client client)
    {
        _ClientPool = pool;
        _Client = client;
    }

    @GetMapping("/zchat")
    public ZResponse<?> zchat_connect() throws IOException
    {
        _ClientPool.connect(ZSortHolder.Z_CLUSTER_SYMMETRY, _Client);
        return ZResponse.success("connect");
    }

    @GetMapping("/zchat/send")
    public ZResponse<?> zchat_send(
            @RequestParam(name = "output")
                    String output)
    {
        _ClientPool.sendLocal(_Client.getSession(), new X0D_PlainText().setText(output));
        return ZResponse.success("send");
    }

}
