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

package com.isahl.chess.pawn.endpoint.device.api.service;

import com.isahl.chess.king.base.cron.ScheduleHandler;
import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.knight.raft.model.replicate.LogEntry;
import com.isahl.chess.pawn.endpoint.device.api.features.IDeviceService;
import com.isahl.chess.pawn.endpoint.device.api.db.model.ShadowEntity;
import com.isahl.chess.pawn.endpoint.device.config.MixConfig;
import com.isahl.chess.pawn.endpoint.device.db.remote.postgres.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.db.remote.postgres.repository.IDeviceJpaRepository;
import com.isahl.chess.rook.storage.cache.config.EhcacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.cache.CacheManager;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.isahl.chess.king.base.cron.TimeWheel.IWheelItem.PRIORITY_NORMAL;
import static com.isahl.chess.king.base.util.IoUtil.isBlank;
import static com.isahl.chess.queen.db.model.IStorage.Operation.OP_INSERT;
import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * @author william.d.zk
 * @date 2020-09-09
 */
@Service
public class DeviceService
        implements IDeviceService
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final IDeviceJpaRepository _DeviceJpaRepository;
    private final CacheManager         _CacheManager;
    private final CryptoUtil           _CryptoUtil       = new CryptoUtil();
    private final MixConfig            _MixConfig;
    private final TimeWheel            _TimeWheel;
    private final ShadowBatch          _BatchHandleLogin = new ShadowBatch();
    private final ShadowBatch          _BatchHandleIdle  = new ShadowBatch();

    private static class ShadowBatch
            extends ConcurrentLinkedQueue<ShadowEntity>
            implements IValid
    {}

    @Autowired
    public DeviceService(IDeviceJpaRepository deviceRepository,
                         CacheManager cacheManager,
                         MixConfig mixConfig,
                         TimeWheel timeWheel)
    {
        _DeviceJpaRepository = deviceRepository;
        _CacheManager = cacheManager;
        _MixConfig = mixConfig;
        _TimeWheel = timeWheel;
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
        EhcacheConfig.createCache(_CacheManager, "raft_log_entry", Long.class, LogEntry.class, Duration.of(5, MINUTES));
        _TimeWheel.acquire(_BatchHandleLogin,
                           new ScheduleHandler<>(Duration.ofSeconds(10),
                                                 true,
                                                 this::batchHandleLogin,
                                                 PRIORITY_NORMAL));
        _TimeWheel.acquire(_BatchHandleIdle,
                           new ScheduleHandler<>(Duration.ofSeconds(20), true, this::batchHandleIdle, PRIORITY_NORMAL));
    }

    @Cacheable(value = "device_id_cache",
               key = "#session",
               unless = "#session == 0 || #result == null")
    public DeviceEntity findDeviceById(long session)
    {
        return _DeviceJpaRepository.findById(session)
                                   .orElse(null);
    }

    @Cacheable(value = "device_token_cache",
               key = "#token",
               unless = "#result == null")
    public DeviceEntity findDeviceByToken(String token)
    {
        return _DeviceJpaRepository.findByToken(token);
    }

    @CachePut(value = "device_token_cache",
              key = "#device.token")
    public DeviceEntity saveDevice(DeviceEntity device)
    {
        return _DeviceJpaRepository.save(device);
    }

    @Override
    public DeviceEntity upsertDevice(DeviceEntity device) throws ZException
    {
        if(device.operation()
                 .getValue() > OP_INSERT.getValue())
        {   // update
            DeviceEntity exist;
            Optional<DeviceEntity> result = _DeviceJpaRepository.findById(device.primaryKey());
            exist = result.orElseThrow(()->new ZException("entity_not_found_exception %s → %#x",
                                                          device.operation()
                                                                .name(),
                                                          device.primaryKey()));
            if(exist.getInvalidAt()
                    .isBefore(LocalDateTime.now()) || device.getPasswordId() > exist.getPasswordId())
            {
                exist.setPassword(_CryptoUtil.randomPassword(17, 32));
                exist.increasePasswordId();
                exist.setInvalidAt(LocalDateTime.now()
                                                .plus(_MixConfig.getPasswordInvalidDays()));
            }
            return saveDevice(exist);
        }
        else {
            DeviceEntity exist = null;
            if(!isBlank(device.getSn())) {
                exist = _DeviceJpaRepository.findBySn(device.getSn());
            }
            else if(!isBlank(device.getToken())) {
                exist = _DeviceJpaRepository.findByToken(device.getToken());
            }
            DeviceEntity entity = exist == null ? new DeviceEntity() : exist;
            if(exist == null) {
                String source = String.format("sn:%s,random %s%d",
                                              device.getSn(),
                                              _MixConfig.getPasswordRandomSeed(),
                                              Instant.now()
                                                     .toEpochMilli());
                _Logger.debug("new device %s ", source);
                entity.setToken(IoUtil.bin2Hex(_CryptoUtil.sha256(source.getBytes(StandardCharsets.UTF_8))));
                entity.setSn(device.getSn());
                entity.setUsername(device.getUsername());
                entity.setProfile(device.getProfile());
            }
            if(exist == null || exist.getInvalidAt()
                                     .isBefore(LocalDateTime.now()))
            {
                entity.setPassword(_CryptoUtil.randomPassword(17, 32));
                entity.increasePasswordId();
                entity.setInvalidAt(LocalDateTime.now()
                                                 .plus(_MixConfig.getPasswordInvalidDays()));
            }
            return saveDevice(entity);
        }
    }

    @Cacheable(value = "device_token_cache",
               key = "#token",
               unless = "#token == null",
               condition = "#result != null")
    @Override
    public DeviceEntity findByToken(String token) throws ZException
    {
        return _DeviceJpaRepository.findByToken(token);
    }

    @Override
    public DeviceEntity findBySn(String sn) throws ZException
    {
        return _DeviceJpaRepository.findBySn(sn);
    }

    @Override
    public List<DeviceEntity> findDevices(Specification<DeviceEntity> condition, Pageable pageable) throws ZException
    {
        return _DeviceJpaRepository.findAll(condition, pageable)
                                   .toList();
    }

    @Override
    public DeviceEntity getOneDevice(long id)
    {
        return _DeviceJpaRepository.findById(id)
                                   .orElse(null);
    }

    @Override
    public final List<ShadowEntity> getOnlineDevices(Specification<ShadowEntity> specification,
                                                     Pageable pageable) throws ZException
    {
        return null;
    }

    @Override
    public List<DeviceEntity> findDevicesIn(List<Long> deviceIdList)
    {
        return _DeviceJpaRepository.findAllById(deviceIdList);
    }

    private void batchHandleLogin(ShadowBatch batch)
    {
        if(batch.isEmpty()) {return;}
        _Logger.info("batch login: %d", batch.size());
        try {
            for(Iterator<ShadowEntity> it = batch.iterator(); it.hasNext(); ) {
                ShadowEntity shadow = it.next();
                it.remove();
                /*
                ShadowEntity exist = _ShadowJpaRepository.findByDeviceId(shadow.getDeviceId());
                if(exist != null) {
                    shadow.setShadowId(exist.getShadowId());
                }
                _ShadowJpaRepository.save(shadow);
                */
            }
        }
        catch(Exception e) {
            _Logger.warning(e);
        }
    }

    private void batchHandleIdle(ShadowBatch batch)
    {
        if(batch.isEmpty()) {return;}
        _Logger.info("batch handle idle: %d", batch.size());
        try {
            for(Iterator<ShadowEntity> it = batch.iterator(); it.hasNext(); ) {
                ShadowEntity shadow = it.next();
                it.remove();
                //  _ShadowJpaRepository.deleteByDevice(shadow.getDeviceId());
            }
        }
        catch(Exception e) {
            _Logger.warning(e);
        }
    }
}
