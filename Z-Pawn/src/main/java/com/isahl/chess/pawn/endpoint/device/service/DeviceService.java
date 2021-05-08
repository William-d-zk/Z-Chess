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

import com.isahl.chess.bishop.io.mqtt.handler.IQttRouter;
import com.isahl.chess.bishop.io.sort.ZSortHolder;
import com.isahl.chess.bishop.io.ws.zchat.zhandler.ZClusterMappingCustom;
import com.isahl.chess.bishop.io.ws.zchat.zhandler.ZLinkMappingCustom;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.schedule.TimeWheel;
import com.isahl.chess.king.base.util.CryptUtil;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.knight.raft.IRaftDao;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.knight.raft.service.RaftCustom;
import com.isahl.chess.pawn.endpoint.device.DeviceNode;
import com.isahl.chess.pawn.endpoint.device.config.MixConfig;
import com.isahl.chess.pawn.endpoint.device.jpa.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.jpa.repository.IDeviceJpaRepository;
import com.isahl.chess.pawn.endpoint.device.spi.IDeviceService;
import com.isahl.chess.pawn.endpoint.device.spi.IMessageService;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.config.IMixConfig;
import com.isahl.chess.queen.event.handler.mix.ILinkCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.isahl.chess.king.base.util.IoUtil.isBlank;
import static com.isahl.chess.queen.db.inf.IStorage.Operation.OP_INSERT;

/**
 * @author william.d.zk
 * @date 2019-06-10
 */

@Service
public class DeviceService
        implements
        IDeviceService
{
    private final Logger                   _Logger    = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());
    private final MixConfig                _MixConfig;
    private final CryptUtil                _CryptUtil = new CryptUtil();
    private final DeviceNode               _DeviceNode;
    private final ILinkCustom              _LinkCustom;
    private final RaftCustom<DeviceNode>   _RaftCustom;
    private final IDeviceJpaRepository     _DeviceRepository;
    private final RaftNode<DeviceNode>     _RaftNode;
    private final LogicHandler<DeviceNode> _LogicHandler;

    @Autowired
    DeviceService(MixConfig deviceConfig,
                  @Qualifier("pawn_io_config") IAioConfig ioConfig,
                  IRaftConfig raftConfig,
                  IMixConfig mixConfig,
                  ILinkCustom linkCustom,
                  IDeviceJpaRepository deviceRepository,
                  IRaftDao raftDao,
                  IQttRouter qttRouter,
                  IMessageService messageService) throws IOException
    {
        final TimeWheel _TimeWheel = new TimeWheel();
        _MixConfig = deviceConfig;
        List<ITriple> hosts = deviceConfig.getListeners()
                                          .stream()
                                          .map(listener ->
                                          {
                                              ZSortHolder sort = switch (listener.getScheme())
                                              {
                                                  case "mqtt" -> ZSortHolder.QTT_SERVER;
                                                  case "ws-mqtt" -> ZSortHolder.WS_QTT_SERVER;
                                                  case "tls-mqtt" -> ZSortHolder.QTT_SERVER_SSL;
                                                  case "ws-zchat" -> ZSortHolder.WS_ZCHAT_SERVER;
                                                  case "wss-zchat" -> ZSortHolder.WS_ZCHAT_SERVER_SSL;
                                                  case "wss-mqtt" -> ZSortHolder.WS_QTT_SERVER_SSL;
                                                  case "ws-text" -> ZSortHolder.WS_PLAIN_TEXT_SERVER;
                                                  case "wss-text" -> ZSortHolder.WS_PLAIN_TEXT_SERVER_SSL;
                                                  default -> throw new UnsupportedOperationException(listener.getScheme());
                                              };
                                              return new Triple<>(listener.getHost(), listener.getPort(), sort);
                                          })
                                          .collect(Collectors.toList());
        _DeviceNode = new DeviceNode(hosts, deviceConfig.isMultiBind(), ioConfig, raftConfig, mixConfig, _TimeWheel);
        _DeviceRepository = deviceRepository;
        _LinkCustom = linkCustom;
        _RaftNode = new RaftNode<>(_TimeWheel, raftConfig, raftDao, _DeviceNode);
        _RaftCustom = new RaftCustom<>(_RaftNode);
        _LogicHandler = new LogicHandler<>(_DeviceNode, qttRouter, _RaftNode, messageService);
    }

    @PostConstruct
    private void start() throws IOException
    {
        _RaftNode.init();
        _DeviceNode.start(_LogicHandler, new ZLinkMappingCustom(_LinkCustom), new ZClusterMappingCustom<>(_RaftCustom));
        _RaftNode.start();
        _Logger.info("device service start");
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

    /*
    @Override
    public Stream<DeviceEntity> getOnlineDevices(String username) throws ZException
    {
        Collection<ISession> sessions = _DeviceNode.getMappedSessionsWithType(ZUID.TYPE_CONSUMER_SLOT);
        if (sessions == null || sessions.isEmpty()) return null;
        return sessions.stream()
                       .map(session -> _DeviceRepository.findByIdAndUsername(session.getIndex(), username))
                       .filter(Objects::nonNull);
    }
    
    @Override
    public Stream<Pair<DeviceEntity,
                       Map<String,
                           IQoS.Level>>> getOnlineDevicesWithTopic(String username) throws ZException
    {
        Stream<DeviceEntity> onlineDevices = getOnlineDevices(username);
        return onlineDevices != null ? onlineDevices.map(device -> new Pair<>(device, device.getSubscribes())): null;
    }
    */
    @Bean
    public DeviceNode getDeviceNode()
    {
        return _DeviceNode;
    }

    @Bean
    public RaftNode<DeviceNode> getRaftNode()
    {
        return _RaftNode;
    }
}
