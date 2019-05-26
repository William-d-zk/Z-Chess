/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.spring.device.service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import com.tgx.chess.bishop.biz.db.dao.DeviceEntry;
import com.tgx.chess.bishop.biz.device.QttNode;
import com.tgx.chess.bishop.io.mqtt.bean.QttContext;
import com.tgx.chess.king.base.util.CryptUtil;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.spring.device.model.DeviceEntity;
import com.tgx.chess.spring.device.repository.ClientRepository;
import com.tgx.chess.spring.device.repository.DeviceRepository;

/**
 * @author william.d.zk
 * @date 2019-05-26
 */
@Service
@PropertySource("classpath:device.qtt.properties")
public class QttService
        implements
        IRepository<DeviceEntry,
                    QttContext>
{
    private final DeviceRepository _DeviceRepository;
    private final ClientRepository _ClientRepository;
    private final QttNode          _QttNode;

    private final Random    _Random    = new Random();
    private final CryptUtil _CryptUtil = new CryptUtil();

    @Autowired
    public QttService(DeviceRepository deviceRepository,
                      ClientRepository clientRepository,
                      @Value("${qtt.server.host}") String qttHost,
                      @Value("${qtt.server.port}") int qttPort)
    {
        _DeviceRepository = deviceRepository;
        _ClientRepository = clientRepository;
        _QttNode = new QttNode(qttHost, qttPort, this);
    }

    @PostConstruct
    private void start() throws IOException
    {
        _QttNode.start();
    }

    public List<DeviceEntry> findAll()
    {
        List<DeviceEntity> entities = _DeviceRepository.findAll();
        return Objects.nonNull(entities) ? entities.stream()
                                                   .map(this::convertDevice)
                                                   .peek(entry -> entry.setOnline(Objects.nonNull(_QttNode.findSessionByIndex(entry.getDeviceUID()))))
                                                   .collect(Collectors.toList())
                                         : null;
    }

    public DeviceEntity saveDevice(DeviceEntity device)
    {
        return _DeviceRepository.save(device);
    }

    public DeviceEntity findDeviceByMac(String mac)
    {
        return _DeviceRepository.findByMac(mac);
    }

    public DeviceEntity findDeviceByToken(String token)
    {
        return _DeviceRepository.findByToken(token);
    }

    @Override
    public DeviceEntry save(IControl tar)
    {
        switch (tar.getSerial())
        {

        }
        return null;
    }

    @Override
    public DeviceEntry find(IControl key)
    {
        switch (key.getSerial())
        {

        }
        return null;
    }

    private DeviceEntry convertDevice(DeviceEntity entity)
    {
        DeviceEntry deviceEntry = new DeviceEntry();
        deviceEntry.setDeviceUID(entity.getId());
        deviceEntry.setMac(entity.getMac());
        deviceEntry.setToken(entity.getToken());
        deviceEntry.setInvalidTime(entity.getInvalidAt()
                                         .getTime());
        return deviceEntry;
    }

    @SafeVarargs
    public final void localBizSend(long deviceId, IControl<QttContext>... toSends)
    {
        _QttNode.localBizSend(deviceId, toSends);
    }

    public void localBizClose(long deviceId)
    {
        _QttNode.localBizClose(deviceId);
    }

}
