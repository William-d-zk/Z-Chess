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

package com.isahl.chess.pawn.endpoint.device.service;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.CryptUtil;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.pawn.endpoint.device.config.MixConfig;
import com.isahl.chess.pawn.endpoint.device.jpa.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.jpa.model.DeviceSubscribe;
import com.isahl.chess.pawn.endpoint.device.jpa.model.MessageEntity;
import com.isahl.chess.pawn.endpoint.device.jpa.repository.IDeviceJpaRepository;
import com.isahl.chess.pawn.endpoint.device.jpa.repository.IMessageJpaRepository;
import com.isahl.chess.pawn.endpoint.device.model.ShadowDevice;
import com.isahl.chess.pawn.endpoint.device.spi.IDeviceService;
import com.isahl.chess.queen.io.core.inf.IQoS;
import com.isahl.chess.rook.storage.cache.config.EhcacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.cache.CacheManager;
import javax.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.isahl.chess.bishop.io.Direction.OWNER_SERVER;
import static com.isahl.chess.king.base.util.IoUtil.isBlank;
import static com.isahl.chess.queen.db.inf.IStorage.Operation.OP_INSERT;
import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * @author william.d.zk
 * 
 * @date 2020-09-09
 */
@Service
public class DeviceService
        implements
        IDeviceService
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final IMessageJpaRepository   _MessageJpaRepository;
    private final IDeviceJpaRepository    _DeviceRepository;
    private final CacheManager            _CacheManager;
    private final CryptUtil               _CryptUtil      = new CryptUtil();
    private final MixConfig               _MixConfig;
    private final Map<Long,
                      ShadowDevice>       _ShawdowDevices = new HashMap<>(1 << 10);

    public DeviceService(IMessageJpaRepository messageJpaRepository,
                         IDeviceJpaRepository deviceRepository,
                         CacheManager cacheManager,
                         MixConfig mixConfig)
    {
        _MessageJpaRepository = messageJpaRepository;
        _DeviceRepository = deviceRepository;
        _CacheManager = cacheManager;
        _MixConfig = mixConfig;
    }

    @PostConstruct
    void initService() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        EhcacheConfig.createCache(_CacheManager,
                                  "device_cache",
                                  String.class,
                                  DeviceEntity.class,
                                  Duration.of(20, MINUTES));
    }

    public void saveMessageState(MessageEntity message, long session)
    {

    }

    public void clean(long deviceId)
    {
        _MessageJpaRepository.deleteAllByDestination(deviceId, OWNER_SERVER);
        _DeviceRepository.findById(deviceId)
                         .ifPresent(device ->
                         {
                             DeviceSubscribe subscribe = device.getSubscribe();
                             if (subscribe != null) {
                                 subscribe.clean();
                             }
                             saveDevice(device);
                         });
    }

    public void loadHistory(long session)
    {
        List<MessageEntity> myHistory = _MessageJpaRepository.findAllByDestinationAndOwner(session, OWNER_SERVER);

    }

    @Cacheable(value = "device_cache", key = "#token")
    public DeviceEntity findDeviceByToken(String token)
    {
        return _DeviceRepository.findByToken(token);
    }

    public void subscribe(Map<String,
                              IQoS.Level> subscribes,
                          long deviceId,
                          Function<Optional<DeviceEntity>,
                                   BiConsumer<String,
                                              IQoS.Level>> function)
    {
        Optional<DeviceEntity> deviceOptional = _DeviceRepository.findById(deviceId);
        subscribes.forEach(function.apply(deviceOptional));
        deviceOptional.ifPresent(this::saveDevice);
    }

    public void unsubscribe(List<String> topics,
                            long deviceId,
                            Function<Optional<DeviceEntity>,
                                     Consumer<String>> function)
    {
        Optional<DeviceEntity> deviceOptional = _DeviceRepository.findById(deviceId);
        topics.forEach(function.apply(deviceOptional));
        deviceOptional.ifPresent(this::saveDevice);
    }

    @CachePut(value = "device_cache", key = "#device.token")
    public DeviceEntity saveDevice(DeviceEntity device)
    {
        return _DeviceRepository.save(device);
    }

    @Override
    public DeviceEntity upsertDevice(DeviceEntity device) throws ZException
    {
        if (device.operation()
                  .getValue() > OP_INSERT.getValue())
        {   // update
            DeviceEntity exist;
            try {
                exist = _DeviceRepository.getOne(device.primaryKey());
            }
            catch (EntityNotFoundException e) {
                _Logger.warning("entity_not_found_exception", e);
                throw new ZException(e,
                                     device.operation()
                                           .name());
            }
            if (exist.getInvalidAt()
                     .isBefore(LocalDateTime.now())
                || device.getPasswordId() > exist.getPasswordId())
            {
                exist.setPassword(_CryptUtil.randomPassword(17, 32));
                exist.increasePasswordId();
                exist.setInvalidAt(LocalDateTime.now()
                                                .plus(_MixConfig.getPasswordInvalidDays()));
            }
            return saveDevice(exist);
        }
        else {
            DeviceEntity exist = null;
            if (!isBlank(device.getSn())) {
                exist = _DeviceRepository.findBySn(device.getSn());
            }
            else if (!isBlank(device.getToken())) {
                exist = _DeviceRepository.findByToken(device.getToken());
            }
            DeviceEntity entity = exist == null ? new DeviceEntity(): exist;
            if (exist == null) {
                String source = String.format("sn:%s,random %s%d",
                                              device.getSn(),
                                              _MixConfig.getPasswordRandomSeed(),
                                              Instant.now()
                                                     .toEpochMilli());
                _Logger.debug("new device %s ", source);
                entity.setToken(IoUtil.bin2Hex(_CryptUtil.sha256(source.getBytes(StandardCharsets.UTF_8))));
                entity.setSn(device.getSn());
                entity.setUsername(device.getUsername());
                entity.setSubscribe(device.getSubscribe());
                entity.setProfile(device.getProfile());
            }
            if (exist == null
                || exist.getInvalidAt()
                        .isBefore(LocalDateTime.now()))
            {
                entity.setPassword(_CryptUtil.randomPassword(17, 32));
                entity.increasePasswordId();
                entity.setInvalidAt(LocalDateTime.now()
                                                 .plus(_MixConfig.getPasswordInvalidDays()));
            }
            return saveDevice(entity);
        }
    }

    @Override
    @Cacheable(value = "device_cache", key = "#token", unless = "#token==null")
    public DeviceEntity queryDevice(String sn, String token) throws ZException
    {
        return _DeviceRepository.findBySnOrToken(sn, token);
    }

    @Override
    public List<DeviceEntity> findDevices(Specification<DeviceEntity> condition, Pageable pageable) throws ZException
    {
        return _DeviceRepository.findAll(condition, pageable)
                                .toList();
    }

    @Override
    public DeviceEntity getOneDevice(long id)
    {
        return _DeviceRepository.getOne(id);
    }

    @Override
    public List<ShadowDevice> getOnlineDevicesByUsername(String username)
    {
        return null;
    }

    @Override
    public List<ShadowDevice> getOnlineDevicesByTopic(String topic)
    {
        return null;
    }

    @Override
    public List<ShadowDevice> getOnlineDevices()
    {
        //        Collection<ISession> sessions = _DeviceNode.getMappedSessionsWithType(TYPE_CONSUMER_SLOT);
        return null;
    }

}
