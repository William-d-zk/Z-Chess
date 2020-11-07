/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.pawn.endpoint.spring.device.service;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.isahl.chess.bishop.io.mqtt.handler.IQttDurable;
import com.isahl.chess.bishop.io.mqtt.handler.QttRouter;
import com.isahl.chess.king.base.schedule.Status;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.DeviceSubscribe;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.MessageEntity;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.repository.IDeviceJpaRepository;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.repository.IMessageJpaRepository;
import com.isahl.chess.queen.io.core.inf.IQoS;

/**
 * @author william.d.zk
 * 
 * @date 2020-09-09
 */
@Service
public class DurableService
        implements
        IQttDurable
{
    private final IMessageJpaRepository _MessageJpaRepository;
    private final IDeviceJpaRepository  _DeviceJpaRepository;

    public DurableService(IMessageJpaRepository messageJpaRepository,
                          IDeviceJpaRepository deviceJpaRepository)
    {
        _MessageJpaRepository = messageJpaRepository;
        _DeviceJpaRepository = deviceJpaRepository;
    }

    public void saveMessageState(MessageEntity message, long session)
    {

    }

    @Override
    public void upsertState(long msgId, Status status, long session)
    {

    }

    @Override
    public Map<String,
               IQoS.Level> loadSubscribe(long session)
    {
        DeviceEntity device = _DeviceJpaRepository.findById(session)
                                                  .orElse(null);
        if (device != null) {
            DeviceSubscribe subscribe = device.getSubscribe();
            if (subscribe != null) { return subscribe.getSubscribes(); }
        }
        return null;
    }

    @Override
    public void cleanState(long session)
    {
        DeviceEntity device = _DeviceJpaRepository.findById(session)
                                                  .orElse(null);
        if (device != null) {
            DeviceSubscribe subscribe = device.getSubscribe();
            if (subscribe != null) {
                subscribe.clean();
            }
            _DeviceJpaRepository.save(device);
        }
    }

    @Bean
    public QttRouter getQttRouter()
    {
        return new QttRouter(this);
    }
}
