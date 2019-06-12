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

import static com.tgx.chess.king.base.util.IoUtil.isBlank;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import com.tgx.chess.bishop.biz.db.dao.DeviceEntry;
import com.tgx.chess.bishop.biz.device.QttNode;
import com.tgx.chess.bishop.io.mqtt.bean.QttContext;
import com.tgx.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.CryptUtil;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IProtocol;
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
        extends
        DeviceService
{
    private final QttNode _QttNode;

    private final Random    _Random    = new Random();
    private final CryptUtil _CryptUtil = new CryptUtil();

    private final Logger _Logger = Logger.getLogger(getClass().getSimpleName());

    @Autowired
    public QttService(DeviceRepository deviceRepository,
                      ClientRepository clientRepository,
                      @Value("${qtt.server.host}") String qttHost,
                      @Value("${qtt.server.port}") int qttPort)
    {
        super(deviceRepository, clientRepository);
        _QttNode = new QttNode(qttHost, qttPort, this);
    }

    @PostConstruct
    private void start() throws IOException
    {
        _QttNode.start();
    }

    @Override
    public DeviceEntry save(IProtocol tar)
    {
        switch (tar.getSerial())
        {
            case X111_QttConnect.COMMAND:
                X111_QttConnect x111 = (X111_QttConnect) tar;
                String deviceSn = x111.getClientId();
                DeviceEntity deviceEntity = findDeviceBySn(deviceSn);
                DeviceEntry deviceEntry = new DeviceEntry();
                if (Objects.nonNull(deviceEntity)) {
                    String password = deviceEntity.getPassword();
                    deviceEntry.setPrimaryKey(deviceEntity.getId());
                    if (isBlank(password)
                        || "*".equals(password)
                        || ".".equals(password)
                        || (Objects.nonNull(x111.getPassword())
                            && password.equals(new String(x111.getPassword(), StandardCharsets.UTF_8))))
                    {
                        _Logger.info("auth ok");
                        if (x111.isCleanSession()) {
                            deviceEntry.setOperation(IStorage.Operation.OP_RESET);
                        }
                        else {
                            switch (x111.getQosLevel())
                            {
                                case QOS_ONLY_ONCE:
                                    deviceEntry.setOperation(IStorage.Operation.OP_MODIFY);
                                    break;
                                case QOS_AT_LEAST_ONCE:
                                    deviceEntry.setOperation(IStorage.Operation.OP_RETRY);
                                case QOS_LESS_ONCE:
                            }

                        }
                    }
                    else {
                        deviceEntry.setOperation(IStorage.Operation.OP_INVALID);
                    }
                    deviceEntry.setInvalidTime(deviceEntity.getInvalidAt()
                                                           .getTime());
                }
                else {
                    deviceEntity = new DeviceEntity();
                    deviceEntity.setSn(deviceSn);
                    deviceEntity.setPassword(new String(x111.getPassword(), StandardCharsets.UTF_8));
                    deviceEntity.setInvalidAt(Date.from(Instant.now()
                                                               .plusSeconds(TimeUnit.DAYS.toSeconds(41))));
                    saveDevice(deviceEntity);
                    deviceEntry.setOperation(IStorage.Operation.OP_INSERT);
                    deviceEntry.setToken(deviceSn);
                    deviceEntry.setInvalidTime(deviceEntity.getInvalidAt()
                                                           .getTime());
                }
                return deviceEntry;
        }
        return null;
    }

    @Override
    public DeviceEntry find(IProtocol key)
    {
        switch (key.getSerial())
        {

        }
        return null;
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
