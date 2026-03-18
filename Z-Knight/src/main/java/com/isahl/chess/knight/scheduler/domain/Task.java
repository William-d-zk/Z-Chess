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

package com.isahl.chess.knight.scheduler.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "scheduler_task")
public class Task
{
    @Id
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @OneToMany(mappedBy = "taskId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SubTask> subTasks = new ArrayList<>();

    @Column(nullable = false)
    private int timeout;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant deadline;

    @Column(length = 4096)
    private String aggregatedResult;

    public Task()
    {
    }

    public Task(String taskId, TaskType type, int timeoutSeconds)
    {
        this.taskId = taskId;
        this.type = type;
        this.status = TaskStatus.PENDING;
        this.timeout = timeoutSeconds;
        this.createdAt = Instant.now();
        if(timeoutSeconds > 0) {
            this.deadline = createdAt.plusSeconds(timeoutSeconds);
        }
    }

    public List<SubTask> createSubTasks(int count)
    {
        List<SubTask> created = new ArrayList<>();
        for(int i = 0; i < count; i++) {
            SubTask subTask = new SubTask(taskId, null, null);
            subTasks.add(subTask);
            created.add(subTask);
        }
        return created;
    }

    public List<SubTask> createSubTasksForNodes(List<String> targetNodes)
    {
        List<SubTask> created = new ArrayList<>();
        for(String node : targetNodes) {
            SubTask subTask = new SubTask(taskId, node, null);
            subTasks.add(subTask);
            created.add(subTask);
        }
        return created;
    }

    public void addResult(String subTaskId, String result, boolean success)
    {
        for(SubTask st : subTasks) {
            if(st.getSubTaskId().equals(subTaskId)) {
                if(success) {
                    st.markComplete(result);
                }
                else {
                    st.markFailed();
                }
                break;
            }
        }
    }

    public boolean isComplete()
    {
        if(subTasks.isEmpty()) {
            return false;
        }
        return subTasks.stream().allMatch(st -> st.getStatus() == SubTaskStatus.COMPLETE);
    }

    public boolean hasFailures()
    {
        return subTasks.stream().anyMatch(st -> st.getStatus() == SubTaskStatus.FAILED);
    }

    public int getCompleteCount()
    {
        return (int) subTasks.stream().filter(st -> st.getStatus() == SubTaskStatus.COMPLETE).count();
    }

    public String getTaskId()
    {
        return taskId;
    }

    public TaskType getType()
    {
        return type;
    }

    public TaskStatus getStatus()
    {
        return status;
    }

    public List<SubTask> getSubTasks()
    {
        return subTasks;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public Instant getCreatedAt()
    {
        return createdAt;
    }

    public Instant getDeadline()
    {
        return deadline;
    }

    public String getAggregatedResult()
    {
        return aggregatedResult;
    }

    public void setStatus(TaskStatus status)
    {
        this.status = status;
    }

    public void setAggregatedResult(String aggregatedResult)
    {
        this.aggregatedResult = aggregatedResult;
    }
}
