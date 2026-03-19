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

import com.isahl.chess.knight.scheduler.domain.SubTaskStatus;
import com.isahl.chess.knight.scheduler.domain.Task;
import com.isahl.chess.knight.scheduler.repository.SubTaskRepository;
import com.isahl.chess.knight.scheduler.repository.TaskRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TimeoutChecker {
  private static final Logger _Logger = LoggerFactory.getLogger(TimeoutChecker.class);

  private final TaskRepository _TaskRepository;
  private final SubTaskRepository _SubTaskRepository;
  private final RetryScheduler _RetryScheduler;

  public TimeoutChecker(
      TaskRepository taskRepository,
      SubTaskRepository subTaskRepository,
      RetryScheduler retryScheduler) {
    _TaskRepository = taskRepository;
    _SubTaskRepository = subTaskRepository;
    _RetryScheduler = retryScheduler;
  }

  @Scheduled(fixedRate = 5000)
  @Transactional
  public void checkTimeouts() {
    Instant now = Instant.now();
    List<Task> runningTasks =
        _TaskRepository.findByStatus(com.isahl.chess.knight.scheduler.domain.TaskStatus.RUNNING);

    for (Task task : runningTasks) {
      if (task.getDeadline() != null && task.getDeadline().isBefore(now)) {
        _Logger.warn("Task {} has exceeded deadline, handling timeout", task.getTaskId());
        handleTaskTimeout(task);
      }
    }
  }

  private void handleTaskTimeout(Task task) {
    task.getSubTasks().stream()
        .filter(st -> st.getStatus() == SubTaskStatus.RUNNING)
        .forEach(
            st -> {
              _Logger.warn("SubTask {} timed out, triggering timeout retry", st.getSubTaskId());
              _RetryScheduler.handleFailure(
                  st.getSubTaskId(), st.getTargetNode(), RetryStrategy.TIMEOUT_RETRY);
            });
  }
}
