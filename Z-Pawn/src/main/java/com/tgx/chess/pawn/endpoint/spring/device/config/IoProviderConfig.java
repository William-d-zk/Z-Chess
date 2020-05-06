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

package com.tgx.chess.pawn.endpoint.spring.device.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.tgx.chess.king.topology.ZUID;
import com.tgx.chess.knight.cluster.spring.config.SocketConfig;
import com.tgx.chess.queen.config.IAioConfig;
import com.tgx.chess.queen.config.ISocketConfig;

/**
 * @author william.d.zk
 * @date 2020/4/24
 */
@Configuration("io_provider_config")
@ConfigurationProperties(prefix = "z.chess.provider.io")
@PropertySource("classpath:io.provider.properties")
public class IoProviderConfig
        implements
        IAioConfig
{
    private Map<String,
                Integer> sizePowers;

    private SocketConfig consumer;
    private SocketConfig internal;
    private SocketConfig cluster;
    private SocketConfig provider;

    @Override
    public boolean isDomainActive(int type)
    {
        return type <= ZUID.MAX_TYPE;
    }

    @Override
    public int getSizePower(int type)
    {
        switch (type)
        {
            case ZUID.TYPE_CONSUMER_SLOT:
                return sizePowers.getOrDefault("consumer.0", 12);
            case ZUID.TYPE_INTERNAL_SLOT:
                return sizePowers.getOrDefault("internal.1", 7);
            case ZUID.TYPE_PROVIDER_SLOT:
                return sizePowers.getOrDefault("provider.2", 10);
            case ZUID.TYPE_CLUSTER_SLOT:
                return sizePowers.getOrDefault("cluster.3", 7);
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public ISocketConfig getSocketConfig(int type)
    {
        switch (type)
        {
            case ZUID.TYPE_CONSUMER_SLOT:
                return consumer;
            case ZUID.TYPE_INTERNAL_SLOT:
                return internal;
            case ZUID.TYPE_PROVIDER_SLOT:
                return provider;
            case ZUID.TYPE_CLUSTER_SLOT:
                return cluster;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void setSizePowers(Map<String,
                                  Integer> sizePowers)
    {
        this.sizePowers = sizePowers;
    }

    public void setConsumer(SocketConfig consumer)
    {
        this.consumer = consumer;
    }

    public void setInternal(SocketConfig internal)
    {
        this.internal = internal;
    }

    public void setCluster(SocketConfig cluster)
    {
        this.cluster = cluster;
    }

    public void setProvider(SocketConfig provider)
    {
        this.provider = provider;
    }
}
