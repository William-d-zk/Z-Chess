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

package com.tgx.chess.config;

import com.tgx.chess.queen.config.IBizIoConfig;
import com.tgx.chess.queen.config.ISocketConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;

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
                Integer> sizePowers;

    @Override
    public int getClientSizePower()
    {
        return sizePowers.getOrDefault("client", 3);
    }

    @Override
    public int getLocalSizePower()
    {
        return sizePowers.getOrDefault("local", 3);
    }

    @Override
    public int getServerSizePower()
    {
        return sizePowers.getOrDefault("server", 10);
    }

    @Override
    public int getClusterSizePower()
    {
        return sizePowers.getOrDefault("cluster", 7);
    }

    public void setClientSizePower(int clientSizePower)
    {
        this.clientSizePower = clientSizePower;
    }

    public void setLocalSizePower(int localSizePower)
    {
        this.localSizePower = localSizePower;
    }

    public void setServerSizePower(int serverSizePower)
    {
        this.serverSizePower = serverSizePower;
    }

    public void setClusterSizePower(int clusterSizePower)
    {
        this.clusterSizePower = clusterSizePower;
    }

    @Override
    public ISocketConfig getBizSocketConfig(int bizType)
    {
        return null;
    }
}
