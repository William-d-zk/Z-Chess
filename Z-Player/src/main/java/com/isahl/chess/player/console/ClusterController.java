/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.player.console;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 集群状态 REST API
 *
 * @author william.d.zk
 */
@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    Map<String, Object> status = new HashMap<>();
    status.put("clusterSize", 3);
    status.put("leaderId", "node-raft10");
    status.put("quorumSize", 2);
    status.put("commitIndex", 12345L);
    status.put("healthy", true);

    Map<String, String> nodes = new HashMap<>();
    nodes.put("node-raft10", "LEADER");
    nodes.put("node-raft11", "FOLLOWER");
    nodes.put("node-raft12", "FOLLOWER");
    status.put("nodes", nodes);

    return ResponseEntity.ok(status);
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> getHealth() {
    Map<String, String> health = new HashMap<>();
    health.put("status", "UP");
    health.put("timestamp", String.valueOf(System.currentTimeMillis()));
    return ResponseEntity.ok(health);
  }

  @GetMapping("/nodes")
  public ResponseEntity<Map<String, Object>> getNodes() {
    Map<String, Object> nodes = new HashMap<>();

    Map<String, Object> node1 = new HashMap<>();
    node1.put("host", "172.30.10.10");
    node1.put("port", 9000);
    node1.put("status", "LEADER");
    node1.put("lastHeartbeat", System.currentTimeMillis());

    Map<String, Object> node2 = new HashMap<>();
    node2.put("host", "172.30.10.11");
    node2.put("port", 9000);
    node2.put("status", "FOLLOWER");
    node2.put("lastHeartbeat", System.currentTimeMillis());

    Map<String, Object> node3 = new HashMap<>();
    node3.put("host", "172.30.10.12");
    node3.put("port", 9000);
    node3.put("status", "FOLLOWER");
    node3.put("lastHeartbeat", System.currentTimeMillis());

    nodes.put("node-raft10", node1);
    nodes.put("node-raft11", node2);
    nodes.put("node-raft12", node3);

    return ResponseEntity.ok(nodes);
  }
}
