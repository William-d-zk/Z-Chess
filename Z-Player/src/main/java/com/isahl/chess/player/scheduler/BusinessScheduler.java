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

package com.isahl.chess.player.scheduler;

import com.isahl.chess.knight.engine.SchedulingRule;
import com.isahl.chess.knight.policy.Policy;
import com.isahl.chess.knight.scheduler.domain.TaskResult;
import com.isahl.chess.knight.scheduler.domain.TaskStatus;
import com.isahl.chess.knight.service.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BusinessScheduler
{
    private static final Logger _Logger = LoggerFactory.getLogger(BusinessScheduler.class);

    private final SchedulerService _SchedulerService;
    private final Map<String, TaskHandler> _TaskHandlers = new ConcurrentHashMap<>();

    @Autowired
    public BusinessScheduler(SchedulerService schedulerService)
    {
        _SchedulerService = schedulerService;
        registerDefaultHandlers();
    }

    private void registerDefaultHandlers()
    {
        registerHandler("DEVICE_CONTROL", new DeviceControlHandler());
        registerHandler("AI_INFERENCE", new AIInferenceHandler());
        registerHandler("REPORT_GENERATION", new ReportGenerationHandler());
    }

    public void registerHandler(String taskType, TaskHandler handler)
    {
        _TaskHandlers.put(taskType, handler);
        _Logger.info("Registered task handler: type={}", taskType);
    }

    public String submitDeviceControlTask(String payload, SchedulingRule.DispatchRule rule)
    {
        return _SchedulerService.submitTask("DEVICE_CONTROL", payload, rule);
    }

    public String submitAIInferenceTask(String payload, SchedulingRule.ClaimRule rule)
    {
        return _SchedulerService.submitTask("AI_INFERENCE", payload, rule);
    }

    public String submitReportTask(String payload, String groupId, SchedulingRule.GroupRule rule)
    {
        return _SchedulerService.submitGroupTask("REPORT_GENERATION", payload, groupId, rule);
    }

    public TaskStatus getTaskStatus(String taskId)
    {
        return _SchedulerService.getTaskStatus(taskId);
    }

    public TaskResult getTaskResult(String taskId)
    {
        return _SchedulerService.getTaskResult(taskId);
    }

    public void registerPolicy(String taskType, Policy policy)
    {
        _SchedulerService.registerPolicy(taskType, policy);
    }

    public Map<String, Long> getTaskMetrics()
    {
        return _SchedulerService.getTaskMetrics();
    }

    public interface TaskHandler
    {
        String getTaskType();

        Object processPayload(Object payload);

        Policy getDefaultPolicy();
    }

    private class DeviceControlHandler
            implements TaskHandler
    {
        @Override
        public String getTaskType()
        {
            return "DEVICE_CONTROL";
        }

        @Override
        public Object processPayload(Object payload)
        {
            return payload;
        }

        @Override
        public Policy getDefaultPolicy()
        {
            return null;
        }
    }

    private class AIInferenceHandler
            implements TaskHandler
    {
        @Override
        public String getTaskType()
        {
            return "AI_INFERENCE";
        }

        @Override
        public Object processPayload(Object payload)
        {
            return payload;
        }

        @Override
        public Policy getDefaultPolicy()
        {
            return null;
        }
    }

    private class ReportGenerationHandler
            implements TaskHandler
    {
        @Override
        public String getTaskType()
        {
            return "REPORT_GENERATION";
        }

        @Override
        public Object processPayload(Object payload)
        {
            return payload;
        }

        @Override
        public Policy getDefaultPolicy()
        {
            return null;
        }
    }
}
