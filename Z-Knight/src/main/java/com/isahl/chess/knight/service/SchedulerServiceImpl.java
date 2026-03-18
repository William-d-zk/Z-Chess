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

import com.isahl.chess.knight.engine.DefaultSchedulerEngine;
import com.isahl.chess.knight.engine.SchedulingRule;
import com.isahl.chess.knight.engine.SchedulerEngine;
import com.isahl.chess.knight.policy.Policy;
import com.isahl.chess.knight.scheduler.domain.TaskResult;
import com.isahl.chess.knight.scheduler.domain.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchedulerServiceImpl
        implements SchedulerService
{
    private static final Logger _Logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

    private final SchedulerEngine _Engine;
    private final Map<String, List<SchedulingRule>> _Rules = new ConcurrentHashMap<>();

    @Autowired
    public SchedulerServiceImpl(SchedulerEngine engine)
    {
        _Engine = engine;
    }

    @Override
    public String submitTask(String taskType, String payload, SchedulingRule rule)
    {
        String taskId = UUID.randomUUID().toString();
        SchedulerEngine.TaskContext context;

        if(rule instanceof SchedulingRule.DispatchRule dispatch) {
            context = new DefaultSchedulerEngine.DispatchTaskContext(
                    taskId, taskType, payload, 3600, dispatch.getTargetNodes());
        }
        else {
            context = new DefaultSchedulerEngine.DefaultTaskContext(taskId, taskType, payload, 3600);
        }

        _Engine.submitTask(context);
        _Logger.info("Submitted task: taskId={}, taskType={}", taskId, taskType);
        return taskId;
    }

    @Override
    public String submitGroupTask(String taskType, String payload, String groupId, SchedulingRule rule)
    {
        String taskId = UUID.randomUUID().toString();
        _Logger.info("Submitted group task: taskId={}, taskType={}, groupId={}", taskId, taskType, groupId);
        return taskId;
    }

    @Override
    public boolean cancelTask(String taskId)
    {
        return _Engine.cancelTask(taskId);
    }

    @Override
    public TaskStatus getTaskStatus(String taskId)
    {
        return _Engine.getTaskStatus(taskId);
    }

    @Override
    public TaskResult getTaskResult(String taskId)
    {
        return _Engine.getTaskResult(taskId);
    }

    @Override
    public void registerPolicy(String taskType, Policy policy)
    {
        _Engine.registerPolicy(taskType, policy);
    }

    @Override
    public Policy getPolicy(String taskType, String policyType)
    {
        Policy policy = _Engine.getPolicy(taskType);
        if(policy != null && policy.getPolicyType().equals(policyType)) {
            return policy;
        }
        return null;
    }

    @Override
    public List<SchedulingRule> getRules(String taskType)
    {
        return _Rules.getOrDefault(taskType, List.of());
    }

    @Override
    public void registerRule(String taskType, SchedulingRule rule)
    {
        _Rules.computeIfAbsent(taskType, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(rule);
        _Logger.info("Registered rule: taskType={}, ruleId={}", taskType, rule.getRuleId());
    }

    @Override
    public Map<String, Long> getTaskMetrics()
    {
        return _Engine.getPendingTaskCount();
    }

    @Override
    public Map<String, Integer> getNodeMetrics()
    {
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("onlineNodes", 0);
        metrics.put("pendingTasks", 0);
        return metrics;
    }
}
