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

package com.isahl.chess.player.api.service;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.knight.cluster.model.ConsistentProtocol;
import com.isahl.chess.knight.raft.RaftPeer;
import com.isahl.chess.knight.cluster.inf.IConsistencyService;
import com.isahl.chess.pawn.endpoint.device.DeviceNode;
import com.isahl.chess.queen.event.handler.cluster.IConsistencyCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class ConsistencyOpenService
        implements IConsistencyService
{

    private final Logger _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());

    private final DeviceNode           _DeviceNode;
    private final RaftPeer<DeviceNode> _RaftPeer;
    private final IConsistencyCustom   _ConsistencyCustom;

    @Autowired
    public ConsistencyOpenService(DeviceNode deviceNode,
                                  RaftPeer<DeviceNode> raftPeer,
                                  IConsistencyCustom consistentCustom)
    {
        _DeviceNode = deviceNode;
        _RaftPeer = raftPeer;
        _ConsistencyCustom = consistentCustom;
    }

    @Override
    public void submit(String content, boolean pub, long origin)
    {
        if(IoUtil.isBlank(content)) { return; }
        ConsistentProtocol consensus = new ConsistentProtocol(content.getBytes(StandardCharsets.UTF_8),
                                                              pub,
                                                              _RaftPeer.generateId(),
                                                              origin);
        submit(consensus, _DeviceNode, _ConsistencyCustom);
        _Logger.debug("consistent submit %s", consensus);
    }
}
