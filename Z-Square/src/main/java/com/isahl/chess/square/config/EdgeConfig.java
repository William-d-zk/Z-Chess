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

package com.isahl.chess.square.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "z.chess.square")
public class EdgeConfig
{
    private String nodeId;
    private String schedulerUrl = "http://localhost:8080";
    private long heartbeatInterval = 30000;
    private int cpuCores;
    private long totalMemory;

    public void setNodeId(String nodeId)
    {
        this.nodeId = nodeId;
    }

    public String getNodeId()
    {
        return nodeId;
    }

    public String getSchedulerUrl()
    {
        return schedulerUrl;
    }

    public void setSchedulerUrl(String schedulerUrl)
    {
        this.schedulerUrl = schedulerUrl;
    }

    public long getHeartbeatInterval()
    {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(long heartbeatInterval)
    {
        this.heartbeatInterval = heartbeatInterval;
    }

    public int getCpuCores()
    {
        return cpuCores > 0 ? cpuCores : Runtime.getRuntime().availableProcessors();
    }

    public void setCpuCores(int cpuCores)
    {
        this.cpuCores = cpuCores;
    }

    public long getTotalMemory()
    {
        return totalMemory > 0 ? totalMemory : Runtime.getRuntime().maxMemory();
    }

    public void setTotalMemory(long totalMemory)
    {
        this.totalMemory = totalMemory;
    }
}