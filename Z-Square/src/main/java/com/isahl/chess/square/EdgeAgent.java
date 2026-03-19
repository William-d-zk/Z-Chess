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

package com.isahl.chess.square;

import com.isahl.chess.square.config.EdgeConfig;
import com.isahl.chess.square.scheduler.HeartbeatScheduler;
import com.isahl.chess.square.service.EdgeClient;
import com.isahl.chess.square.service.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(EdgeConfig.class)
@EnableScheduling
public class EdgeAgent implements CommandLineRunner {
  private static final Logger _Logger = LoggerFactory.getLogger(EdgeAgent.class);

  private final EdgeConfig _Config;
  private final EdgeClient _Client;
  private final TaskExecutor _Executor;
  private final HeartbeatScheduler _Heartbeat;

  @Autowired
  public EdgeAgent(
      EdgeConfig config, EdgeClient client, TaskExecutor executor, HeartbeatScheduler heartbeat) {
    _Config = config;
    _Client = client;
    _Executor = executor;
    _Heartbeat = heartbeat;
  }

  public static void main(String[] args) {
    SpringApplication.run(EdgeAgent.class, args);
  }

  @Override
  public void run(String... args) {
    _Logger.info(
        "Starting Z-Square Agent: nodeId={}, schedulerUrl={}",
        _Config.getNodeId(),
        _Config.getSchedulerUrl());
    _Heartbeat.start();
    _Client.startListening(_Executor);
    _Logger.info("Z-Square Agent started successfully");
  }
}
