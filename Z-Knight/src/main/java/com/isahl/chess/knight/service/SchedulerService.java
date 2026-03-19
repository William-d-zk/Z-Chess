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

package com.isahl.chess.knight.service;

import com.isahl.chess.knight.engine.SchedulingRule;
import com.isahl.chess.knight.policy.Policy;
import com.isahl.chess.knight.scheduler.domain.TaskResult;
import com.isahl.chess.knight.scheduler.domain.TaskStatus;
import java.util.List;
import java.util.Map;

public interface SchedulerService {
  String submitTask(String taskType, String payload, SchedulingRule rule);

  String submitGroupTask(String taskType, String payload, String groupId, SchedulingRule rule);

  boolean cancelTask(String taskId);

  TaskStatus getTaskStatus(String taskId);

  TaskResult getTaskResult(String taskId);

  void registerPolicy(String taskType, Policy policy);

  Policy getPolicy(String taskType, String policyType);

  List<SchedulingRule> getRules(String taskType);

  void registerRule(String taskType, SchedulingRule rule);

  Map<String, Long> getTaskMetrics();

  Map<String, Integer> getNodeMetrics();

  interface DispatchRequest {
    String getTaskId();

    String getTaskType();

    String getPayload();

    List<String> getTargetNodes();

    int getTimeoutSeconds();

    String getRuleId();
  }

  interface ClaimRequest {
    String getTaskId();

    String getTaskType();

    String getPayload();

    int getSubTaskCount();

    int getTimeoutSeconds();

    String getRuleId();
  }

  interface ResultRequest {
    String getSubTaskId();

    String getResult();

    boolean isSuccess();
  }
}
