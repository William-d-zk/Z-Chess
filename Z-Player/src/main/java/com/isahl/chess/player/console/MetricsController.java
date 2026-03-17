/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.player.console;

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
