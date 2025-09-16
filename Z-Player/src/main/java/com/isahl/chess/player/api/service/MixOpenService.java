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

import static java.lang.Math.min;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.pawn.endpoint.device.db.central.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.resource.features.IDeviceService;
import com.isahl.chess.pawn.endpoint.device.resource.features.IStateService;
import com.isahl.chess.player.api.model.DeviceDo;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * @author william.d.zk
 */
@Service
public class MixOpenService
{

    private final Logger _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());

    private final IDeviceService _DeviceService;
    private final IStateService  _StateService;

    @Autowired
    public MixOpenService(IDeviceService deviceService, IStateService stateService)
    {
        _DeviceService = deviceService;
        _StateService = stateService;
    }

    /**
     * 新增设备信息
     *
     * @param device
     * @return
     */
    public DeviceEntity newDevice(DeviceEntity device)
    {
        return _DeviceService.upsertDevice(device);
    }

    /**
     * 更新设备信息
     *
     * @param device
     * @return
     */
    public DeviceEntity updateDevice(DeviceEntity device)
    {
        return _DeviceService.updateDevice(device);
    }

    /**
     * 根据设备令牌查找
     *
     * @param token
     * @return
     */
    public DeviceEntity findByToken(String token)
    {
        return _DeviceService.findByToken(token);
    }

    /**
     * 根据设备编号查找
     *
     * @param number
     * @return
     */
    public DeviceEntity findByNumber(String number)
    {
        return _DeviceService.findByNotice(number);
    }

    @SafeVarargs
    public final List<DeviceEntity> findAllByColumnsAfter(Pageable pageable,
                                                          LocalDateTime dateTime,
                                                          Triple<String, Object, Predicate.BooleanOperator>... columns)
    {
        return _DeviceService.findDevices((Specification<DeviceEntity>) (root, criteriaQuery, criteriaBuilder)->{
            criteriaBuilder.greaterThan(root.get("createdAt"), dateTime);
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
        List<Long> sessions = _StateService.listIndex();
        return sessions.subList((int) pageable.getOffset(),
                                min(sessions.size(),
                                    (int) pageable.next()
                                                  .getOffset()))
                       .stream()
                       .map(deviceIdx->DeviceDo.of(_StateService.getClient(deviceIdx)))
                       .toList();
    }

    public List<DeviceDo> getStorageDevice(Pageable pageable)
    {
        return _StateService.listStorages(pageable)
                            .stream()
                            .map(se->DeviceDo.of(se.client()))
                            .toList();
    }





    /** 需要调试 服务收到的Http 请求的时候放开
     * Application.properties 需要添加
     -- logging.level.org.springframework.web.filter.CommonsRequestLoggingFilter=debug
    @Bean
    CommonsRequestLoggingFilter loggingFilter(){
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        // 记录 客户端 IP信息
        loggingFilter.setIncludeClientInfo(true);
        // 记录请求头
        loggingFilter.setIncludeHeaders(true);
        // 如果记录请求头的话，可以指定哪些记录，哪些不记录
        // loggingFilter.setHeaderPredicate();
        // 记录 请求体  特别是POST请求的body参数
        loggingFilter.setIncludePayload(true);
        // 请求体的大小限制 默认50
        loggingFilter.setMaxPayloadLength(10000);
        //记录请求路径中的query参数
        loggingFilter.setIncludeQueryString(true);
        return loggingFilter;
    }
    */
}