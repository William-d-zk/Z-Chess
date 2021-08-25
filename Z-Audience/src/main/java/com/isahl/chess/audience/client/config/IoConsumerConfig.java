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

package com.isahl.chess.audience.client.config;

import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.config.ISocketConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;

/**
 * @author william.d.zk
 * 
 * @date 2020/2/1
 */
@Configuration("io_consumer_config")
@ConfigurationProperties(prefix = "z.chess.consumer.io")
@PropertySource("classpath:consumer.io.properties")
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
        return switch (type)
        {
            case ZUID.TYPE_INTERNAL_SLOT -> sizePowers.getOrDefault("internal.1", 9);
            case ZUID.TYPE_CONSUMER_SLOT -> sizePowers.getOrDefault("consumer.0", 3);
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public ISocketConfig getSocketConfig(int type)
    {
        return switch (type)
        {
            case ZUID.TYPE_INTERNAL_SLOT -> internal;
            case ZUID.TYPE_CONSUMER_SLOT -> consumer;
            default -> null;
        };
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
