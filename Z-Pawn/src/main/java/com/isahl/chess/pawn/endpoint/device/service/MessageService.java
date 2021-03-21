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
import com.isahl.chess.pawn.endpoint.device.jpa.model.MessageBody;
import com.isahl.chess.pawn.endpoint.device.jpa.model.MessageEntity;
import com.isahl.chess.pawn.endpoint.device.jpa.repository.IMessageJpaRepository;
import com.isahl.chess.pawn.endpoint.device.spi.IMessageService;
import com.isahl.chess.pawn.endpoint.device.spi.plugin.IMessagePlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author william.d.zk
 * 
 * @date 2020/2/21
 */
@Service
public class MessageService
        implements
        IMessageService
{

    private final IMessageJpaRepository _MessageRepository;
    private final List<IMessagePlugin>  _MessagePlugins;

    @Autowired
    public MessageService(IMessageJpaRepository jpaRepository,
                          List<IMessagePlugin> messagePlugins)
    {
        _MessageRepository = jpaRepository;
        _MessagePlugins = messagePlugins;
    }

    @Override
    public List<MessageBody> listByTopic(String topic, int limit) throws ZException
    {

        List<MessageEntity> messageList = _MessageRepository.listByTopic(topic, limit);
        return messageList.stream()
                          .map(MessageEntity::getBody)
                          .collect(Collectors.toList());
    }

    @Override
    public List<MessageEntity> findAfterId(long id)
    {
        return _MessageRepository.findAll((Specification<MessageEntity>) (root,
                                                                          criteriaQuery,
                                                                          criteriaBuilder) -> criteriaQuery.where(criteriaBuilder.greaterThan(root.get("id"),
                                                                                                                                              id))
                                                                                                           .getRestriction());
    }

    @Override
    public MessageEntity handleMessage(MessageEntity msgEntity)
    {
        _MessagePlugins.forEach(plugin -> plugin.handleMessage(msgEntity));
        return msgEntity;
    }

    @Override
    public MessageEntity find1Msg(long src, long dest, long msgId, LocalDateTime time)
    {
        return _MessageRepository.findByOriginAndDestinationAndMsgIdAndCreatedAtAfter(src, dest, msgId, time);
    }

    @Override
    public MessageEntity find1Msg(long msgUid)
    {
        return _MessageRepository.getOne(msgUid);
    }

}
