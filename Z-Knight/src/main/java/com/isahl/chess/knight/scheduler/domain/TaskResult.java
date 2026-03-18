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

import java.util.List;

public class TaskResult
{
    private String taskId;
    private TaskStatus status;
    private String aggregatedResult;
    private List<SubTaskResultEntry> subTaskResults;

    public TaskResult()
    {
    }

    public TaskResult(String taskId, TaskStatus status, String aggregatedResult, List<SubTaskResultEntry> subTaskResults)
    {
        this.taskId = taskId;
        this.status = status;
        this.aggregatedResult = aggregatedResult;
        this.subTaskResults = subTaskResults;
    }

    public String getTaskId()
    {
        return taskId;
    }

    public TaskStatus getStatus()
    {
        return status;
    }

    public String getAggregatedResult()
    {
        return aggregatedResult;
    }

    public List<SubTaskResultEntry> getSubTaskResults()
    {
        return subTaskResults;
    }

    public static class SubTaskResultEntry
    {
        private String subTaskId;
        private String nodeId;
        private String result;
        private boolean success;

        public SubTaskResultEntry()
        {
        }

        public SubTaskResultEntry(String subTaskId, String nodeId, String result, boolean success)
        {
            this.subTaskId = subTaskId;
            this.nodeId = nodeId;
            this.result = result;
            this.success = success;
        }

        public String getSubTaskId()
        {
            return subTaskId;
        }

        public String getNodeId()
        {
            return nodeId;
        }

        public String getResult()
        {
            return result;
        }

        public boolean isSuccess()
        {
            return success;
        }
    }
}
