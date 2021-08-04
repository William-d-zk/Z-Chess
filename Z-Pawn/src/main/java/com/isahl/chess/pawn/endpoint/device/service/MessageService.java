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

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.pawn.endpoint.device.jpa.remote.postgres.model.MessageEntity;
import com.isahl.chess.pawn.endpoint.device.jpa.remote.postgres.repository.IMessageJpaRepository;
import com.isahl.chess.pawn.endpoint.device.model.MessageBody;
import com.isahl.chess.pawn.endpoint.device.api.IMessageService;
import com.isahl.chess.pawn.endpoint.device.spi.plugin.IMessagePlugin;
import com.isahl.chess.rook.storage.cache.config.EhcacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.cache.CacheManager;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * @author william.d.zk
 * @date 2020/2/21
 */
@Service
public class MessageService
        implements IMessageService
{

    private final IMessageJpaRepository _MessageRepository;
    private final List<IMessagePlugin>  _MessagePlugins;
    private final CacheManager          _CacheManager;
    private final Random                _Random = new Random();

    @Autowired
    public MessageService(IMessageJpaRepository jpaRepository,
                          CacheManager cacheManager,
                          List<IMessagePlugin> messagePlugins)
    {
        _MessageRepository = jpaRepository;
        _MessagePlugins = messagePlugins;
        _CacheManager = cacheManager;
    }

    @PostConstruct
    void init() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        EhcacheConfig.createCache(_CacheManager,
                                  "message_cache",
                                  String.class,
                                  MessageEntity.class,
                                  Duration.of(2, MINUTES));
    }

    @Override
    public List<MessageBody> listByTopic(String topic, int limit) throws ZException
    {

        List<MessageEntity> messageList = _MessageRepository.listByTopic(topic, limit);
        return messageList.stream()
                          .map(MessageEntity::getBody)
                          .collect(Collectors.toList());
    }

    public List<MessageEntity> findAfterId(long id)
    {
        return _MessageRepository.findAll((Specification<MessageEntity>) (root, criteriaQuery, criteriaBuilder)->{
            return criteriaQuery.where(criteriaBuilder.greaterThan(root.get("id"), id))
                                .getRestriction();
        });
    }

    @Override
    public void handleMessage(MessageEntity msgEntity)
    {
        _MessagePlugins.forEach(plugin->plugin.handleMessage(msgEntity));
    }

    @Override
    public Optional<MessageEntity> find1Msg(Specification<MessageEntity> specification)
    {
        //TODO 此处直接访问DB不是个好设计，中间应该有一层Batch-Async + Cache的中间层降低DB负载
        return _MessageRepository.findOne(specification);
    }

    @Override
    public List<MessageEntity> findAllMsg(Specification<MessageEntity> specification, Pageable pageable)
    {
        return _MessageRepository.findAll(specification, pageable)
                                 .toList();
    }
}
