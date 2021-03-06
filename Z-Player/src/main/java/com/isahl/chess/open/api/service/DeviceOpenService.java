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

package com.isahl.chess.open.api.service;

import static com.isahl.chess.king.base.util.IoUtil.isBlank;
import static com.isahl.chess.queen.db.inf.IStorage.Operation.OP_INSERT;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.CryptUtil;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.pawn.endpoint.spring.device.config.DeviceConfig;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.repository.IDeviceJpaRepository;
import com.isahl.chess.pawn.endpoint.spring.device.spi.IDeviceService;

/**
 * @author william.d.zk
 */
@Service
public class DeviceOpenService
{
    private final IDeviceJpaRepository _JpaRepository;
    private final DeviceConfig         _DeviceConfig;
    private final CryptUtil            _CryptUtil = new CryptUtil();
    private final Logger               _Logger    = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());
    private final IDeviceService       _DeviceService;

    @Autowired
    public DeviceOpenService(IDeviceJpaRepository jpaRepository,
                             DeviceConfig deviceConfig,
                             IDeviceService deviceService)
    {
        _JpaRepository = jpaRepository;
        _DeviceConfig = deviceConfig;
        _DeviceService = deviceService;
    }

    public DeviceEntity save(DeviceEntity device) throws ZException
    {
        if (device.operation()
                  .getValue() > OP_INSERT.getValue())
        {   // update 
            DeviceEntity exist;
            try {
                exist = _JpaRepository.getOne(device.primaryKey());
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
                                                .plus(_DeviceConfig.getPasswordInvalidDays()));
            }
            return _JpaRepository.save(exist);
        }
        else {
            DeviceEntity exist = null;
            if (!isBlank(device.getSn())) {
                exist = _JpaRepository.findBySn(device.getSn());
            }
            else if (!isBlank(device.getToken())) {
                exist = _JpaRepository.findByToken(device.getToken());
            }
            DeviceEntity entity = exist == null ? new DeviceEntity(): exist;
            if (exist == null) {
                String source = String.format("sn:%s,random %s%d",
                                              device.getSn(),
                                              _DeviceConfig.getPasswordRandomSeed(),
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
                                                 .plus(_DeviceConfig.getPasswordInvalidDays()));
            }
            return _JpaRepository.save(entity);
        }
    }

    public DeviceEntity find(DeviceEntity device) throws ZException
    {
        DeviceEntity exist = _JpaRepository.findBySnOrToken(device.getSn(), device.getToken());
        if (exist == null) {
            exist = _JpaRepository.getOne(device.getId());
        }
        return exist;
    }

    public Stream<DeviceEntity> filterOnlineDevices(String username)
    {
        if (isBlank(username)) return null;
        return _DeviceService.getOnlineDevices(username);
    }

    @Cacheable(value = "onlineOfUser", key = "#username")
    public long countOnlineDevices(String username)
    {
        if (isBlank(username)) return 0;
        return _DeviceService.getOnlineDevices(username)
                             .count();
    }

    public Page<DeviceEntity> findAll(String sn, Pageable pageable)
    {
        return _JpaRepository.findAll((Specification<DeviceEntity>) (root, criteriaQuery, criteriaBuilder) ->
        {
            List<Predicate> predicates = new ArrayList<>();
            if (!IoUtil.isBlank(sn)) {
                predicates.add(criteriaBuilder.like(root.get("sn"), "%" + sn + "%"));
            }
            return criteriaQuery.where(predicates.toArray(new Predicate[0]))
                                .getRestriction();
        }, pageable);
    }
}