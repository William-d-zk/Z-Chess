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

package com.isahl.chess.pawn.endpoint.device.spi.plugin;

import com.isahl.chess.bishop.protocol.mqtt.command.X113_QttPublish;
import com.isahl.chess.king.base.cron.ScheduleHandler;
import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.cron.features.ICancelable;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.pawn.endpoint.device.api.db.model.MessageEntity;
import com.isahl.chess.pawn.endpoint.device.spi.IHandleHook;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author william.d.zk
 * @date 2021-08-07
 */
@Component
public class LocalQueueHook
        implements IHandleHook,
                   ICancelable
{

    private final TimeWheel        _TimeWheel;
    private final Queue<IProtocol> _MainQueue;
    private final List<ISubscribe> _Subscribes;

    @Autowired
    public LocalQueueHook(TimeWheel timeWheel, List<ISubscribe> subscribes)
    {
        _TimeWheel = timeWheel;
        _Subscribes = subscribes;
        _MainQueue = new ConcurrentLinkedQueue<>();
    }

    @PostConstruct
    void startTimer()
    {
        _TimeWheel.acquire(this, new ScheduleHandler<>(Duration.ofSeconds(5), true, this::batchPush));
    }

    @Override
    public void handle(IProtocol content, List<ITriple> results)
    {
        if(content.serial() == 0x113) {
            X113_QttPublish x113 = (X113_QttPublish) content;
            if(!x113.isDuplicate()) {_MainQueue.offer(x113);}
        }
    }

    private void batchPush(LocalQueueHook self)
    {
        int cachedSize = _MainQueue.size();
        List<IProtocol> cached = new ArrayList<>(cachedSize);
        for(int i = 0; i < cachedSize; i++) {
            cached.add(_MainQueue.poll());
        }
        for(ISubscribe subscribe : _Subscribes) {
            subscribe.onBatch(cached.stream()
                                    .map(subscribe)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList()));
        }
    }

    @Override
    public void cancel()
    {

    }

    public interface ISubscribe
            extends Function<IProtocol, MessageEntity>
    {
        void onBatch(List<MessageEntity> contents);
    }
}
