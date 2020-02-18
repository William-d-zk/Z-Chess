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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.ZUID;
import com.tgx.chess.bishop.biz.config.IClusterConfig;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.spring.device.model.MessageEntry;
import com.tgx.chess.spring.jpa.device.repository.IMessageJpaRepository;

/**
 * @author william.d.zk
 */
@Component
public class MessageRepository
        implements
        IRepository<MessageEntry>
{
    private final IClusterConfig       _ClusterConfig;
    private final ZUID                 _ZUid;
    private final IMessageJpaRepository _MessageJpaRepository;

    @Autowired
    public MessageRepository(IClusterConfig clusterConfig,
                             IMessageJpaRepository messageJpaRepository)
    {
        _ClusterConfig = clusterConfig;
        _MessageJpaRepository = messageJpaRepository;
        _ZUid = _ClusterConfig.createZUID(true);
    }

    @Override
    public MessageEntry save(IStorage target)
    {
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
}
