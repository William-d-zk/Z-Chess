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

package com.tgx.chess.rook.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.tgx.chess.queen.config.IBizIoConfig;
import com.tgx.chess.queen.config.ISocketConfig;
import com.tgx.chess.queen.io.core.inf.ISessionManager;

/**
 * @author william.d.zk
 * @date 2020/2/1
 */
@Configuration
@ConfigurationProperties(prefix = "z.chess.io")
@PropertySource("classpath:io.properties")
public class IoConfig
        implements
        IBizIoConfig
{

    private Map<String,
                Integer>         sizePowers;
    private SocketConfig         server;
    private SocketConfig         cluster;
    private SocketConfig         client;
    private SocketConfig         local;

    @Override
    public int getSizePower(int bizType)
    {
        switch (bizType)
        {
            case ISessionManager.CLIENT_SLOT:
                return sizePowers.getOrDefault("client.0", 3);
            case ISessionManager.LOCAL_SLOT:
                return sizePowers.getOrDefault("local.1", 3);
            case ISessionManager.SERVER_SLOT:
                return sizePowers.getOrDefault("server.2", 10);
            case ISessionManager.CLUSTER_SLOT:
                return sizePowers.getOrDefault("cluster.3", 7);
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public ISocketConfig getBizSocketConfig(int bizType)
    {
        switch (bizType)
        {
            case ISessionManager.CLIENT_SLOT:
                return client;
            case ISessionManager.LOCAL_SLOT:
                return local;
            case ISessionManager.SERVER_SLOT:
                return server;
            case ISessionManager.CLUSTER_SLOT:
                return cluster;
            default:
                return null;
        }
    }

    public void setSizePowers(Map<String,
                                  Integer> sizePowers)
    {
        this.sizePowers = sizePowers;
    }

    public void setServer(SocketConfig server)
    {
        this.server = server;
    }

    public void setCluster(SocketConfig cluster)
    {
        this.cluster = cluster;
    }

    public void setClient(SocketConfig client)
    {
        this.client = client;
    }

    public void setLocal(SocketConfig local)
    {
        this.local = local;
    }
}
