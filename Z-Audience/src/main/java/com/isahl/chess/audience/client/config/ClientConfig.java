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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration("audience_client_config")
@ConfigurationProperties(prefix = "z.chess.audience")
@PropertySource("classpath:audience.properties")
public class ClientConfig
{

    public static class Target
    {
        private String name;
        private String host;
        private int    port;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getHost()
        {
            return host;
        }

        public void setHost(String host)
        {
            this.host = host;
        }

        public int getPort()
        {
            return port;
        }

        public void setPort(int port)
        {
            this.port = port;
        }
    }

    private Target qtt;
    private Target ws;
    private Target chat;
    private int    ioCount;

    public Target getQtt()
    {
        return qtt;
    }

    public void setQtt(Target qtt)
    {
        this.qtt = qtt;
    }

    public Target getWs()
    {
        return ws;
    }

    public void setWs(Target ws)
    {
        this.ws = ws;
    }

    public Target getChat()
    {
        return chat;
    }

    public void setChat(Target chat)
    {
        this.chat = chat;
    }

    public int getIoCount()
    {
        return ioCount;
    }

    public void setIoCount(int ioCount)
    {
        this.ioCount = ioCount;
    }
}
