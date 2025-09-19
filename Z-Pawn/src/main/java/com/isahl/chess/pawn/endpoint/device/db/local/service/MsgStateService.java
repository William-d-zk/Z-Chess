/*
 * MIT License
 *
 * Copyright (c) 2025. Z-Chess
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

package com.isahl.chess.pawn.endpoint.device.db.local.service;

import com.isahl.chess.pawn.endpoint.device.db.local.model.MsgStateEntity;
import com.isahl.chess.pawn.endpoint.device.db.local.repository.IMsgStateRepository;
import com.isahl.chess.rook.storage.cache.config.EhcacheConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.cache.CacheManager;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * @author william.d.zk
 * @version 1.0.18
 * @since 2025-09-18
 */
@Service
public class MsgStateService
{
    private final IMsgStateRepository _MsgStateRepository;
    private final CacheManager        _CacheManager;

    @Autowired(required = false)
    public MsgStateService(IMsgStateRepository msgStateRepository, CacheManager cacheManager)
    {
        _MsgStateRepository = msgStateRepository;
        _CacheManager = cacheManager;
    }

    @PostConstruct
    void initCache() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        EhcacheConfig.createCache(_CacheManager,
                                  "msg_local_cache",
                                  String.class,
                                  MsgStateEntity.class,
                                  Duration.of(2, MINUTES));
    }

    @Cacheable(condition = "#p0 != null",
               key = "#p0",
               unless = "#result == null",
               value = "msg_local_cache")
    public MsgStateEntity getMsgStateEntity(String id)
    {
        return _MsgStateRepository.findById(id)
                                  .orElse(null);
    }

    @CachePut(condition = "#p0 != null",
              value = "msg_local_cache",
              key = "#p0.getId()")
    public void setMsgStateEntity(MsgStateEntity msgStateEntity)
    {
        _MsgStateRepository.save(msgStateEntity);
    }

    @CacheEvict(condition = "#p0 != null",
                value = "msg_local_cache",
                key = "#p0")
    public void deleteMsgStateEntity(String id)
    {
        _MsgStateRepository.deleteById(id);
    }


}
