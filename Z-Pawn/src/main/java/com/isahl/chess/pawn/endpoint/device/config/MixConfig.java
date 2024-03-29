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

package com.isahl.chess.pawn.endpoint.device.config;

import com.isahl.chess.king.base.cron.TimeWheel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.time.Duration;
import java.util.List;

@PropertySource("classpath:pawn.mix.properties")
@ConfigurationProperties(prefix = "z.chess.pawn.mix")
@Configuration("pawn_mix_config")
public class MixConfig
{
    public static class Server
    {
        String host;
        int    port;
        String scheme;

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

        public String getScheme()
        {
            return scheme;
        }

        public void setScheme(String scheme)
        {
            this.scheme = scheme;
        }
    }

    private       List<Server> listeners;
    private       Duration     passwordInvalidDays;
    private       String       passwordRandomSeed;
    private       boolean      multiBind;
    private final TimeWheel    _TimeWheel = new TimeWheel();

    public List<Server> getListeners()
    {
        return listeners;
    }

    public void setListeners(List<Server> listeners)
    {
        this.listeners = listeners;
    }

    public Duration getPasswordInvalidDays()
    {
        return passwordInvalidDays;
    }

    public void setPasswordInvalidDays(Duration passwordInvalidDays)
    {
        this.passwordInvalidDays = passwordInvalidDays;
    }

    public String getPasswordRandomSeed()
    {
        return passwordRandomSeed;
    }

    public void setPasswordRandomSeed(String passwordRandomSeed)
    {
        this.passwordRandomSeed = passwordRandomSeed;
    }

    public boolean isMultiBind()
    {
        return multiBind;
    }

    public void setMultiBind(boolean multiBind)
    {
        this.multiBind = multiBind;
    }

    @Bean
    public TimeWheel getTimeWheel()
    {
        return _TimeWheel;
    }
}
