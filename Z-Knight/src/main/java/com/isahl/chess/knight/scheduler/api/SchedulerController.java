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

package com.isahl.chess.knight.scheduler.api;

import com.isahl.chess.king.base.content.ZResponse;
import com.isahl.chess.knight.scheduler.core.TaskScheduler;
import com.isahl.chess.knight.scheduler.domain.SubTask;
import com.isahl.chess.knight.scheduler.domain.Task;
import com.isahl.chess.knight.scheduler.domain.TaskResult;
import com.isahl.chess.knight.scheduler.domain.TaskStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/scheduler")
public class SchedulerController {
  private static final Logger _Logger = LoggerFactory.getLogger(SchedulerController.class);

  private final TaskScheduler _DispatchScheduler;
  private final TaskScheduler _ClaimScheduler;

  @Autowired
  public SchedulerController(TaskScheduler dispatchScheduler, TaskScheduler claimScheduler) {
    _DispatchScheduler = dispatchScheduler;
    _ClaimScheduler = claimScheduler;
  }

  @PostMapping("tasks/dispatch")
  public ZResponse<?> dispatchTask(@RequestBody DispatchRequest request) {
    if (request.taskId == null || request.taskId.isEmpty()) {
      request.taskId = UUID.randomUUID().toString();
    }
    Task task =
        _DispatchScheduler.dispatchTask(
            request.taskId, request.payload, request.targetNodes, request.timeoutSeconds);
    _Logger.info("Created dispatch task: {}", task.getTaskId());
    return ZResponse.success(
        Map.of(
            "taskId", task.getTaskId(),
            "status", task.getStatus(),
            "subTaskCount", task.getSubTasks().size()));
  }

  @PostMapping("tasks/claim")
  public ZResponse<?> claimTask(@RequestBody ClaimRequest request) {
    if (request.taskId == null || request.taskId.isEmpty()) {
      request.taskId = UUID.randomUUID().toString();
    }
    Task task =
        _ClaimScheduler.claimTask(
            request.taskId, request.payload, request.subTaskCount, request.timeoutSeconds);
    _Logger.info("Created claim task: {}", task.getTaskId());
    return ZResponse.success(
        Map.of(
            "taskId", task.getTaskId(),
            "status", task.getStatus(),
            "subTaskCount", task.getSubTasks().size()));
  }

  @PostMapping("tasks/{taskId}/claim")
  public ZResponse<?> nodeClaim(
      @PathVariable String taskId,
      @RequestParam String nodeId,
      @RequestParam(defaultValue = "1") int maxCount) {
    Optional<SubTask> claimed = _ClaimScheduler.claimSubTasks(nodeId, maxCount);
    if (claimed.isPresent()) {
      SubTask st = claimed.get();
      return ZResponse.success(
          Map.of(
              "subTaskId", st.getSubTaskId(),
              "taskId", st.getTaskId(),
              "payload", st.getPayload() != null ? st.getPayload() : ""));
    }
    return ZResponse.success("No tasks available");
  }

  @PostMapping("results")
  public ZResponse<?> reportResult(@RequestBody ResultRequest request) {
    _ClaimScheduler.reportResult(request.subTaskId, request.result, request.success);
    _Logger.info("Reported result for subTask {}: success={}", request.subTaskId, request.success);
    return ZResponse.success("Result recorded");
  }

  @GetMapping("tasks/{taskId}")
  public ZResponse<?> getTaskStatus(@PathVariable String taskId) {
    TaskStatus status = _DispatchScheduler.getTaskStatus(taskId);
    if (status == null) {
      status = _ClaimScheduler.getTaskStatus(taskId);
    }
    if (status == null) {
      return ZResponse.error("Task not found");
    }
    TaskResult result = _ClaimScheduler.getTaskResult(taskId);
    return ZResponse.success(
        Map.of(
            "taskId",
            taskId,
            "status",
            status,
            "result",
            result != null ? result.getAggregatedResult() : "",
            "progress",
            result != null ? result.getSubTaskResults().size() : 0));
  }

  public static class DispatchRequest {
    public String taskId;
    public String payload;
    public List<String> targetNodes;
    public int timeoutSeconds = 3600;
  }

  public static class ClaimRequest {
    public String taskId;
    public String payload;
    public int subTaskCount;
    public int timeoutSeconds = 3600;
  }

  public static class ResultRequest {
    public String subTaskId;
    public String result;
    public boolean success;
  }
}
