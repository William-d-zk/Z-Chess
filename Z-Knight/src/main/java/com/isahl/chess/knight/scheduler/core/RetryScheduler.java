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

import com.isahl.chess.knight.scheduler.domain.SubTask;
import com.isahl.chess.knight.scheduler.domain.SubTaskStatus;
import com.isahl.chess.knight.scheduler.repository.SubTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RetryScheduler
{
    private static final Logger _Logger = LoggerFactory.getLogger(RetryScheduler.class);

    private final TaskScheduler _BaseScheduler;
    private final ConcurrentHashMap<String, RetryContext> _RetryContexts = new ConcurrentHashMap<>();

    public RetryScheduler(TaskScheduler baseScheduler)
    {
        _BaseScheduler = baseScheduler;
    }

    public void handleFailure(String subTaskId, String nodeId, RetryStrategy strategy)
    {
        RetryContext ctx = _RetryContexts.computeIfAbsent(subTaskId, k -> new RetryContext());

        switch(strategy) {
            case EDGE_RETRY -> handleEdgeRetry(subTaskId, ctx);
            case REDUNDANT_RETRY -> handleRedundantRetry(subTaskId, nodeId, ctx);
            case TIMEOUT_RETRY -> handleTimeoutRetry(subTaskId, ctx);
        }
    }

    private void handleEdgeRetry(String subTaskId, RetryContext ctx)
    {
        if(ctx.edgeRetryCount < ctx.config.maxRetries) {
            ctx.edgeRetryCount++;
            long delay = ctx.config.retryDelayMs * (long) Math.pow(2, ctx.edgeRetryCount - 1);
            _Logger.info("Edge retry {} for subTask {}, delay {}ms", ctx.edgeRetryCount, subTaskId, delay);
        }
        else {
            _Logger.warn("Max edge retries reached for subTask {}, switching to redundant retry", subTaskId);
            ctx.currentStrategy = RetryStrategy.REDUNDANT_RETRY;
        }
    }

    private void handleRedundantRetry(String subTaskId, String failedNodeId, RetryContext ctx)
    {
        if(ctx.redundantRetryCount < ctx.config.maxRetries) {
            ctx.redundantRetryCount++;
            _Logger.info("Redundant retry {} for subTask {} to different node", ctx.redundantRetryCount, subTaskId);
        }
        else {
            _Logger.error("Max redundant retries reached for subTask {}, marking as failed", subTaskId);
            _BaseScheduler.reportResult(subTaskId, "Max retries exceeded", false);
            _RetryContexts.remove(subTaskId);
        }
    }

    private void handleTimeoutRetry(String subTaskId, RetryContext ctx)
    {
        if(ctx.timeoutRetryCount < ctx.config.maxRetries) {
            ctx.timeoutRetryCount++;
            _Logger.info("Timeout retry {} for subTask {}", ctx.timeoutRetryCount, subTaskId);
        }
        else {
            _Logger.error("Max timeout retries reached for subTask {}", subTaskId);
            _BaseScheduler.reportResult(subTaskId, "Timeout exceeded", false);
            _RetryContexts.remove(subTaskId);
        }
    }

    public static class RetryContext
    {
        public int edgeRetryCount = 0;
        public int redundantRetryCount = 0;
        public int timeoutRetryCount = 0;
        public RetryStrategy currentStrategy = RetryStrategy.EDGE_RETRY;
        public RetryConfig config = new RetryConfig();
    }
}
