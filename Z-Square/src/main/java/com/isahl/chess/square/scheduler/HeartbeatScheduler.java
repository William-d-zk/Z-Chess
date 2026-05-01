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

package com.isahl.chess.square.scheduler;

import com.isahl.chess.square.config.EdgeConfig;
import com.isahl.chess.square.model.NodeInfo;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HeartbeatScheduler {
  private static final Logger _Logger = LoggerFactory.getLogger(HeartbeatScheduler.class);

  private final EdgeConfig _Config;
  private final RestTemplate _RestTemplate;
  private volatile boolean _Running;

  @Autowired
  public HeartbeatScheduler(EdgeConfig config, RestTemplateBuilder restTemplateBuilder) {
    _Config = config;
    _RestTemplate =
        restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build();
    _Running = false;
  }

  public void start() {
    _Running = true;
    _Logger.info(
        "HeartbeatScheduler started: nodeId={}, interval={}ms",
        _Config.getNodeId(),
        _Config.getHeartbeatInterval());
  }

  public void stop() {
    _Running = false;
    _Logger.info("HeartbeatScheduler stopped");
  }

  @Scheduled(fixedRateString = "${z.chess.square.heartbeat-interval:30000}")
  public void sendHeartbeat() {
    if (!_Running) {
      return;
    }
    try {
      NodeInfo nodeInfo = NodeInfo.create(_Config.getNodeId());
      String url = _Config.getSchedulerUrl() + "/api/scheduler/nodes/heartbeat";
      ResponseEntity<Void> response = _RestTemplate.postForEntity(url, nodeInfo, Void.class);
      _Logger.debug(
          "Heartbeat sent: nodeId={}, status={}", _Config.getNodeId(), response.getStatusCode());
    } catch (Exception e) {
      _Logger.warn("Heartbeat failed: nodeId={}, error={}", _Config.getNodeId(), e.getMessage());
    }
  }
}
