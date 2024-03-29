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
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.pawn.endpoint.device.db.central.model.MessageEntity;
import com.isahl.chess.pawn.endpoint.device.spi.IHandleHook;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author william.d.zk
 * @date 2021-08-07
 */
@Component
public class PersistentHook
        implements IHandleHook,
                   ICancelable
{

    private final TimeWheel        _TimeWheel;
    private final Queue<IoSerial>  _MainQueue;
    private final List<ISubscribe> _Subscribes;

    @Autowired
    public PersistentHook(TimeWheel timeWheel, List<ISubscribe> subscribes)
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
    public void afterLogic(IoSerial content, List<ITriple> results)
    {
        if(content.serial() == 0x113) {
            X113_QttPublish x113 = (X113_QttPublish) content;
            if(!x113.isDuplicate()) {
                String topic = x113.topic();
                byte[] contents = x113.payload();
                MessageEntity msgEntity = new MessageEntity();
                msgEntity.setMessage(contents);
                msgEntity.setTopic(topic);
                msgEntity.setNetAt(LocalDateTime.now());
                msgEntity.setOrigin(x113.session()
                                        .index());
                _MainQueue.offer(msgEntity);
            }
        }
        else {
            throw new IllegalStateException("Unexpected value: 0x" + Integer.toHexString(content.serial()));
        }
    }

    @Override
    public void afterConsume(IoSerial content)
    {
        if(content instanceof MessageEntity msg) {
            _MainQueue.offer(msg);
        }
    }

    private void batchPush(PersistentHook self)
    {
        int size = _MainQueue.size();
        List<IoSerial> cached = new ArrayList<>(size);
        for(int i = 0; i < size; i++) {
            cached.add(_MainQueue.poll());
        }
        for(ISubscribe subscribe : _Subscribes) {
            subscribe.onBatch(cached);
        }
    }

    @Override
    public void cancel()
    {

    }

    public interface ISubscribe
    {
        void onBatch(List<IoSerial> contents);
    }

    @Override
    public boolean isExpect(IoSerial content)
    {
        return switch(content.serial()){
            case 0x113, 0x1D -> true;
            default -> false;
        };
    }
}
