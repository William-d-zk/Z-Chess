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

package com.isahl.chess.knight.raft.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.util.LongToDataSizeConverter;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RaftConfig
{
    private Map<Integer, String> peers;
    private Map<Integer, String> nodes;
    private Map<Long, String>    gates;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Duration             electInSecond;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Duration             snapshotInSecond;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Duration             heartbeatInSecond;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Duration             clientSubmitInSecond;
    @JsonDeserialize(converter = LongToDataSizeConverter.class)
    private DataSize             snapshotMinSize;
    @JsonDeserialize(converter = LongToDataSizeConverter.class)
    private DataSize             snapshotFragmentMaxSize;
    @JsonDeserialize(converter = LongToDataSizeConverter.class)
    private DataSize             maxSegmentSize;
    private IRaftConfig.Uid      uid;
    private boolean              beLearner;

    public Map<Integer, String> getPeers()
    {
        return peers;
    }

    public void setPeers(Map<Integer, String> peers)
    {
        this.peers = peers;
    }

    public Map<Integer, String> getNodes()
    {
        return nodes;
    }

    public void setNodes(Map<Integer, String> nodes)
    {
        this.nodes = nodes;
    }

    public Map<Long, String> getGates()
    {
        return gates;
    }

    public void setGates(Map<Long, String> gates)
    {
        this.gates = gates;
    }

    public Duration getElectInSecond()
    {
        return electInSecond;
    }

    public void setElectInSecond(Duration electInSecond)
    {
        this.electInSecond = electInSecond;
    }

    public Duration getSnapshotInSecond()
    {
        return snapshotInSecond;
    }

    public void setSnapshotInSecond(Duration snapshotInSecond)
    {
        this.snapshotInSecond = snapshotInSecond;
    }

    public Duration getHeartbeatInSecond()
    {
        return heartbeatInSecond;
    }

    public void setHeartbeatInSecond(Duration heartbeatInSecond)
    {
        this.heartbeatInSecond = heartbeatInSecond;
    }

    public Duration getClientSubmitInSecond()
    {
        return clientSubmitInSecond;
    }

    public void setClientSubmitInSecond(Duration clientSubmitInSecond)
    {
        this.clientSubmitInSecond = clientSubmitInSecond;
    }

    public long getSnapshotMinSize()
    {
        return snapshotMinSize.toBytes();
    }

    public void setSnapshotMinSize(DataSize snapshotMinSize)
    {
        this.snapshotMinSize = snapshotMinSize;
    }

    public long getSnapshotFragmentMaxSize()
    {
        return snapshotFragmentMaxSize.toBytes();
    }

    public void setSnapshotFragmentMaxSize(DataSize snapshotFragmentMaxSize)
    {
        this.snapshotFragmentMaxSize = snapshotFragmentMaxSize;
    }

    public long getMaxSegmentSize()
    {
        return maxSegmentSize.toBytes();
    }

    public void setMaxSegmentSize(DataSize maxSegmentSize)
    {
        this.maxSegmentSize = maxSegmentSize;
    }

    public IRaftConfig.Uid getUid()
    {
        return uid;
    }

    public void setUid(IRaftConfig.Uid uid)
    {
        this.uid = uid;
    }

    public boolean isBeLearner()
    {
        return beLearner;
    }

    public void setBeLearner(boolean beLearner)
    {
        this.beLearner = beLearner;
    }
}



