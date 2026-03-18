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

package com.isahl.chess.knight.engine;

import com.isahl.chess.knight.policy.Policy;
import com.isahl.chess.knight.scheduler.core.DispatchScheduler;
import com.isahl.chess.knight.scheduler.core.TaskScheduler;
import com.isahl.chess.knight.scheduler.domain.SubTask;
import com.isahl.chess.knight.scheduler.domain.Task;
import com.isahl.chess.knight.scheduler.domain.TaskResult;
import com.isahl.chess.knight.scheduler.domain.TaskStatus;
import com.isahl.chess.knight.scheduler.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DefaultSchedulerEngine
        implements SchedulerEngine
{
    private static final Logger _Logger = LoggerFactory.getLogger(DefaultSchedulerEngine.class);

    private final DispatchScheduler _DispatchScheduler;
    private final TaskScheduler _ClaimScheduler;
    private final TaskRepository _TaskRepository;
    private final Map<String, Policy> _Policies = new ConcurrentHashMap<>();

    @Autowired
    public DefaultSchedulerEngine(DispatchScheduler dispatchScheduler, TaskScheduler claimScheduler, TaskRepository taskRepository)
    {
        _DispatchScheduler = dispatchScheduler;
        _ClaimScheduler = claimScheduler;
        _TaskRepository = taskRepository;
    }

    @Override
    public String submitTask(TaskContext context)
    {
        String taskType = context.getTaskType();
        List<String> targetNodes = null;
        int subTaskCount = 1;

        if(context instanceof DispatchTaskContext dispatch) {
            targetNodes = dispatch.getTargetNodes();
            _DispatchScheduler.dispatchTask(context.getTaskId(), context.getPayload(), targetNodes, context.getTimeoutSeconds());
        }
        else if(context instanceof ClaimTaskContext claim) {
            subTaskCount = claim.getSubTaskCount();
            _DispatchScheduler.claimTask(context.getTaskId(), context.getPayload(), subTaskCount, context.getTimeoutSeconds());
        }
        _Logger.info("Submitted task: id={}, type={}", context.getTaskId(), taskType);
        return context.getTaskId();
    }

    @Override
    public boolean cancelTask(String taskId)
    {
        TaskStatus status = getTaskStatus(taskId);
        if(status == null || status == TaskStatus.COMPLETE || status == TaskStatus.PARTIAL_COMPLETE) {
            return false;
        }
        _Logger.info("Cancelling task: {}", taskId);
        return true;
    }

    @Override
    public TaskStatus getTaskStatus(String taskId)
    {
        TaskStatus status = _DispatchScheduler.getTaskStatus(taskId);
        if(status == null) {
            status = _ClaimScheduler.getTaskStatus(taskId);
        }
        return status;
    }

    @Override
    public TaskResult getTaskResult(String taskId)
    {
        TaskResult result = _DispatchScheduler.getTaskResult(taskId);
        if(result == null) {
            result = _ClaimScheduler.getTaskResult(taskId);
        }
        return result;
    }

    @Override
    public Optional<SubTaskContext> claimSubTask(String nodeId, int maxCount)
    {
        Optional<SubTask> subTaskOpt = _ClaimScheduler.claimSubTasks(nodeId, maxCount);
        return subTaskOpt.map(st -> (SubTaskContext) new DefaultSubTaskContext(
                st.getSubTaskId(),
                st.getTaskId(),
                st.getPayload()
        ));
    }

    @Override
    public void reportResult(String subTaskId, String result, boolean success)
    {
        _ClaimScheduler.reportResult(subTaskId, result, success);
    }

    @Override
    public void registerPolicy(String taskType, Policy policy)
    {
        _Policies.put(taskType, policy);
        _Logger.info("Registered policy: taskType={}, policyId={}", taskType, policy.getPolicyId());
    }

    @Override
    public Policy getPolicy(String taskType)
    {
        return _Policies.get(taskType);
    }

    @Override
    public Map<String, Long> getPendingTaskCount()
    {
        Map<String, Long> counts = new ConcurrentHashMap<>();
        _TaskRepository.findAll()
                       .forEach(task -> {
                           TaskStatus status = task.getStatus();
                           counts.merge(status != null ? status.name() : "UNKNOWN", 1L, Long::sum);
                       });
        return counts;
    }

    public static class DefaultTaskContext
            implements TaskContext
    {
        private final String taskId;
        private final String taskType;
        private final String payload;
        private final int timeoutSeconds;

        public DefaultTaskContext(String taskId, String taskType, String payload, int timeoutSeconds)
        {
            this.taskId = taskId;
            this.taskType = taskType;
            this.payload = payload;
            this.timeoutSeconds = timeoutSeconds;
        }

        @Override
        public String getTaskId()
        {
            return taskId;
        }

        @Override
        public String getTaskType()
        {
            return taskType;
        }

        @Override
        public String getPayload()
        {
            return payload;
        }

        @Override
        public int getTimeoutSeconds()
        {
            return timeoutSeconds;
        }
    }

    public static class DispatchTaskContext
            extends DefaultTaskContext
    {
        private final List<String> targetNodes;

        public DispatchTaskContext(String taskId, String taskType, String payload, int timeoutSeconds, List<String> targetNodes)
        {
            super(taskId, taskType, payload, timeoutSeconds);
            this.targetNodes = targetNodes;
        }

        public List<String> getTargetNodes()
        {
            return targetNodes;
        }
    }

    public static class ClaimTaskContext
            extends DefaultTaskContext
    {
        private final int subTaskCount;

        public ClaimTaskContext(String taskId, String taskType, String payload, int timeoutSeconds, int subTaskCount)
        {
            super(taskId, taskType, payload, timeoutSeconds);
            this.subTaskCount = subTaskCount;
        }

        public int getSubTaskCount()
        {
            return subTaskCount;
        }
    }

    public static class DefaultSubTaskContext
            implements SubTaskContext
    {
        private final String subTaskId;
        private final String taskId;
        private final String payload;

        public DefaultSubTaskContext(String subTaskId, String taskId, String payload)
        {
            this.subTaskId = subTaskId;
            this.taskId = taskId;
            this.payload = payload;
        }

        @Override
        public String getSubTaskId()
        {
            return subTaskId;
        }

        @Override
        public String getTaskId()
        {
            return taskId;
        }

        @Override
        public String getPayload()
        {
            return payload;
        }
    }
}
