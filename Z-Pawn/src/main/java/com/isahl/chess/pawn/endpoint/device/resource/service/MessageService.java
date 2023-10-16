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

import static java.time.temporal.ChronoUnit.MINUTES;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.pawn.endpoint.device.db.central.model.MessageEntity;
import com.isahl.chess.pawn.endpoint.device.db.central.repository.IMessageRepository;
import com.isahl.chess.pawn.endpoint.device.resource.features.IMessageService;
import com.isahl.chess.pawn.endpoint.device.resource.model.MessageBody;
import com.isahl.chess.rook.storage.cache.config.EhcacheConfig;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * @author william.d.zk
 * @date 2020/2/21
 */
@Service
public class MessageService
        implements IMessageService
{

    private final CacheManager       _CacheManager;
    private final IMessageRepository _MessageRepository;
    private final ZUID               _ZUID;

    @Autowired
    public MessageService(CacheManager cacheManager, IMessageRepository messageRepository, IRaftConfig raftConfig)
    {
        _CacheManager = cacheManager;
        _MessageRepository = messageRepository;
        _ZUID = raftConfig.getZUID();
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
        return null;
    }

    public List<MessageEntity> findAfterId(long id)
    {
        return null;
        /*
        return _MessageRepository.findAll((Specification<MessageEntity>) (root, criteriaQuery, criteriaBuilder)->{
            return criteriaQuery.where(criteriaBuilder.greaterThan(root.get("id"), id))
                                .getRestriction();
        });
         */
    }

    @Override
    public Optional<MessageEntity> findOneMsg(Specification<MessageEntity> specification)
    {
        return Optional.empty();
    }

    @Override
    public List<MessageEntity> findAllMsg(Specification<MessageEntity> specification, Pageable pageable)
    {
        return null;
        /*
        return _MessageRepository.findAll(specification, pageable)
                                 .toList();
         */
    }

    @Override
    public void submit(MessageEntity post)
    {

    }

    @Override
    public long generateId(long session)
    {
        return _ZUID.moveOn(session);
    }
}
