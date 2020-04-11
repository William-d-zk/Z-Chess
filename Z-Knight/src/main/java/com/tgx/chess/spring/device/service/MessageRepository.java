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

import static com.tgx.chess.queen.db.inf.IStorage.Operation.OP_INSERT;

import java.util.List;

import javax.persistence.EntityNotFoundException;

import org.hibernate.LazyInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.ZUID;
import com.tgx.chess.bishop.biz.config.IClusterConfig;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.spring.device.model.MessageBody;
import com.tgx.chess.spring.device.model.MessageEntry;
import com.tgx.chess.spring.jpa.device.dao.MessageEntity;
import com.tgx.chess.spring.jpa.device.repository.IMessageJpaRepository;

/**
 * @author william.d.zk
 */
@Component
public class MessageRepository
        implements
        IRepository<MessageEntry>
{
    private final Logger                _Logger = Logger.getLogger(getClass().getSimpleName());
    private final IClusterConfig        _ClusterConfig;
    private final ZUID                  _ZUid;
    private final IMessageJpaRepository _JpaRepository;

    @Autowired
    public MessageRepository(IClusterConfig clusterConfig,
                             IMessageJpaRepository jpaRepository)
    {
        _ClusterConfig = clusterConfig;
        _JpaRepository = jpaRepository;
        _ZUid = _ClusterConfig.createZUID();
    }

    @Override
    public MessageEntry save(IStorage target)
    {
        if (target instanceof MessageEntry) {
            MessageEntry message = (MessageEntry) target;
            MessageEntity entity;
            EXIST:
            {

                if (message.getOperation()
                           .getValue() > OP_INSERT.getValue())
                {
                    try {
                        entity = _JpaRepository.getOne(message.getPrimaryKey());
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

                entity = new MessageEntity();
                entity.setOrigin(message.getOrigin());
                entity.setDestination(message.getDestination());
                entity.setMsgId(message.getMsgId());
                entity.setDirection(message.getDirection());
                entity.setOwner(message.getOwner());
                entity.setCmd(message.getCmd());
                MessageBody body = new MessageBody();
                body.setTopic(message.getTopic());
                body.setPayload(message.getPayload());
                entity.setPayload(body);
            }
            _JpaRepository.save(entity);
            message.setPrimaryKey(entity.getId());
            return message;
        }
        return null;
    }

    @Override
    public MessageEntry find(IStorage key)
    {
        return null;
    }

    @Override
    public List<MessageEntry> findAll(IStorage key)
    {
        return null;
    }

    @Override
    public void saveAll(List<IStorage> targets)
    {
    }

    @Override
    public long getPeerId()
    {
        return _ZUid.getDevicePeerId();
    }

    @Override
    public long getZid()
    {
        return _ZUid.getId();
    }
}
