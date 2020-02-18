/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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

package com.tgx.chess.spring.jpa.generator;

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.ZUID;
import com.tgx.chess.config.ZClusterConfig;
import com.tgx.chess.king.base.log.Logger;

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
public class ZMessageGenerator
        implements
        IdentifierGenerator
{
    private final Logger _Logger = Logger.getLogger(getClass().getName());
    private static ZUID  _ZUid;

    private final ZClusterConfig _ZClusterConfig;

    @Autowired
    public ZMessageGenerator(ZClusterConfig config)
    {
        _ZClusterConfig = config;
    }

    public ZMessageGenerator()
    {
        _ZClusterConfig = null;
    }

    @PostConstruct
    public void init()
    {
        if (_ZClusterConfig == null) return;
        _ZUid = _ZClusterConfig.createZUID(true);
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
        return _ZUid.getId();
    }
}
