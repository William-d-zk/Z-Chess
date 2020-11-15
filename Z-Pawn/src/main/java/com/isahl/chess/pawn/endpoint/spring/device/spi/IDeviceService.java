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

package com.isahl.chess.pawn.endpoint.spring.device.spi;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.MessageBody;
import com.isahl.chess.queen.io.core.inf.IQoS;

public interface IDeviceService
{
    DeviceEntity saveDevice(DeviceEntity device) throws ZException;

    DeviceEntity findDevice(DeviceEntity key) throws ZException;

    MessageBody getMessageById(long id) throws ZException;

    List<DeviceEntity> findAllDevices() throws ZException;

    Stream<DeviceEntity> getOnlineDevices(String username) throws ZException;

    Stream<Pair<DeviceEntity,
                Map<String,
                    IQoS.Level>>> getOnlineDevicesWithTopic(String username) throws ZException;
}
