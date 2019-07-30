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
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.tgx.chess.ZApiExecption;
import com.tgx.chess.bishop.biz.db.dao.DeviceEntry;
import com.tgx.chess.bishop.biz.device.DeviceNode;
import com.tgx.chess.bishop.io.ZSort;
import com.tgx.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.tgx.chess.bishop.io.mqtt.control.X113_QttPublish;
import com.tgx.chess.bishop.io.mqtt.control.X114_QttPuback;
import com.tgx.chess.bishop.io.mqtt.control.X116_QttPubrel;
import com.tgx.chess.bishop.io.mqtt.handler.QttRouter;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.device.X20_SignUp;
import com.tgx.chess.bishop.io.zprotocol.device.X22_SignIn;
import com.tgx.chess.bishop.io.zprotocol.device.X24_UpdateToken;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.CryptUtil;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IProtocol;
import com.tgx.chess.spring.device.model.ClientEntity;
import com.tgx.chess.spring.device.model.DeviceEntity;
import com.tgx.chess.spring.device.repository.ClientRepository;
import com.tgx.chess.spring.device.repository.DeviceRepository;

/**
 * @author william.d.zk
 * @date 2019-06-10
 */
@PropertySource({ "classpath:device.ws.properties",
                  "classpath:device.qtt.properties",
                  "classpath:device.config.properties" })
