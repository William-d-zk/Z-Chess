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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.tgx.chess.queen.config.IMixConfig;

@Configuration
@ConfigurationProperties(prefix = "z.chess.mix")
@PropertySource("classpath:core.mix.properties")
public class CoreMixConfig
        implements
        IMixConfig
{
    private int decoderCount;
    private int encoderCount;
    private int logicCount;
    private int bizIoCount;
    private int clusterIoCount;
    private int aioQueuePower;
    private int clusterPower;
    private int localPower;
    private int linkPower;
    private int logicPower;
    private int errorPower;
    private int closerPower;

    @Override
    public int getDecoderCount()
    {
        return decoderCount;
    }

    @Override
    public int getEncoderCount()
    {
        return encoderCount;
    }

    @Override
    public int getLogicCount()
    {
        return logicCount;
    }

    @Override
    public int getBizIoCount()
    {
        return bizIoCount;
    }

    @Override
    public int getClusterIoCount()
    {
        return clusterIoCount;
    }

    @Override
    public int getAioQueuePower()
    {
        return aioQueuePower;
    }

    @Override
    public int getClusterPower()
    {
        return clusterPower;
    }

    @Override
    public int getLocalPower()
    {
        return localPower;
    }

    @Override
    public int getLinkPower()
    {
        return linkPower;
    }

    @Override
    public int getLogicPower()
    {
        return logicPower;
    }

    @Override
    public int getErrorPower()
    {
        return errorPower;
    }

    @Override
    public int getCloserPower()
    {
        return closerPower;
    }

    public void setDecoderCount(int decoderCount)
    {
        this.decoderCount = decoderCount;
    }

    public void setEncoderCount(int encoderCount)
    {
        this.encoderCount = encoderCount;
    }

    public void setLogicCount(int logicCount)
    {
        this.logicCount = logicCount;
    }

    public void setBizIoCount(int bizIoCount)
    {
        this.bizIoCount = bizIoCount;
    }

    public void setClusterIoCount(int clusterIoCount)
    {
        this.clusterIoCount = clusterIoCount;
    }

    public void setAioQueuePower(int aioQueuePower)
    {
        this.aioQueuePower = aioQueuePower;
    }

    public void setClusterPower(int clusterPower)
    {
        this.clusterPower = clusterPower;
    }

    public void setLocalPower(int localPower)
    {
        this.localPower = localPower;
    }

    public void setLinkPower(int linkPower)
    {
        this.linkPower = linkPower;
    }

    public void setLogicPower(int logicPower)
    {
        this.logicPower = logicPower;
    }

    public void setErrorPower(int errorPower)
    {
        this.errorPower = errorPower;
    }

    public void setCloserPower(int closerPower)
    {
        this.closerPower = closerPower;
    }

}
