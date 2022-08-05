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

package com.isahl.chess.knight.cluster.features;

import com.isahl.chess.king.base.features.ICode;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.config.CodeKing;
import com.isahl.chess.knight.cluster.IClusterNode;
import com.isahl.chess.knight.cluster.config.CodeKnight;
import com.isahl.chess.queen.events.model.QEvent;
import com.lmax.disruptor.RingBuffer;

import java.util.concurrent.locks.ReentrantLock;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.CONSISTENT_SERVICE;

public interface IConsistencyService
{
    default <T extends IoSerial> ICode submit(T request, IClusterNode node)
    {
        if(request == null || node == null) {
            return CodeKing.MISS;
        }
        if(!node.clusterPeer()
                .isInCongress())
        {
            return CodeKnight.CLUSTER_NO_IN_CONGRESS;
        }
        final ReentrantLock _Lock = node.selectLock(CONSISTENT_SERVICE);
        final RingBuffer<QEvent> _Publish = node.selectPublisher(CONSISTENT_SERVICE);
        _Lock.lock();
        try {
            long sequence = _Publish.next();
            try {
                QEvent event = _Publish.get(sequence);
                event.produce(CONSISTENT_SERVICE,
                              new Pair<>(request,
                                         node.clusterPeer()
                                             .peerId()),
                              null);
            }
            finally {
                _Publish.publish(sequence);
            }
        }
        finally {
            _Lock.unlock();
        }
        return CodeKing.SUCCESS;
    }

    ICode submit(String content);

    ICode modify(String host, int port);

    ICode modify(String host, String gate, int gatePort);

}
