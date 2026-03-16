/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
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

package com.isahl.chess.king.metrics;

import io.micrometer.core.instrument.*;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Z-Chess 核心监控指标
 * 
 * @author william.d.zk
 */
public class ZChessMetrics
{
    
    private final MeterRegistry _registry;
    
    // 连接指标
    private final Counter _connectionsTotal;
    private final Counter _connectionsFailed;
    private final AtomicLong _connectionsActive;
    
    // MQTT 指标
    private final Counter _mqttMessagesPublished;
    private final Counter _mqttMessagesReceived;
    private final Counter _mqttMessagesDropped;
    
    // 集群指标
    private final Counter _raftLeaderChanges;
    private final AtomicLong _raftCommitIndex;
    private final Timer _raftElectionDuration;
    
    public ZChessMetrics(MeterRegistry registry)
    {
        this._registry = registry;
        
        // 连接指标
        _connectionsActive = new AtomicLong(0);
        _connectionsTotal = Counter.builder("zchess_connections_total")
                .description("Total number of connections")
                .register(_registry);
        _connectionsFailed = Counter.builder("zchess_connections_failed_total")
                .description("Number of failed connections")
                .register(_registry);
        Gauge.builder("zchess_connections_active", _connectionsActive, AtomicLong::get)
                .description("Number of active connections")
                .register(_registry);
        
        // MQTT 指标
        _mqttMessagesPublished = Counter.builder("zchess_mqtt_messages_published_total")
                .description("Total number of published MQTT messages")
                .register(_registry);
        _mqttMessagesReceived = Counter.builder("zchess_mqtt_messages_received_total")
                .description("Total number of received MQTT messages")
                .register(_registry);
        _mqttMessagesDropped = Counter.builder("zchess_mqtt_messages_dropped_total")
                .description("Number of dropped MQTT messages")
                .register(_registry);
        
        // 集群指标
        _raftCommitIndex = new AtomicLong(0);
        _raftLeaderChanges = Counter.builder("zchess_raft_leader_changes_total")
                .description("Total number of RAFT leader changes")
                .register(_registry);
        Gauge.builder("zchess_raft_commit_index", _raftCommitIndex, AtomicLong::get)
                .description("Current RAFT commit index")
                .register(_registry);
        _raftElectionDuration = Timer.builder("zchess_raft_election_duration_seconds")
                .description("RAFT election duration")
                .register(_registry);
        
        MetricsRegistry.initialize(registry);
    }
    
    // 连接指标方法
    
    public void recordConnection()
    {
        _connectionsTotal.increment();
        _connectionsActive.incrementAndGet();
    }
    
    public void recordConnectionClosed()
    {
        _connectionsActive.decrementAndGet();
    }
    
    public void recordConnectionFailed()
    {
        _connectionsFailed.increment();
    }
    
    // MQTT 指标方法
    
    public void recordMessagePublished()
    {
        _mqttMessagesPublished.increment();
    }
    
    public void recordMessageReceived()
    {
        _mqttMessagesReceived.increment();
    }
    
    public void recordMessageDropped()
    {
        _mqttMessagesDropped.increment();
    }
    
    // 集群指标方法
    
    public void recordLeaderChange()
    {
        _raftLeaderChanges.increment();
    }
    
    public void updateCommitIndex(long index)
    {
        _raftCommitIndex.set(index);
    }
    
    public Timer.Sample startElectionTimer()
    {
        return Timer.start(_registry);
    }
    
    public void recordElectionDuration(Timer.Sample sample)
    {
        sample.stop(_raftElectionDuration);
    }
}
