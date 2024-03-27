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

import com.isahl.chess.king.base.disruptor.features.functions.OperateType;
import com.isahl.chess.king.base.features.ICode;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.config.CodeKing;
import com.isahl.chess.pawn.endpoint.device.DeviceNode;
import com.isahl.chess.player.api.model.EchoDo;
import com.isahl.chess.queen.events.model.QEvent;
import com.lmax.disruptor.RingBuffer;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
public class HookOpenService
{
    private final Logger _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());

    private final DeviceNode _DeviceNode;

    public HookOpenService(DeviceNode deviceNode) {_DeviceNode = deviceNode;}

    public ICode hookLogic(EchoDo request)
    {
        final RingBuffer<QEvent> _Publisher = _DeviceNode.selectPublisher(OperateType.SERVICE);
        final ReentrantLock _Lock = _DeviceNode.selectLock(OperateType.SERVICE);
        if(_Lock.tryLock()) {
            try {
                long sequence = _Publisher.next();
                try {
                    QEvent event = _Publisher.get(sequence);
                    event.produce(OperateType.SERVICE, Pair.of(request, null), null);
                    return CodeKing.SUCCESS;
                }
                finally {
                    _Publisher.publish(sequence);
                }
            }
            finally {
                _Lock.unlock();
            }
        }
        return CodeKing.LOCKED;
    }

}
