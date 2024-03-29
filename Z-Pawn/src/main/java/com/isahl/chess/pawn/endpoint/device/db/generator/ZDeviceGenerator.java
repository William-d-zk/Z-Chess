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

package com.isahl.chess.pawn.endpoint.device.db.generator;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.config.ZRaftConfig;
import jakarta.annotation.PostConstruct;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
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
        implements IdentifierGenerator
{
    private static ZUID   _ZUID;
    private final  Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final IRaftConfig _ZClusterConfig;

    @Autowired
    public ZDeviceGenerator(ZRaftConfig config)
    {
        _ZClusterConfig = config;
    }

    /**
     * 不能删除，JPA 注入的时候用的是这个方法，Bean 构建之后才
     * 正式初始化了ZUID，且ZRaftConfig中也仅持有one-instance
     */
    public ZDeviceGenerator()
    {
        _ZClusterConfig = null;
    }

    @PostConstruct
    public void init()
    {
        if(_ZClusterConfig == null) {return;}
        _ZUID = _ZClusterConfig.getZUID();
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        long id = next();
        _Logger.debug("generate z-id %#x, %s", id, object);
        return id;
    }

    private long next()
    {
        return _ZUID.getId(ZUID.TYPE_CONSUMER);
    }
}
