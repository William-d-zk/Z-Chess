/*
 * MIT License
 *
 * Copyright (c) 2016~2022. Z-Chess
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

package com.isahl.chess.pawn.endpoint.device.resource.service;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.pawn.endpoint.device.config.MixConfig;
import com.isahl.chess.pawn.endpoint.device.db.central.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.db.central.repository.IDeviceRepository;
import com.isahl.chess.pawn.endpoint.device.resource.features.IDeviceService;
import com.isahl.chess.rook.storage.cache.config.EhcacheConfig;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.cache.CacheManager;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.isahl.chess.king.base.util.IoUtil.isBlank;
import static com.isahl.chess.queen.db.model.IStorage.Operation.OP_INSERT;
import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * @author william.d.zk
 * {@code @date} 2020-09-09
 */
@Service
public class DeviceService
        implements IDeviceService
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final IDeviceRepository _DeviceRepository;
    private final CacheManager      _CacheManager;
    private final CryptoUtil        _CryptoUtil = new CryptoUtil();
    private final MixConfig         _MixConfig;

    @Autowired
    public DeviceService(IDeviceRepository deviceRepository, CacheManager cacheManager, MixConfig mixConfig)
    {
        _DeviceRepository = deviceRepository;
        _CacheManager = cacheManager;
        _MixConfig = mixConfig;
    }

    @PostConstruct
    public void init() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        EhcacheConfig.createCache(_CacheManager,
                                  "device_token_cache",
                                  String.class,
                                  DeviceEntity.class,
                                  Duration.of(20, MINUTES));
        EhcacheConfig.createCache(_CacheManager,
                                  "device_id_cache",
                                  Long.class,
                                  DeviceEntity.class,
                                  Duration.of(15, MINUTES));
    }

    @CachePut(value = "device_token_cache",
              key = "#p0.token",
              condition = "#result != null")
    public DeviceEntity saveDevice(DeviceEntity device)
    {
        return _DeviceRepository.save(device);
    }

    @Override
    public DeviceEntity upsertDevice(DeviceEntity device) throws ZException
    {
        if(device.operation()
                 .getValue() > OP_INSERT.getValue())
        {   // update
            DeviceEntity exist;
            Optional<DeviceEntity> result = _DeviceRepository.findById(device.primaryKey());
            exist = result.orElseThrow(()->new ZException("entity_not_found_exception %s → %#x",
                                                          device.operation()
                                                                .name(),
                                                          device.primaryKey()));
            if(exist.getInvalidAt()
                    .isBefore(LocalDateTime.now()) || device.getPasswordId() > exist.getPasswordId())
            {
                exist.setPassword(_CryptoUtil.randomPassword(17, 32, true));
                exist.increasePasswordId();
                exist.setInvalidAt(LocalDateTime.now()
                                                .plus(_MixConfig.getPasswordInvalidDays()));
                exist.setUpdatedById(device.getUpdatedById());
                exist.setUpdatedAt(device.getUpdatedAt());
            }
            return saveDevice(exist);
        }
        else {
            DeviceEntity exist = null;
            if(!isBlank(device.getNotice())) {
                exist = _DeviceRepository.findByNumber(device.getNotice());
            }
            else if(!isBlank(device.getToken())) {
                exist = _DeviceRepository.findByToken(device.getToken());
            }
            DeviceEntity entity = exist == null ? device : exist;
            if(exist == null) {
                String source = String.format("sn:%s,random %s%d",
                                              device.getNotice(),
                                              _MixConfig.getPasswordRandomSeed(),
                                              Instant.now()
                                                     .toEpochMilli());
                _Logger.debug("new device %s ", source);
                entity.setToken(IoUtil.bin2Hex(_CryptoUtil.sha256(source.getBytes(StandardCharsets.UTF_8))));
                entity.setUpdatedAt(LocalDateTime.now());
            }
            if(exist == null || exist.getInvalidAt()
                                     .isBefore(LocalDateTime.now()))
            {
                entity.setPassword(_CryptoUtil.randomPassword(17, 32, true));
                entity.increasePasswordId();
                entity.setInvalidAt(LocalDateTime.now()
                                                 .plus(_MixConfig.getPasswordInvalidDays()));
                entity.setUpdatedAt(device.getUpdatedAt());
                entity.setUpdatedById(device.getUpdatedById());
            }
            return saveDevice(entity);
        }
    }

    @Caching(cacheable = { @Cacheable(value = "device_token_cache",
                                      key = "#p0",
                                      condition = "#p0 != null",
                                      unless = "#result == null") },
             put = { @CachePut(value = "device_id_cache",
                               key = "#result.id",
                               condition = "#result != null") })
    @Override
    public DeviceEntity findByToken(String token) throws ZException
    {
        return _DeviceRepository.findByToken(token);
    }

    @Override
    public DeviceEntity findByNumber(String number) throws ZException
    {
        return _DeviceRepository.findByNumber(number);
    }

    @Override
    public List<DeviceEntity> findDevices(Specification<DeviceEntity> condition, Pageable pageable) throws ZException
    {
        return _DeviceRepository.findAll(condition, pageable)
                                .toList();
    }

    @Cacheable(value = "device_id_cache",
               key = "#p0",
               condition = "#p0 != 0",
               unless = "#result == null")
    @Override
    public DeviceEntity getOneDevice(long id)
    {
        return _DeviceRepository.findById(id)
                                .orElse(null);
    }

    @Override
    public List<DeviceEntity> findDevicesIn(List<Long> deviceIdList)
    {
        return _DeviceRepository.findAllById(deviceIdList);
    }
}
