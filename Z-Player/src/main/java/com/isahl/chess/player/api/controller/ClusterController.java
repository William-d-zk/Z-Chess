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

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.response.ZResponse;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.knight.raft.service.RaftService;
import com.isahl.chess.pawn.endpoint.device.DeviceNode;
import com.isahl.chess.player.api.model.ClusterDo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author william.d.zk
 * @date 2021/6/11
 */
@RestController("cluster")
public class ClusterController
{
    private final RaftService<DeviceNode> _RaftService;

    @Autowired
    public ClusterController(RaftService<DeviceNode> raftService)
    {
        _RaftService = raftService;
    }

    @PostMapping("change")
    public @ResponseBody
    ZResponse<?> changeTopology(@RequestBody ClusterDo peer)
    {
        Triple<Long, String, Integer> triple = new Triple<>(peer.getPeerId(), peer.getHost(), peer.getPort());
        try {
            //            _RaftService.appendPeer(triple);
            return ZResponse.success(_RaftService.getTopology());
        }
        catch(ZException e) {
            return ZResponse.error(e.getMessage());
        }
    }

}