/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2020 Z-Chess                                                
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

import com.tgx.chess.bishop.ZUID;
import com.tgx.chess.bishop.biz.config.IClusterConfig;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.CryptUtil;
import com.tgx.chess.king.base.util.IoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.spring.device.model.DeviceEntry;
import com.tgx.chess.spring.jpa.device.dao.DeviceEntity;
import com.tgx.chess.spring.jpa.device.repository.IDeviceJpaRepository;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static com.tgx.chess.king.base.util.IoUtil.isBlank;

/**
 * @author william.d.zk
 */
@Component
public class DeviceRepository
        implements
        IRepository<DeviceEntry>
{
    private final IDeviceJpaRepository _JpaRepository;
    private final IClusterConfig       _ClusterConfig;
    private final ZUID                 _ZUid;
    private final DeviceConfig         _DeviceConfig;
    private final CryptUtil            _CryptUtil = new CryptUtil();
    private final Logger               _Logger    = Logger.getLogger(getClass().getSimpleName());

    @Autowired
    public DeviceRepository(IDeviceJpaRepository jpaRepository,
                            DeviceConfig deviceConfig,
                            IClusterConfig clusterConfig)
    {
        _JpaRepository = jpaRepository;
        _ClusterConfig = clusterConfig;
        _DeviceConfig = deviceConfig;
        _ZUid = _ClusterConfig.createZUID(false);
    }

    @Override
    public DeviceEntry save(IStorage target)
    {
        if (target instanceof DeviceEntry) {
            DeviceEntry device = (DeviceEntry) target;
            if (device.isExist()) {
                DeviceEntity exist = _JpaRepository.findById(device.getPrimaryKey())
                                                   .orElse(null);
                Objects.requireNonNull(exist);
                if (exist.getInvalidAt()
                         .toInstant()
                         .isBefore(Instant.now())
                    || device.getPasswordId() > exist.getPasswordId())
                {
                    exist.setPassword(_CryptUtil.randomPassword(5, 32));
                    exist.increasePasswordId();
                    exist.setInvalidAt(Date.from(Instant.now()
                                                        .plus(_DeviceConfig.getPasswordInvalidDays())));
                }
                exist = _JpaRepository.save(exist);
                return convertDevice(exist, device);
            }
            else {
                DeviceEntity exist = null;
                if (!isBlank(device.getSn())) {
                    exist = _JpaRepository.findBySn(device.getSn());
                }
                else if (!isBlank(device.getToken())) {
                    exist = _JpaRepository.findByToken(device.getToken());
                }
                DeviceEntity entity = exist == null ? new DeviceEntity()
                                                    : exist;
                if (exist == null) {
                    entity = new DeviceEntity();
                    String source = String.format("sn:%s,random %s%d",
                                                  device.getSn(),
                                                  _DeviceConfig.getPasswordRandomSeed(),
                                                  Instant.now()
                                                         .toEpochMilli());
                    _Logger.info("new device %s ", source);
                    entity.setToken(IoUtil.bin2Hex(_CryptUtil.sha256(source.getBytes(StandardCharsets.UTF_8))));
                    entity.setSn(device.getSn());
                    entity.setUsername(device.getUsername());
                }
                if (exist == null
                    || exist.getInvalidAt()
                            .toInstant()
                            .isBefore(Instant.now()))
                {
                    entity.setPassword(_CryptUtil.randomPassword(5, 32));
                    entity.increasePasswordId();
                    entity.setInvalidAt(Date.from(Instant.now()
                                                         .plus(_DeviceConfig.getPasswordInvalidDays())));
                }
                entity = _JpaRepository.save(entity);
                return convertDevice(entity, device);
            }
        }
        return null;
    }

    @Override
    public DeviceEntry find(IStorage key)
    {
        return key instanceof DeviceEntry ? convertDevice(_JpaRepository.findByToken(((DeviceEntry) key).getToken()),
                                                          (DeviceEntry) key)
                                          : null;
    }

    private DeviceEntry convertDevice(DeviceEntity entity, DeviceEntry entry)
    {
        if (entity != null) {
            DeviceEntry deviceEntry = entry == null ? new DeviceEntry()
                                                    : entry;
            deviceEntry.setPrimaryKey(entity.getId());
            deviceEntry.setToken(entity.getToken());
            deviceEntry.setSn(entity.getSn());
            deviceEntry.setUsername(entity.getUsername());
            deviceEntry.setPassword(entity.getPassword());
            deviceEntry.setPasswordId(entity.getPasswordId());
            deviceEntry.setInvalidTime(entity.getInvalidAt()
                                             .getTime());
            return deviceEntry;
        }
        return null;
    }

    @Override
    public List<DeviceEntry> findAll(IStorage key)
    {
        return null;
    }

    @Override
    public void saveAll(List<IStorage> targets)
    {

    }

}
