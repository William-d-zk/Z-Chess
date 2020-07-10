/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.tgx.chess.pawn.endpoint.spring.device.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tgx.chess.bishop.io.ZSort;
import com.tgx.chess.bishop.io.mqtt.handler.IQttRouter;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zhandler.ZClusterMappingCustom;
import com.tgx.chess.bishop.io.zhandler.ZLinkMappingCustom;
import com.tgx.chess.king.base.exception.ZException;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.king.topology.ZUID;
import com.tgx.chess.knight.raft.IRaftDao;
import com.tgx.chess.knight.raft.config.IRaftConfig;
import com.tgx.chess.knight.raft.model.RaftNode;
import com.tgx.chess.knight.raft.service.ClusterCustom;
import com.tgx.chess.pawn.endpoint.spring.device.DeviceNode;
import com.tgx.chess.pawn.endpoint.spring.device.config.DeviceConfig;
import com.tgx.chess.pawn.endpoint.spring.device.jpa.model.DeviceEntity;
import com.tgx.chess.pawn.endpoint.spring.device.jpa.model.MessageBody;
import com.tgx.chess.pawn.endpoint.spring.device.jpa.repository.IDeviceJpaRepository;
import com.tgx.chess.pawn.endpoint.spring.device.jpa.repository.IMessageJpaRepository;
import com.tgx.chess.pawn.endpoint.spring.device.spi.IDeviceService;
import com.tgx.chess.queen.config.IAioConfig;
import com.tgx.chess.queen.config.IMixConfig;
import com.tgx.chess.queen.event.handler.mix.ILinkCustom;
import com.tgx.chess.queen.io.core.inf.IQoS;
import com.tgx.chess.queen.io.core.inf.ISession;

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
    private final ILinkCustom<ZContext>     _LinkCustom;
    private final ClusterCustom<DeviceNode> _ClusterCustom;
    private final IDeviceJpaRepository      _DeviceRepository;
    private final IMessageJpaRepository     _MessageRepository;
    private final RaftNode<DeviceNode>      _RaftNode;
    private final LogicHandler<DeviceNode>  _LogicHandler;
    private final IQttRouter                _QttRouter;

    @Autowired
    DeviceService(DeviceConfig deviceConfig,
                  IAioConfig ioConfig,
                  IRaftConfig raftConfig,
                  IMixConfig mixConfig,
                  ILinkCustom<ZContext> linkCustom,
                  IMessageJpaRepository messageRepository,
                  IDeviceJpaRepository deviceRepository,
                  IRaftDao raftDao,
                  IQttRouter qttRouter) throws IOException
    {
        final TimeWheel _TimeWheel = new TimeWheel();
        List<ITriple> hosts = new ArrayList<>(2);
        String[] wsSplit = deviceConfig.getAddressWs()
                                       .split(":", 2);
        String[] qttSplit = deviceConfig.getAddressQtt()
                                        .split(":", 2);
        String wsServiceHost = wsSplit[0];
        String qttServiceHost = qttSplit[0];
        int wsServicePort = Integer.parseInt(wsSplit[1]);
        int qttServicePort = Integer.parseInt(qttSplit[1]);
        hosts.add(new Triple<>(wsServiceHost, wsServicePort, ZSort.WS_SERVER));
        hosts.add(new Triple<>(qttServiceHost,
                               qttServicePort,
                               deviceConfig.isQttOverWs() ? ZSort.WS_QTT_SERVER
                                                          : ZSort.QTT_SERVER));
        _DeviceNode = new DeviceNode(hosts, ioConfig, raftConfig, mixConfig, _TimeWheel);
        _DeviceRepository = deviceRepository;
        _LinkCustom = linkCustom;
        _RaftNode = new RaftNode<>(_TimeWheel, raftConfig, raftDao, _DeviceNode);
        _ClusterCustom = new ClusterCustom<>(_RaftNode);
        _LogicHandler = new LogicHandler<>(_DeviceNode,
                                           _QttRouter = qttRouter,
                                           _RaftNode,
                                           _MessageRepository = messageRepository);
    }

    @PostConstruct
    private void start() throws IOException
    {
        _DeviceNode.start(_LogicHandler,
                          new ZLinkMappingCustom(_LinkCustom),
                          new ZClusterMappingCustom<>(_ClusterCustom));
        _RaftNode.init();
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
        Collection<ISession<ZContext>> sessions = _DeviceNode.getMappedSessionsWithType(ZUID.TYPE_CONSUMER_SLOT);
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
        if (onlineDevices != null) {
            return onlineDevices.map(device -> new Pair<>(device, _QttRouter.groupBy(device.getId())));
        }
        return null;
    }

}
