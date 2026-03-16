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

package com.isahl.chess.audience.king.metrics;

import com.isahl.chess.king.metrics.ZChessMetrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZChessMetricsTest {
    
    private ZChessMetrics metrics;
    private SimpleMeterRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ZChessMetrics(registry);
    }
    
    @Test
    void testRecordConnection() {
        metrics.recordConnection();
        
        var counter = registry.find("zchess_connections_total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }
    
    @Test
    void testRecordConnectionClosed() {
        metrics.recordConnection();
        metrics.recordConnectionClosed();
        
        var gauge = registry.find("zchess_connections_active").gauge();
        assertNotNull(gauge);
        assertEquals(0.0, gauge.value());
    }
    
    @Test
    void testRecordConnectionFailed() {
        metrics.recordConnectionFailed();
        metrics.recordConnectionFailed();
        
        var counter = registry.find("zchess_connections_failed_total").counter();
        assertNotNull(counter);
        assertEquals(2.0, counter.count());
    }
    
    @Test
    void testRecordMessagePublished() {
        metrics.recordMessagePublished();
        metrics.recordMessagePublished();
        metrics.recordMessagePublished();
        
        var counter = registry.find("zchess_mqtt_messages_published_total").counter();
        assertNotNull(counter);
        assertEquals(3.0, counter.count());
    }
    
    @Test
    void testRecordMessageReceived() {
        metrics.recordMessageReceived();
        metrics.recordMessageReceived();
        
        var counter = registry.find("zchess_mqtt_messages_received_total").counter();
        assertNotNull(counter);
        assertEquals(2.0, counter.count());
    }
    
    @Test
    void testRecordMessageDropped() {
        metrics.recordMessageDropped();
        
        var counter = registry.find("zchess_mqtt_messages_dropped_total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }
    
    @Test
    void testRecordLeaderChange() {
        metrics.recordLeaderChange();
        
        var counter = registry.find("zchess_raft_leader_changes_total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }
    
    @Test
    void testUpdateCommitIndex() {
        metrics.updateCommitIndex(100L);
        
        var gauge = registry.find("zchess_raft_commit_index").gauge();
        assertNotNull(gauge);
        assertEquals(100.0, gauge.value());
    }
    
    @Test
    void testElectionTimer() throws InterruptedException {
        Timer.Sample sample = metrics.startElectionTimer();
        Thread.sleep(10);
        metrics.recordElectionDuration(sample);
        
        var timer = registry.find("zchess_raft_election_duration_seconds").timer();
        assertNotNull(timer);
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) >= 10);
    }
}
