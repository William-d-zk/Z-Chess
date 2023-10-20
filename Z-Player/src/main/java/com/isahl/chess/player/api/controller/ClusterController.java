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
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.knight.raft.features.IRaftMapper;
import com.isahl.chess.knight.raft.features.IRaftService;
import com.isahl.chess.player.api.model.ClusterDo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author william.d.zk
 * @date 2021/6/11
 */
@RestController
@RequestMapping("cluster")
public class ClusterController
{
    private final Logger       _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());
    private final IRaftService _RaftService;
    private final IRaftMapper  _RaftMapper;

    @Autowired
    public ClusterController(IRaftService raftService, IRaftMapper raftMapper)
    {
        _RaftService = raftService;
        _RaftMapper = raftMapper;
    }

    @PostMapping("change")
    public @ResponseBody
    ZResponse<?> changeTopology(
            @RequestBody
                    ClusterDo peer)
    {
        Triple<Long, String, Integer> triple = new Triple<>(peer.getPeerId(), peer.getHost(), peer.getPort());

        try {
            return ZResponse.success(_RaftService.topology());
        }
        catch(ZException e) {
            return ZResponse.error(e.getMessage());
        }
    }

    @GetMapping("close")
    public ZResponse<?> close()
    {
        _RaftMapper.flushAll();
        return ZResponse.success(_RaftMapper.getLogMeta());
    }

}




