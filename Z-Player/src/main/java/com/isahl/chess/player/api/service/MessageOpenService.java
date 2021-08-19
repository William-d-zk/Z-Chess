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
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.pawn.endpoint.device.api.jpa.model.MessageEntity;
import com.isahl.chess.player.api.model.MessageDo;
import org.hibernate.LazyInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;

import static com.isahl.chess.queen.db.inf.IStorage.Operation.OP_INSERT;

/**
 * @author william.d.zk
 */
@Service
public class MessageOpenService
{
    private final Logger                _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());
    private final ZUID                  _ZUID;

    @Autowired
    public MessageOpenService(IRaftConfig clusterConfig)
    {
        _ZUID = clusterConfig.createZUID();
    }

    public MessageEntity save(MessageDo message)
    {
        MessageEntity entity = null;
        EXIST:
        {
            if(message.operation()
                      .getValue() > OP_INSERT.getValue())
            {
                try {
//                    entity = _JpaRepository.getById(message.getId());
                    break EXIST;
                }
                catch(EntityNotFoundException | LazyInitializationException | JpaObjectRetrievalFailureException e) {
                    _Logger.warning("update failed", e);
                }
            }
            // 当数据库中存在多条记录而被查询出来时，程序会报错。

            entity = new MessageEntity();
            entity.setOrigin(message.getOrigin());
            entity.setTopic(message.getTopic());
        }
//        entity = _JpaRepository.save(entity);
        return entity;
    }

    public long generateId()
    {
        return _ZUID.getId();
    }

}
