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

package com.isahl.chess.knight.scheduler.core;

import com.isahl.chess.knight.scheduler.domain.*;
import com.isahl.chess.knight.scheduler.repository.SubTaskRepository;
import com.isahl.chess.knight.scheduler.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ClaimScheduler
        implements TaskScheduler
{
    private static final Logger _Logger = LoggerFactory.getLogger(ClaimScheduler.class);

    private final TaskRepository _TaskRepository;
    private final SubTaskRepository _SubTaskRepository;
    private final TaskPool _TaskPool;
    private final ResultAggregator _ResultAggregator;

    public ClaimScheduler(TaskRepository taskRepository, SubTaskRepository subTaskRepository, TaskPool taskPool, ResultAggregator resultAggregator)
    {
        _TaskRepository = taskRepository;
        _SubTaskRepository = subTaskRepository;
        _TaskPool = taskPool;
        _ResultAggregator = resultAggregator;
    }

    @Override
    @Transactional
    public Task dispatchTask(String taskId, String payload, List<String> targetNodes, int timeoutSeconds)
    {
        Task task = new Task(taskId, TaskType.DISPATCH, timeoutSeconds);
        List<SubTask> subTasks = task.createSubTasksForNodes(targetNodes);

        for(SubTask st : subTasks) {
            st.setPayload(payload);
            _SubTaskRepository.save(st);
        }

        task.setStatus(TaskStatus.RUNNING);
        _TaskRepository.save(task);

        _Logger.info("DispatchScheduler: Created dispatch task {} for {} nodes", taskId, targetNodes.size());
        return task;
    }

    @Override
    @Transactional
    public Task claimTask(String taskId, String payload, int subTaskCount, int timeoutSeconds)
    {
        Task task = new Task(taskId, TaskType.CLAIM, timeoutSeconds);
        List<SubTask> subTasks = task.createSubTasks(subTaskCount);

        for(SubTask st : subTasks) {
            st.setPayload(payload);
            _SubTaskRepository.save(st);
            _TaskPool.addTask(st);
        }

        task.setStatus(TaskStatus.RUNNING);
        _TaskRepository.save(task);

        _Logger.info("ClaimScheduler: Created claim task {} with {} subTasks in pool", taskId, subTaskCount);
        return task;
    }

    @Override
    @Transactional
    public Optional<SubTask> claimSubTasks(String nodeId, int maxCount)
    {
        Optional<SubTask> claimed = _TaskPool.claim(nodeId, maxCount);
        if(claimed.isPresent()) {
            _SubTaskRepository.save(claimed.get());
            _Logger.info("Node {} claimed subTask {}", nodeId, claimed.get().getSubTaskId());
        }
        return claimed;
    }

    @Override
    @Transactional
    public void reportResult(String subTaskId, String result, boolean success)
    {
        SubTask subTask = _SubTaskRepository.findById(subTaskId).orElse(null);
        if(subTask == null) {
            _Logger.warn("SubTask {} not found for result report", subTaskId);
            return;
        }

        if(success) {
            subTask.markComplete(result);
        }
        else {
            subTask.markFailed();
            _TaskPool.release(subTask);
        }
        _SubTaskRepository.save(subTask);

        Task task = _TaskRepository.findById(subTask.getTaskId()).orElse(null);
        if(task != null) {
            task.addResult(subTaskId, result, success);
            if(_ResultAggregator.canComplete(task)) {
                List<TaskResult.SubTaskResultEntry> entries = task.getSubTasks()
                                                                   .stream()
                                                                   .map(st -> new TaskResult.SubTaskResultEntry(
                                                                           st.getSubTaskId(),
                                                                           st.getTargetNode(),
                                                                           st.getResult(),
                                                                           st.getStatus() == SubTaskStatus.COMPLETE))
                                                                   .collect(Collectors.toList());
                task.setAggregatedResult(_ResultAggregator.aggregate(entries));
                task.setStatus(TaskStatus.COMPLETE);
                _Logger.info("Task {} completed", task.getTaskId());
            }
            else if(task.hasFailures()) {
                task.setStatus(TaskStatus.PARTIAL_COMPLETE);
            }
            _TaskRepository.save(task);
        }
    }

    @Override
    public TaskStatus getTaskStatus(String taskId)
    {
        return _TaskRepository.findById(taskId).map(Task::getStatus).orElse(null);
    }

    @Override
    public TaskResult getTaskResult(String taskId)
    {
        return _TaskRepository.findById(taskId)
                             .map(task -> new TaskResult(
                                     task.getTaskId(),
                                     task.getStatus(),
                                     task.getAggregatedResult(),
                                     task.getSubTasks()
                                         .stream()
                                         .map(st -> new TaskResult.SubTaskResultEntry(
                                                 st.getSubTaskId(),
                                                 st.getTargetNode(),
                                                 st.getResult(),
                                                 st.getStatus() == SubTaskStatus.COMPLETE))
                                         .collect(Collectors.toList())
                             ))
                             .orElse(null);
    }
}
