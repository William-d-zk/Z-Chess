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

package com.tgx.chess.rook.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.tgx.chess.king.topology.ZUID;
import com.tgx.chess.queen.config.IAioConfig;
import com.tgx.chess.queen.config.ISocketConfig;

/**
 * @author william.d.zk
 * @date 2020/2/1
 */
@Configuration
@ConfigurationProperties(prefix = "z.com.tgx.chess.io")
@PropertySource("classpath:io.consumer.properties")
public class IoConsumerConfig
        implements
        IAioConfig
{

    private Map<String,
                Integer>         sizePowers;
    private SocketConfig         consumer;
    private SocketConfig         internal;

    @Override
    public boolean isDomainActive(int type)
    {
        return type == ZUID.TYPE_CONSUMER_SLOT || type == ZUID.TYPE_INTERNAL_SLOT;
    }

    @Override
    public int getSizePower(int type)
    {
        switch (type)
        {
            case ZUID.TYPE_INTERNAL_SLOT:
                return sizePowers.getOrDefault("internal.1", 9);
            case ZUID.TYPE_CONSUMER_SLOT:
                return sizePowers.getOrDefault("consumer.0", 3);
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public ISocketConfig getSocketConfig(int type)
    {
        switch (type)
        {
            case ZUID.TYPE_INTERNAL_SLOT:
                return internal;
            case ZUID.TYPE_CONSUMER_SLOT:
                return consumer;
            default:
                return null;
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
}
