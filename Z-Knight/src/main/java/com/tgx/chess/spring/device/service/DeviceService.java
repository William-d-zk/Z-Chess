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

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.NonNull;

import com.tgx.chess.ZApiExecption;
import com.tgx.chess.bishop.biz.db.dao.DeviceEntry;
import com.tgx.chess.king.base.util.CryptUtil;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.io.core.inf.IQoS;
import com.tgx.chess.spring.device.model.ClientEntity;
import com.tgx.chess.spring.device.model.DeviceEntity;
import com.tgx.chess.spring.device.repository.ClientRepository;
import com.tgx.chess.spring.device.repository.DeviceRepository;

/**
 * @author william.d.zk
 * @date 2019-06-10
 */
@PropertySource("classpath:device.config.properties")
public abstract class DeviceService
        implements
        IRepository<DeviceEntry>
{
    private final Random           _Random    = new Random();
    private final CryptUtil        _CryptUtil = new CryptUtil();
    private final DeviceRepository _DeviceRepository;
    private final ClientRepository _ClientRepository;

    @Value("${invalid.days}")
    private int invalidDurationOfDays;

    DeviceService(DeviceRepository deviceRepository,
                  ClientRepository clientRepository)
    {
        _DeviceRepository = deviceRepository;
        _ClientRepository = clientRepository;
    }

    public List<DeviceEntry> findAll()
    {
        List<DeviceEntity> entities = _DeviceRepository.findAll();
        return Objects.nonNull(entities) && !entities.isEmpty() ? entities.stream()
                                                                          .filter(Objects::nonNull)
                                                                          .map(this::convertDevice)
                                                                          .collect(Collectors.toList())
                                                                : null;
    }

    private DeviceEntry convertDevice(DeviceEntity entity)
    {
        Objects.requireNonNull(entity);
        DeviceEntry deviceEntry = new DeviceEntry();
        deviceEntry.setPrimaryKey(entity.getId());
        deviceEntry.setToken(entity.getToken());
        deviceEntry.setInvalidTime(entity.getInvalidAt()
                                         .getTime());
        return deviceEntry;
    }

    private final byte[] _PasswordChars = "qwertyuiopasdfghjklzxcvbnmQAZWSXEDCRFVTGBYHNUJMIKOLP1234567890,-=+_!~`%&*#@;|/".getBytes(StandardCharsets.US_ASCII);

    private DeviceEntity findDevice(@NonNull DeviceEntity device)
    {
        DeviceEntity exist = _DeviceRepository.findBySn(device.getSn());
        if (Objects.isNull(exist)) {
            exist = _DeviceRepository.findByImei(device.getImei());
        }
        if (Objects.isNull(exist)) {
            exist = _DeviceRepository.findByMac(device.getMac());
        }
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
            device.setPasswordId(exist.getPasswordId());
        }
        return device;
    }

    public DeviceEntity saveDevice(@NonNull DeviceEntity device)
    {
        if (isBlank(device.getSn()) && isBlank(device.getImei()) && isBlank(device.getMac())) {
            throw new ZApiExecption("unique device info null");
        }
        device = findDevice(device);
        if (isBlank(device.getPassword())) {
            int passwordLength = _Random.nextInt(27) + 5;
            byte[] pwdBytes = new byte[passwordLength];
            for (int i = 0; i < passwordLength; i++) {
                pwdBytes[i] = _PasswordChars[_Random.nextInt(_PasswordChars.length)];
            }
            device.setPassword(new String(pwdBytes, StandardCharsets.US_ASCII));
            device.increasePasswordId();
            device.setInvalidAt(Date.from(Instant.now()
                                                 .plus(invalidDurationOfDays, ChronoUnit.DAYS)));
        }
        device.setToken(IoUtil.bin2Hex(_CryptUtil.sha256(String.format("sn:%s,mac:%s,imei:%s",
                                                                       device.getSn(),
                                                                       device.getMac(),
                                                                       device.getImei())
                                                               .getBytes(StandardCharsets.US_ASCII))));

        return _DeviceRepository.save(device);
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

    public Set<DeviceEntity> findDevicesByClient(ClientEntity client)
    {
        return _ClientRepository.findByAuth(client.getAuth())
                                .getDevices();
    }

    DeviceEntry auth(DeviceEntity deviceEntity, boolean clean, String password, IQoS.Level level)
    {
        Objects.requireNonNull(level);
        DeviceEntry deviceEntry = new DeviceEntry();
        if (Objects.nonNull(deviceEntity)) {
            deviceEntry.setPrimaryKey(deviceEntity.getId());
            String origin = deviceEntity.getPassword();
            if (isBlank(origin)
                || "*".equals(origin)
                || ".".equals(origin)
                || (Objects.nonNull(password) && origin.equals(password)))
            {
                deviceEntry.setOperation(IStorage.Operation.OP_APPEND);
                deviceEntry.setStrategy(clean ? IStorage.Strategy.CLEAN
                                              : IStorage.Strategy.RETAIN);
                deviceEntry.setQosLevel(level);
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
}
