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

import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.pawn.endpoint.device.api.model.MessageBody;
import com.isahl.chess.pawn.endpoint.device.db.remote.postgres.model.MessageEntity;
import com.isahl.chess.queen.io.core.features.cluster.IClusterPeer;
import com.isahl.chess.queen.io.core.tasks.features.ILocalPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author william.d.zk
 */
@Service
public class MessageOpenService
{
    private final Logger          _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());
    private final IClusterPeer    _ClusterPeer;
    private final ILocalPublisher _Publisher;

    @Autowired
    public MessageOpenService(IClusterPeer clusterPeer, ILocalPublisher publisher)
    {
        _ClusterPeer = clusterPeer;
        _Publisher = publisher;
    }

    public void submit(long deviceId, MessageBody body)
    {
        long msgId = _ClusterPeer.generateId();
        MessageEntity entity = new MessageEntity();
        entity.setId(msgId);
        entity.setContent(body.getContent());
        entity.setTopic(body.getTopic());
        entity.setOrigin(deviceId);
        entity.setNetAt(LocalDateTime.now());
        _Publisher.publish(IOperator.Type.SERVICE, null, Pair.of(entity, null));
    }
}
