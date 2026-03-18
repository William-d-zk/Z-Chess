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

package com.isahl.chess.square.model;

import java.time.Instant;

public class NodeInfo
{
    private String nodeId;
    private int cpuCores;
    private long totalMemory;
    private long freeMemory;
    private String status;
    private Instant timestamp;

    public NodeInfo()
    {
        this.timestamp = Instant.now();
    }

    public NodeInfo(String nodeId, int cpuCores, long totalMemory, long freeMemory, String status)
    {
        this.nodeId = nodeId;
        this.cpuCores = cpuCores;
        this.totalMemory = totalMemory;
        this.freeMemory = freeMemory;
        this.status = status;
        this.timestamp = Instant.now();
    }

    public static NodeInfo create(String nodeId)
    {
        Runtime runtime = Runtime.getRuntime();
        return new NodeInfo(
                nodeId,
                runtime.availableProcessors(),
                runtime.maxMemory(),
                runtime.freeMemory(),
                "ONLINE"
        );
    }

    public String getNodeId()
    {
        return nodeId;
    }

    public void setNodeId(String nodeId)
    {
        this.nodeId = nodeId;
    }

    public int getCpuCores()
    {
        return cpuCores;
    }

    public void setCpuCores(int cpuCores)
    {
        this.cpuCores = cpuCores;
    }

    public long getTotalMemory()
    {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory)
    {
        this.totalMemory = totalMemory;
    }

    public long getFreeMemory()
    {
        return freeMemory;
    }

    public void setFreeMemory(long freeMemory)
    {
        this.freeMemory = freeMemory;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Instant getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp)
    {
        this.timestamp = timestamp;
    }
}