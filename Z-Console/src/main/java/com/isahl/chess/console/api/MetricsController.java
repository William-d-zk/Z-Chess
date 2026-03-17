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

package com.isahl.chess.console.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 监控指标 REST API
 * 
 * @author william.d.zk
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricsController
{
    
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary()
    {
        Map<String, Object> metrics = new HashMap<>();
        
        Map<String, Object> connections = new HashMap<>();
        connections.put("active", 1250);
        connections.put("total", 50000);
        connections.put("failed", 12);
        metrics.put("connections", connections);
        
        Map<String, Object> mqtt = new HashMap<>();
        mqtt.put("messagesPublished", 1000000);
        mqtt.put("messagesReceived", 950000);
        mqtt.put("messagesDropped", 50);
        metrics.put("mqtt", mqtt);
        
        Map<String, Object> cluster = new HashMap<>();
        cluster.put("leaderChanges", 3);
        cluster.put("commitIndex", 12345L);
        metrics.put("cluster", cluster);
        
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/jvm")
    public ResponseEntity<Map<String, Object>> getJvmMetrics()
    {
        Map<String, Object> jvm = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        jvm.put("maxMemory", runtime.maxMemory());
        jvm.put("totalMemory", runtime.totalMemory());
        jvm.put("freeMemory", runtime.freeMemory());
        jvm.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        jvm.put("availableProcessors", runtime.availableProcessors());
        
        return ResponseEntity.ok(jvm);
    }
}
