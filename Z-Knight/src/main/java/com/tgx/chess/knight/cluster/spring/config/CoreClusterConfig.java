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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.knight.cluster.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.tgx.chess.queen.config.IClusterConfig;

@Configuration("core_cluster_config")
@ConfigurationProperties(prefix = "z.chess.cluster")
@PropertySource("classpath:core.cluster.properties")
public class CoreClusterConfig
        implements
        IClusterConfig
{
    private int decoderCountPower;
    private int encoderCountPower;
    private int logicCountPower;
    private int clusterIoCountPower;
    private int aioQueueSizePower;
    private int clusterQueueSizePower;
    private int logicQueueSizePower;
    private int errorQueueSizePower;
    private int closerQueueSizePower;

    @Override
    public int getDecoderCountPower()
    {
        return decoderCountPower;
    }

    @Override
    public int getEncoderCountPower()
    {
        return encoderCountPower;
    }

    @Override
    public int getLogicCountPower()
    {
        return logicCountPower;
    }

    @Override
    public int getClusterIoCountPower()
    {
        return clusterIoCountPower;
    }

    @Override
    public int getAioQueueSizePower()
    {
        return aioQueueSizePower;
    }

    @Override
    public int getClusterQueueSizePower()
    {
        return clusterQueueSizePower;
    }

    @Override
    public int getLogicQueueSizePower()
    {
        return logicQueueSizePower;
    }

    @Override
    public int getErrorQueueSizePower()
    {
        return errorQueueSizePower;
    }

    @Override
    public int getCloserQueueSizePower()
    {
        return closerQueueSizePower;
    }

    public void setDecoderCountPower(int decoderCountPower)
    {
        this.decoderCountPower = decoderCountPower;
    }

    public void setEncoderCountPower(int encoderCountPower)
    {
        this.encoderCountPower = encoderCountPower;
    }

    public void setLogicCountPower(int logicCountPower)
    {
        this.logicCountPower = logicCountPower;
    }

    public void setClusterIoCountPower(int clusterIoCountPower)
    {
        this.clusterIoCountPower = clusterIoCountPower;
    }

    public void setAioQueueSizePower(int aioQueueSizePower)
    {
        this.aioQueueSizePower = aioQueueSizePower;
    }

    public void setClusterQueueSizePower(int clusterQueueSizePower)
    {
        this.clusterQueueSizePower = clusterQueueSizePower;
    }

    public void setLogicQueueSizePower(int logicQueueSizePower)
    {
        this.logicQueueSizePower = logicQueueSizePower;
    }

    public void setErrorQueueSizePower(int errorQueueSizePower)
    {
        this.errorQueueSizePower = errorQueueSizePower;
    }

    public void setCloserQueueSizePower(int closerQueueSizePower)
    {
        this.closerQueueSizePower = closerQueueSizePower;
    }

}
