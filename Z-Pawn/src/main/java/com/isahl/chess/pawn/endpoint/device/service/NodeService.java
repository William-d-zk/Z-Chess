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
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.schedule.TimeWheel;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.knight.raft.IRaftDao;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.knight.raft.service.RaftCustom;
import com.isahl.chess.pawn.endpoint.device.DeviceNode;
import com.isahl.chess.pawn.endpoint.device.config.MixConfig;
import com.isahl.chess.pawn.endpoint.device.jpa.repository.IDeviceJpaRepository;
import com.isahl.chess.pawn.endpoint.device.spi.IMessageService;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.config.IMixConfig;
import com.isahl.chess.queen.event.handler.mix.ILinkCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author william.d.zk
 * @date 2019-06-10
 */

@Service
public class NodeService
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final DeviceNode               _DeviceNode;
    private final ILinkCustom              _LinkCustom;
    private final RaftCustom<DeviceNode>   _RaftCustom;
    private final RaftNode<DeviceNode>     _RaftNode;
    private final LogicHandler<DeviceNode> _LogicHandler;

    @Autowired
    NodeService(MixConfig deviceConfig,
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
