/*
 * MIT License                                                                   
 *                                                                               
 * Copyright (c) 2016~2020. Z-Chess                                          
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

package com.tgx.chess.pawn.endpoint.spring.jpa.generator;

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.topology.ZUID;
import com.tgx.chess.knight.raft.config.ZRaftConfig;

/**
 * 
 * JPA 会优先初始化
 * 且会给每一个表的都创建一个IdentifierGenerator，
 * ZUid 是一个全局生成器，ZClusterConfig 在 spring 中是 single instance创建的
 * 程序启动时装载 bean 也是沿用了single instance的模式，所以在init时正式注入 ZUid
 * 通过static 同步给所有 JPA 的所有引用。
 * 
 * @author william.d.zk
 */
@Component
public class ZDeviceGenerator
        implements
        IdentifierGenerator
{
    private static ZUID       _ZUID;
    private final Logger      _Logger = Logger.getLogger(getClass().getSimpleName());
    private final ZRaftConfig _ZClusterConfig;

    @Autowired
    public ZDeviceGenerator(ZRaftConfig config)
    {
        _ZClusterConfig = config;
    }

    public ZDeviceGenerator()
    {
        _ZClusterConfig = null;
    }

    @PostConstruct
    public void init()
    {
        if (_ZClusterConfig == null) return;
        _ZUID = _ZClusterConfig.createZUID();
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException
    {
        long id = next();
        _Logger.debug("generate z-id %x, %s", id, object);
        return id;
    }

    private long next()
    {
        return _ZUID.getId(ZUID.TYPE_CONSUMER);
    }
}
