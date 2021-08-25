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

package com.isahl.chess.player.api.service;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.pawn.endpoint.device.api.features.IDeviceService;
import com.isahl.chess.pawn.endpoint.device.api.db.model.ShadowEntity;
import com.isahl.chess.pawn.endpoint.device.db.remote.postgres.model.DeviceEntity;
import com.isahl.chess.player.api.model.DeviceDo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.isahl.chess.king.base.util.IoUtil.isBlank;

/**
 * @author william.d.zk
 */
@Service
public class MixOpenService
{

    private final Logger _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());

    private final IDeviceService _DeviceService;

    @Autowired
    public MixOpenService(IDeviceService deviceService)
    {
        _DeviceService = deviceService;
    }

    public DeviceEntity newDevice(DeviceEntity device)
    {
        return _DeviceService.upsertDevice(device);
    }

    public DeviceEntity findByToken(String token)
    {
        return _DeviceService.findByToken(token);
    }

    public DeviceEntity findBySn(String sn)
    {
        return _DeviceService.findBySn(sn);
    }

    @SafeVarargs
    public final List<DeviceEntity> findAllByColumnsAfter(Pageable pageable,
                                                          LocalDateTime dateTime,
                                                          Triple<String, Object, Predicate.BooleanOperator>... columns)
    {
        return _DeviceService.findDevices((Specification<DeviceEntity>) (root, criteriaQuery, criteriaBuilder)->{
            criteriaBuilder.greaterThan(root.get("createAt"), dateTime);
            if(columns != null && columns.length > 0) {
                Predicate.BooleanOperator last = Predicate.BooleanOperator.AND;
                List<Predicate> predicates = new LinkedList<>();
                for(Triple<String, Object, Predicate.BooleanOperator> triple : columns) {
                    String column = triple.getFirst();
                    Object key = triple.getSecond();
                    Predicate.BooleanOperator op = triple.getThird();
                    Predicate predicate = criteriaBuilder.equal(root.get(column), key);
                    if(last != null) {
                        switch(last) {
                            case AND -> criteriaBuilder.and(predicate);
                            case OR -> criteriaBuilder.or(predicate);
                        }
                    }
                    predicates.add(predicate);
                    last = op;
                }
                return criteriaQuery.where(predicates.toArray(new Predicate[0]))
                                    .getRestriction();
            }
            else {
                return criteriaQuery.getRestriction();
            }
        }, pageable);
    }

    public List<DeviceDo> getOnlineDevice(Pageable pageable)
    {
        return shadows2DeviceDo(_DeviceService.getOnlineDevices(null, pageable));
    }

    private List<DeviceDo> shadows2DeviceDo(List<ShadowEntity> shadows)
    {
        if(shadows == null || shadows.isEmpty()) {return new LinkedList<>();}
        List<DeviceEntity> devices = _DeviceService.findDevicesIn(shadows.stream()
                                                                         .map(ShadowEntity::getDeviceId)
                                                                         .collect(Collectors.toList()));
        return devices.stream()
                      .map(DeviceDo::of)
                      .collect(Collectors.toList());
    }

    public List<DeviceDo> getOnlineDevicesGroupByUsername(String username, Pageable pageable)
    {
        if(isBlank(username)) {return getOnlineDevice(pageable);}
        return shadows2DeviceDo(_DeviceService.getOnlineDevices((Specification<ShadowEntity>) (r, q, builder)->{
            return q.where(builder.equal(r.get("username"), username))
                    .getRestriction();
        }, pageable));
    }

}