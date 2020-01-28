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

import com.tgx.chess.bishop.biz.db.dao.ZUID;
import com.tgx.chess.config.ZClusterConfig;
import com.tgx.chess.king.base.log.Logger;

/**
 * @author william.d.zk
 */
@Component
public class ZGenerator
        implements
        IdentifierGenerator
{
    private final Logger _Logger = Logger.getLogger(getClass().getName());

    @Autowired
    private ZClusterConfig zClusterConfig;
    private ZUID           zuid;

    public ZGenerator()
    {
    }

    @PostConstruct
    public void init()
    {
        zuid = new ZUID(zClusterConfig.getIdcId(),
                        zClusterConfig.getClusterId(),
                        zClusterConfig.getNodeId(),
                        zClusterConfig.getType());
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
        return zuid.getId();
    }
}
