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

package com.isahl.chess.square.service;

import com.isahl.chess.square.config.EdgeConfig;
import com.isahl.chess.square.model.TaskResult;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class EdgeClient {
  private static final Logger _Logger = LoggerFactory.getLogger(EdgeClient.class);

  private final EdgeConfig _Config;
  private final RestTemplate _RestTemplate;
  private final ExecutorService _PollExecutor;
  private volatile boolean _Running;

  @Autowired
  public EdgeClient(EdgeConfig config) {
    _Config = config;
    _RestTemplate = new RestTemplate();
    _PollExecutor = Executors.newSingleThreadExecutor();
  }

  public void startListening(TaskExecutor executor) {
    _Running = true;
    _PollExecutor.submit(
        () -> {
          while (_Running) {
            try {
              claimAndExecute(executor);
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              break;
            } catch (Exception e) {
              _Logger.error("Error in task polling loop", e);
              try {
                Thread.sleep(5000);
              } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
              }
            }
          }
        });
    _Logger.info("EdgeClient started listening for tasks");
  }

  public void stopListening() {
    _Running = false;
    _PollExecutor.shutdown();
    try {
      if (!_PollExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
        _PollExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      _PollExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private void claimAndExecute(TaskExecutor executor) {
    try {
      String url =
          String.format(
              "%s/api/scheduler/tasks/%s/claim?nodeId=%s&maxCount=1",
              _Config.getSchedulerUrl(), "claim", _Config.getNodeId());
      ResponseEntity<Map> response = _RestTemplate.postForEntity(url, null, Map.class);
      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        Map<?, ?> body = response.getBody();
        if (body.containsKey("subTaskId")) {
          String subTaskId = String.valueOf(body.get("subTaskId"));
          String taskId = String.valueOf(body.get("taskId"));
          String payload = body.get("payload") != null ? String.valueOf(body.get("payload")) : "";
          _Logger.info("Claimed task: subTaskId={}, taskId={}", subTaskId, taskId);
          executeAndReport(subTaskId, taskId, payload, executor);
        } else {
          _Logger.debug("No tasks available for claiming");
        }
      }
    } catch (Exception e) {
      _Logger.debug("Claim request failed: {}", e.getMessage());
    }
  }

  private void executeAndReport(
      String subTaskId, String taskId, String payload, TaskExecutor executor) {
    try {
      String result = executor.execute(payload);
      reportResult(subTaskId, taskId, result, true);
      _Logger.info("Task completed: subTaskId={}", subTaskId);
    } catch (Exception e) {
      _Logger.error("Task execution failed: subTaskId={}", subTaskId, e);
      reportResult(subTaskId, taskId, e.getMessage(), false);
    }
  }

  public void reportResult(String subTaskId, String taskId, String result, boolean success) {
    try {
      String url = _Config.getSchedulerUrl() + "/api/scheduler/results";
      TaskResult taskResult = new TaskResult(subTaskId, taskId, result, success);
      _RestTemplate.postForEntity(url, taskResult, Map.class);
      _Logger.debug("Result reported: subTaskId={}, success={}", subTaskId, success);
    } catch (Exception e) {
      _Logger.error("Failed to report result: subTaskId={}", subTaskId, e);
    }
  }

  public <T> T postForObject(String path, Object request, Class<T> responseType) {
    String url = _Config.getSchedulerUrl() + path;
    return _RestTemplate.postForObject(url, request, responseType);
  }

  public <T> ResponseEntity<T> getForEntity(String path, Class<T> responseType) {
    String url = _Config.getSchedulerUrl() + path;
    return _RestTemplate.getForEntity(url, responseType);
  }
}
