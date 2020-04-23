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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.tgx.chess.bishop.io.ZSort;
import com.tgx.chess.bishop.io.mqtt.handler.IQttRouter;
import com.tgx.chess.bishop.io.mqtt.handler.QttRouter;
import com.tgx.chess.bishop.io.zhandler.ZClusterMappingCustom;
import com.tgx.chess.bishop.io.zhandler.ZLinkMappingCustom;
import com.tgx.chess.king.base.exception.ZException;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.knight.raft.config.IRaftConfig;
import com.tgx.chess.knight.raft.model.RaftNode;
import com.tgx.chess.knight.raft.model.log.RaftDao;
import com.tgx.chess.knight.raft.service.ClusterCustom;
import com.tgx.chess.pawn.endpoint.DeviceNode;
import com.tgx.chess.pawn.endpoint.spring.device.api.IDeviceService;
import com.tgx.chess.pawn.endpoint.spring.device.model.DeviceDo;
import com.tgx.chess.pawn.endpoint.spring.device.model.DeviceEntry;
import com.tgx.chess.pawn.endpoint.spring.device.model.MessageBody;
import com.tgx.chess.pawn.endpoint.spring.device.model.MessageEntry;
import com.tgx.chess.queen.config.IAioConfig;
import com.tgx.chess.queen.config.IMixConfig;
import com.tgx.chess.queen.db.inf.IRepository;

/**
 * @author william.d.zk
 * @date 2019-06-10
 */

@Service
public class DeviceService
        implements
        IDeviceService
{
    private final Logger                    _Logger = Logger.getLogger(getClass().getSimpleName());
    private final DeviceNode                _DeviceNode;
    private final LinkCustom                _LinkCustom;
    private final ClusterCustom<DeviceNode> _ClusterCustom;
    private final IRepository<MessageEntry> _MessageRepository;
    private final RaftNode<DeviceNode>      _RaftNode;

    @Autowired
    DeviceService(DeviceConfig deviceConfig,
                  IAioConfig aioConfig,
                  IRaftConfig raftConfig,
                  IMixConfig serverConfig,
                  LinkCustom linkCustom,
                  ClusterCustom<DeviceNode> clusterCustom,
                  IRepository<MessageEntry> messageRepository,
                  RaftDao raftDao) throws IOException
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
        hosts.add(new Triple<>(qttServiceHost, qttServicePort, ZSort.QTT_SERVER));
        _DeviceNode = new DeviceNode(hosts, aioConfig, raftConfig, serverConfig, _TimeWheel);
        _MessageRepository = messageRepository;
        _LinkCustom = linkCustom;
        _ClusterCustom = clusterCustom;
        _RaftNode = new RaftNode<>(_TimeWheel, raftConfig, raftDao, _DeviceNode);
        _ClusterCustom.setRaftNode(_RaftNode);
    }

    @PostConstruct
    private void start() throws IOException
    {
        final IQttRouter _QttRouter = new QttRouter();
        LogicHandler logicHandler = new LogicHandler(_DeviceNode, _QttRouter, _MessageRepository);
        _LinkCustom.setQttRouter(_QttRouter);
        _DeviceNode.start(logicHandler,
                          new ZLinkMappingCustom(_LinkCustom),
                          new ZClusterMappingCustom<>(_ClusterCustom));
        _RaftNode.init();
    }

    @Override
    public DeviceDo saveDevice(DeviceDo device) throws ZException
    {
        DeviceEntry entry = convertDo2Entry(device);
        _LinkCustom.findDevice(entry);
        return convertEntry2Do(_LinkCustom.saveDevice(entry), device);
    }

    @Bean
    public RaftNode<DeviceNode> getRaftNode()
    {
        return _RaftNode;
    }

    private DeviceDo convertEntry2Do(DeviceEntry entry, DeviceDo deviceDo)
    {
        DeviceDo dd = deviceDo == null ? new DeviceDo()
                                       : deviceDo;
        dd.setToken(entry.getToken());
        dd.setSn(entry.getSn());
        dd.setPassword(entry.getPassword());
        dd.setUsername(entry.getUsername());
        dd.setInvalidAt(Instant.ofEpochMilli(entry.getInvalidTime()));
        return dd;
    }

    private DeviceEntry convertDo2Entry(DeviceDo deviceDo)
    {
        DeviceEntry deviceEntry = new DeviceEntry();
        deviceEntry.setToken(deviceDo.getToken());
        deviceEntry.setSn(deviceDo.getSn());
        deviceEntry.setPassword(deviceDo.getPassword());
        deviceEntry.setUsername(deviceDo.getUsername());
        if (deviceDo.getInvalidAt() != null) {
            deviceEntry.setInvalidTime(deviceDo.getInvalidAt()
                                               .toEpochMilli());
        }
        return deviceEntry;
    }

    @Override
    public DeviceDo findDevice(DeviceDo key) throws ZException
    {
        DeviceEntry deviceEntry = convertDo2Entry(key);
        _LinkCustom.findDevice(deviceEntry);
        return convertEntry2Do(deviceEntry, key);
    }

    @Override
    public List<DeviceDo> findAllDevices() throws ZException
    {
        return null;
    }

    @Override
    public MessageBody getMessageById(long id) throws ZException
    {
        return null;
    }

}