@Service
public class DeviceService
        implements
        IRepository<DeviceEntry>
{
    private final Logger                                   _Logger                = Logger.getLogger(getClass().getSimpleName());
    private final CryptUtil                                _CryptUtil             = new CryptUtil();
    private final DeviceRepository                         _DeviceRepository;
    private final ClientRepository                         _ClientRepository;
    private final DeviceNode                               _DeviceNode;
    private final Map<Long,
                      Map<Long,
                          IControl<ZContext>>>             _DeviceMessageStateMap = new ConcurrentSkipListMap<>();

    @Value("${invalid.days}")
    private int invalidDurationOfDays;

    @Autowired
    DeviceService(@Value("${qtt.server.host}") String qttServiceHost,
                  @Value("${qtt.server.port}") int qttServicePort,
                  @Value("${ws.server.host}") String wsServiceHost,
                  @Value("${ws.server.port}") int wsServicePort,
                  DeviceRepository deviceRepository,
                  ClientRepository clientRepository)
    {
        _DeviceRepository = deviceRepository;
        _ClientRepository = clientRepository;
        List<ITriple> hosts = new ArrayList<>(2);
        hosts.add(new Triple<>(wsServiceHost, wsServicePort, ZSort.WS_SERVER));
        hosts.add(new Triple<>(qttServiceHost, qttServicePort, ZSort.QTT_SYMMETRY));
        _DeviceNode = new DeviceNode(hosts, this, new QttRouter());
    }

    @PostConstruct
    private void start() throws IOException
    {
        _DeviceNode.start();
    }

    public DeviceEntry convertDevice(DeviceEntity entity)
    {
        Objects.requireNonNull(entity);
        DeviceEntry deviceEntry = new DeviceEntry();
        deviceEntry.setPrimaryKey(entity.getId());
        deviceEntry.setToken(entity.getToken());
        deviceEntry.setInvalidTime(entity.getInvalidAt()
                                         .getTime());
        return deviceEntry;
    }

    private DeviceEntity findDevice(@NonNull DeviceEntity device)
    {
        DeviceEntity exist = _DeviceRepository.findBySn(device.getSn());
        if (Objects.isNull(exist) && !isBlank(device.getImei())) {
            exist = _DeviceRepository.findByImei(device.getImei());
        }
        if (Objects.isNull(exist) && !isBlank(device.getMac())) {
            exist = _DeviceRepository.findByMac(device.getMac());
        }
        return exist;
    }

    public DeviceEntity saveDevice(@NonNull DeviceEntity device)
    {
        if (isBlank(device.getSn()) && isBlank(device.getImei()) && isBlank(device.getMac())) {
            throw new ZApiExecption("unique device info null");
        }
        DeviceEntity exist = findDevice(device);
        if (Objects.nonNull(exist)) {
            device.setId(exist.getId());
            if (!isBlank(exist.getMac())) {
                device.setMac(exist.getMac());
            }
            if (!isBlank(exist.getImei())) {
                device.setImei(exist.getImei());
            }
            if (!isBlank(exist.getImsi()) && isBlank(device.getImsi())) {
                device.setImsi(exist.getImsi());
            }
            if (!isBlank(exist.getPhone()) && isBlank(device.getPhone())) {
                device.setPhone(exist.getPhone());
            }
        }
        if (exist == null || isBlank(exist.getPassword())) {
            device.setPassword(_CryptUtil.randomPassword(5, 32));
            device.increasePasswordId();
            device.setInvalidAt(Date.from(Instant.now()
                                                 .plus(invalidDurationOfDays, ChronoUnit.DAYS)));

        }
        else {
            device.setPassword(exist.getPassword());
            device.setInvalidAt(exist.getInvalidAt());
        }
        device.setToken(_CryptUtil.sha256(String.format("sn:%s,mac:%s,imei:%s",
                                                        device.getSn(),
                                                        device.getMac(),
                                                        device.getImei())));
        return _DeviceRepository.save(device);
    }

    public void updateDevices(Collection<DeviceEntity> devices)
    {
        _DeviceRepository.saveAll(devices);
    }

    public DeviceEntity findDeviceByMac(String mac)
    {
        return _DeviceRepository.findByMac(mac);
    }

    public DeviceEntity findDeviceByImei(String imei)
    {
        return _DeviceRepository.findByImei(imei);
    }

    public DeviceEntity findDeviceByImsi(String imsi)
    {
        return _DeviceRepository.findByImsi(imsi);
    }

    public DeviceEntity findDeviceByToken(String token)
    {
        return _DeviceRepository.findByToken(token);
    }

    public DeviceEntity findDeviceBySn(String sn)
    {
        return _DeviceRepository.findBySn(sn);
    }

    public DeviceEntity findDeviceByPhone(String phone)
    {
        return _DeviceRepository.findByPhone(phone);
    }

    public Set<DeviceEntity> findDevicesByClient(ClientEntity client)
    {
        ClientEntity clientEntity = _ClientRepository.findById(client.getId())
                                                     .orElse(null);
        return clientEntity == null ? null
                                    : clientEntity.getDevices();
    }

    private DeviceEntry auth(DeviceEntity deviceEntity, String password)
    {
        DeviceEntry deviceEntry = new DeviceEntry();
        if (Objects.nonNull(deviceEntity)) {
            _Logger.info("device [%s] | connect ", deviceEntity.getSn());
            deviceEntry.setPrimaryKey(deviceEntity.getId());
            String origin = deviceEntity.getPassword();
            if (isBlank(origin)
                || "*".equals(origin)
                || ".".equals(origin)
                || (Objects.nonNull(password) && origin.equals(password)))
            {
                deviceEntry.setOperation(IStorage.Operation.OP_APPEND);
            }
            else {
                deviceEntry.setOperation(IStorage.Operation.OP_INVALID);
            }
            deviceEntry.setInvalidTime(deviceEntity.getInvalidAt()
                                                   .getTime());
        }
        else {
            deviceEntry.setOperation(IStorage.Operation.OP_NULL);
        }
        return deviceEntry;
    }

    @Override
    public DeviceEntry save(IProtocol target)
    {
        switch (target.getSerial())
        {
            case X20_SignUp.COMMAND:
                X20_SignUp x20 = (X20_SignUp) target;
                byte[] deviceMac = x20.getMac();
                String devicePwd = x20.getPassword();
                long pwdId = x20.getPasswordId();
                DeviceEntity deviceEntity = _DeviceRepository.findByMac(IoUtil.readMac(deviceMac));
                if (Objects.isNull(deviceEntity) || deviceEntity.getPasswordId() == pwdId) {
                    if (Objects.isNull(deviceEntity)) {
                        deviceEntity = new DeviceEntity();
                        deviceEntity.setMac(IoUtil.readMac(deviceMac));
                        deviceEntity.setPasswordId(pwdId);

                    }
                    deviceEntity.setPassword(devicePwd);
                    deviceEntity.setInvalidAt(java.util.Date.from(Instant.now()
                                                                         .plusSeconds(TimeUnit.DAYS.toSeconds(41))));
                    byte[] src = new byte[6 + devicePwd.getBytes().length];
                    IoUtil.write(deviceMac, src, 0);
                    IoUtil.write(devicePwd.getBytes(), src, 6);
                    deviceEntity.setToken(IoUtil.bin2Hex(_CryptUtil.sha256(src)));
                    try {
                        _DeviceRepository.save(deviceEntity);
                        return convertDevice(deviceEntity);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case X24_UpdateToken.COMMAND:
                X24_UpdateToken x24 = (X24_UpdateToken) target;
                break;
            case X113_QttPublish.COMMAND:
                /*
                QOS 1/2 时需要存储 Publish 的状态
                 */
                X113_QttPublish x113 = (X113_QttPublish) target;
                long index = x113.getSession()
                                 .getIndex();
                long identity = x113.getLocalId();
                if (_DeviceMessageStateMap.computeIfPresent(index, (key, old) ->
                {
                    old.put(identity, x113);
                    return old;
                }) == null) {
                    final Map<Long,
                              IControl<ZContext>> _IdentityMessageMap = new HashMap<>(7);
                    _IdentityMessageMap.put(identity, x113);
                    _DeviceMessageStateMap.put(index, _IdentityMessageMap);
                }
                break;
            case X116_QttPubrel.COMMAND:
                /*
                QOS 2时 需要存储 Pubrel 的状态,此时将 Publish 状态清除，
                消息所有权转移到 Publish 接收方
                 */
                X116_QttPubrel x116 = (X116_QttPubrel) target;
                index = x116.getSession()
                            .getIndex();
                identity = x116.getLocalId();
                _DeviceMessageStateMap.computeIfPresent(index, (key, old) ->
                {
                    old.put(identity, x116);
                    return old;
                });
                break;

        }
        return null;
    }

    @Override
    public DeviceEntry find(IProtocol key)
    {
        _Logger.debug("find %d", key.getSerial());
        switch (key.getSerial())
        {
            case X22_SignIn.COMMAND:
                X22_SignIn x22 = (X22_SignIn) key;
                String deviceToken = IoUtil.bin2Hex(x22.getToken());
                String devicePwd = x22.getPassword();
                byte[] password = devicePwd.getBytes();
                byte[] toSign = new byte[password.length + 6];
                IoUtil.write(password, toSign, 6);
                DeviceEntity deviceEntity = _DeviceRepository.findByTokenAndPassword(deviceToken, devicePwd);
                if (Objects.nonNull(deviceEntity)) { return convertDevice(deviceEntity); }
                break;
            case X111_QttConnect.COMMAND:
                X111_QttConnect x111 = (X111_QttConnect) key;
                deviceToken = x111.getClientId();
                deviceEntity = findDeviceByToken(deviceToken);
                return auth(deviceEntity,
                            Objects.nonNull(x111.getPassword()) ? new String(x111.getPassword(), StandardCharsets.UTF_8)
                                                                : null);
            case X114_QttPuback.COMMAND:
                break;
            case X116_QttPubrel.COMMAND:
                /*
                   此时才能将QoS 2的 Publish 消息进行分发。
                 */
                X116_QttPubrel x116 = (X116_QttPubrel) key;
                long sessionIdx = x116.getSession()
                                      .getIndex();
                Map<Long,
                    IControl<ZContext>> storage = _DeviceMessageStateMap.get(sessionIdx);
                long msgId = x116.getLocalId();
                IControl<ZContext> push = storage.remove(msgId);
                DeviceEntry deviceEntry = new DeviceEntry();
                deviceEntry.setPrimaryKey(sessionIdx);
                NavigableMap<Long,
                             IControl<ZContext>> msgQueue = new TreeMap<>();
                msgQueue.put(msgId, push);
                deviceEntry.setMessageQueue(msgQueue);
                return deviceEntry;
        }
        return null;
    }

    @SafeVarargs
    public final void localBizSend(long deviceId, IControl<ZContext>... toSends)
    {
        _DeviceNode.localBizSend(deviceId, toSends);
    }

    public void localBizClose(long deviceId)
    {
        _DeviceNode.localBizClose(deviceId);
    }
}
