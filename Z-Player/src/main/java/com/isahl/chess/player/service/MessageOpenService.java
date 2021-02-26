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

package com.isahl.chess.player.service;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.player.model.MessageDo;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.MessageEntity;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.repository.IMessageJpaRepository;
import org.hibernate.LazyInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.List;

import static com.isahl.chess.queen.db.inf.IStorage.Operation.OP_INSERT;

/**
 * @author william.d.zk
 */
@Service
public class MessageOpenService
{
    private final Logger                _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());
    private final ZUID                  _ZUID;
    private final IMessageJpaRepository _JpaRepository;

    @Autowired
    public MessageOpenService(IRaftConfig clusterConfig,
                              IMessageJpaRepository jpaRepository)
    {
        _JpaRepository = jpaRepository;
        _ZUID = clusterConfig.createZUID();
    }

    public MessageEntity save(MessageDo message)
    {
        MessageEntity entity = null;
        EXIST:
        {
            if (message.operation()
                       .getValue() > OP_INSERT.getValue())
            {
                try {
                    entity = _JpaRepository.getOne(message.getId());
                    entity.setOwner(message.getOwner());
                    break EXIST;
                }
                catch (EntityNotFoundException |
                       LazyInitializationException |
                       JpaObjectRetrievalFailureException e)
                {
                    _Logger.warning("update failed", e);
                }
            }
            // 当数据库中存在多条记录而被查询出来时，程序会报错。
            // TODO 需要改造
            try {
                entity = _JpaRepository.findByOriginAndDestinationAndMsgId(message.getOrigin(),
                                                                           message.getDestination(),
                                                                           message.getMsgId());
                if (entity != null) {
                    entity.setOwner(message.getOwner());
                    break EXIST;
                }
            }
            catch (Exception e) {
                // msg id 重复
                List<MessageEntity> toDelete = _JpaRepository.findAllByOriginAndDestinationAndMsgId(message.getOrigin(),
                                                                                                    message.getDestination(),
                                                                                                    message.getMsgId());
                _JpaRepository.deleteAll(toDelete);
                _Logger.warning("[%d] row with origin %d,destination:%s,msg_id:%d",
                                e,
                                toDelete.size(),
                                message.getOrigin(),
                                message.getDestination(),
                                message.getMsgId());
            }
            if (entity == null) {
                entity = new MessageEntity();
                entity.setOrigin(message.getOrigin());
                entity.setDestination(message.getDestination());
                entity.setMsgId(message.getMsgId());
                entity.setOwner(message.getOwner());
                entity.setDirection(message.getDirection());
                entity.setBody(message.getContent());
            }
        }
        entity = _JpaRepository.save(entity);
        return entity;
    }

    public MessageEntity find(MessageEntity key)
    {
        long primary = key.primaryKey();
        long msgId = key.getMsgId();
        long origin = key.getOrigin();
        long destination = key.getDestination();
        if (primary == 0 && (msgId == 0 || origin == 0 || destination == 0)) {
            return null;
        }
        else {
            MessageEntity entity = null;
            try {
                entity = _JpaRepository.findByOriginAndDestinationAndMsgId(origin, destination, msgId);
            }
            catch (EntityNotFoundException e) {
                _Logger.warning(e);
            }
            if (entity != null) { return entity; }
            try {
                entity = _JpaRepository.getOne(primary);
            }
            catch (EntityNotFoundException e) {
                _Logger.warning(e);
            }
            if (entity != null) { return entity; }
        }
        return null;
    }

    public long generateId()
    {
        return _ZUID.getId();
    }

}
