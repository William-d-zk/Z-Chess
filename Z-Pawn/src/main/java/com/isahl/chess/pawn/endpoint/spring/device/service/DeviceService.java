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

package com.isahl.chess.pawn.endpoint.spring.device.service;

import com.isahl.chess.bishop.io.mqtt.handler.IQttRouter;
import com.isahl.chess.bishop.io.sort.ZSortHolder;
import com.isahl.chess.bishop.io.ws.zchat.zhandler.ZClusterMappingCustom;
import com.isahl.chess.bishop.io.ws.zchat.zhandler.ZLinkMappingCustom;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.schedule.TimeWheel;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.knight.raft.IRaftDao;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.knight.raft.service.ClusterCustom;
import com.isahl.chess.pawn.endpoint.spring.device.DeviceNode;
import com.isahl.chess.pawn.endpoint.spring.device.config.MixConfig;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.MessageBody;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.repository.IDeviceJpaRepository;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.repository.IMessageJpaRepository;
import com.isahl.chess.pawn.endpoint.spring.device.spi.IDeviceService;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.config.IMixConfig;
import com.isahl.chess.queen.event.handler.mix.ILinkCustom;
import com.isahl.chess.queen.io.core.inf.IQoS;
import com.isahl.chess.queen.io.core.inf.ISession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author william.d.zk
 * @date 2019-06-10
 */

@Service
public class DeviceService
        implements
        IDeviceService
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final DeviceNode                _DeviceNode;
    private final ILinkCustom               _LinkCustom;
    private final ClusterCustom<DeviceNode> _ClusterCustom;
    private final IDeviceJpaRepository      _DeviceRepository;
    private final IMessageJpaRepository     _MessageRepository;
    private final RaftNode<DeviceNode>      _RaftNode;
    private final LogicHandler<DeviceNode>  _LogicHandler;

    @Autowired
    DeviceService(MixConfig deviceConfig,
                  @Qualifier("pawn_io_config") IAioConfig ioConfig,
                  IRaftConfig raftConfig,
                  IMixConfig mixConfig,
                  ILinkCustom linkCustom,
                  IMessageJpaRepository messageRepository,
                  IDeviceJpaRepository deviceRepository,
                  IRaftDao raftDao,
                  IQttRouter qttRouter) throws IOException
    {
        final TimeWheel _TimeWheel = new TimeWheel();
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
        _ClusterCustom = new ClusterCustom<>(_RaftNode);
        _LogicHandler = new LogicHandler<>(_DeviceNode, qttRouter, _RaftNode, _MessageRepository = messageRepository);
    }

    @PostConstruct
    private void start() throws IOException
    {
        _RaftNode.init();

        _DeviceNode.start(_LogicHandler,
                          new ZLinkMappingCustom(_LinkCustom),
                          new ZClusterMappingCustom<>(_ClusterCustom));
        _RaftNode.start();
        _Logger.info("device service start");
    }

    @Override
    public DeviceEntity saveDevice(DeviceEntity device) throws ZException
    {
        return _DeviceRepository.save(device);
    }

    @Override
    public DeviceEntity findDevice(DeviceEntity key) throws ZException
    {
        return _DeviceRepository.findBySnOrToken(key.getSn(), key.getToken());
    }

    @Override
    public List<DeviceEntity> findAllDevices() throws ZException
    {
        return _DeviceRepository.findAll();
    }

    @Override
    public MessageBody getMessageById(long id) throws ZException
    {
        return _MessageRepository.getOne(id)
                                 .getBody();
    }

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
        if (onlineDevices != null) { return onlineDevices.map(device -> new Pair<>(device, device.getSubscribes())); }
        return null;
    }

}
