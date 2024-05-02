/*
 * MIT License
 *
 * Copyright (c) 2016~2022. Z-Chess
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

package com.isahl.chess.pawn.endpoint.device.resource.service;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.pawn.endpoint.device.db.central.model.MessageEntity;
import com.isahl.chess.pawn.endpoint.device.db.central.model.MsgDeliveryStatus;
import com.isahl.chess.pawn.endpoint.device.db.central.repository.IMessageRepository;
import com.isahl.chess.pawn.endpoint.device.db.central.repository.IMsgDeliveryStatusRepository;
import com.isahl.chess.pawn.endpoint.device.resource.features.IMessageService;
import com.isahl.chess.pawn.endpoint.device.resource.model.MessageBody;
import com.isahl.chess.rook.storage.cache.config.EhcacheConfig;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.cache.CacheManager;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * @author william.d.zk
 * @date 2020/2/21
 */
@Service
public class MessageService
        implements IMessageService
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final CacheManager                 _CacheManager;
    private final IMessageRepository           _MessageRepository;
    private final IMsgDeliveryStatusRepository _MsgDeliveryStatusRepository;
    private final IRaftConfig                  _RaftConfig;

    @Autowired
    public MessageService(IRaftConfig raftConfig,
                          CacheManager cacheManager,
                          IMessageRepository messageRepository,
                          IMsgDeliveryStatusRepository statusRepository)
    {
        _RaftConfig = raftConfig;
        _CacheManager = cacheManager;
        _MessageRepository = messageRepository;
        _MsgDeliveryStatusRepository = statusRepository;
    }

    @PostConstruct
    void init() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        EhcacheConfig.createCache(_CacheManager,
                                  "msg_delivery_status_cache",
                                  String.class,
                                  MsgDeliveryStatus.class,
                                  Duration.of(4, HOURS));
    }

    @Override
    public List<MessageBody> listByTopic(String topic, int limit) throws ZException
    {
        return null;
    }

    @Override
    public List<MessageEntity> findAfterId(long id)
    {
        return _MessageRepository.findAll((root, criteriaQuery, criteriaBuilder)->criteriaQuery.where(criteriaBuilder.greaterThan(
                                                                                                       root.get("id"),
                                                                                                       id))
                                                                                               .getRestriction());

    }

    @Override
    public Optional<MessageEntity> findOneMsg(Specification<MessageEntity> specification)
    {
        return _MessageRepository.findOne(specification);
    }

    @Override
    public List<MessageEntity> findAllMsg(Specification<MessageEntity> specification, Pageable pageable)
    {
        return _MessageRepository.findAll(specification, pageable)
                                 .toList();
    }

    @Override
    public void submit(MessageEntity post)
    {
        post.setMessageId(generateId());
        _MessageRepository.save(post);
    }

    @Override
    public void submitAll(List<MessageEntity> contents)
    {
        if(contents == null || contents.isEmpty()) return;
        _Logger.debug("message service submit [%d]", contents.size());
        _MessageRepository.saveAll(contents);
    }

    @Override
    public long generateId()
    {
        return _RaftConfig.getZUID().getId();
    }

    @Override
    public long generateId(long session)
    {
        return _RaftConfig.getZUID().moveOn(session);
    }

    @Cacheable(value = "msg_delivery_status_cache",
               key = "#p0",
               condition = "#p0 != null",
               unless = "#result == null")
    public MsgDeliveryStatus getDeliveryStatus(String status)
    {
        return _MsgDeliveryStatusRepository.findByStatus(status);
    }

    @Override
    public void stateInit(MessageEntity content)
    {
        Objects.requireNonNull(content);
        content.setStatus(Set.of(_MsgDeliveryStatusRepository.findByFlag("start")));
    }
}
