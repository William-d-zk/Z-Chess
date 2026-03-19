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

@Entity
@Table(name = "scheduler_sub_task")
public class SubTask {
  @Id private String subTaskId;

  @Column(nullable = false)
  private String taskId;

  @Column private String targetNode;

  @Column(length = 4096)
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubTaskStatus status;

  @Column(length = 4096)
  private String result;

  @Column(nullable = false)
  private int retryCount;

  @Column private Instant startedAt;

  @Column private Instant completedAt;

  public SubTask() {}

  public SubTask(String taskId, String targetNode, String payload) {
    this.subTaskId = java.util.UUID.randomUUID().toString();
    this.taskId = taskId;
    this.targetNode = targetNode;
    this.payload = payload;
    this.status = SubTaskStatus.PENDING;
    this.retryCount = 0;
  }

  public void markRunning() {
    this.status = SubTaskStatus.RUNNING;
    this.startedAt = Instant.now();
  }

  public void markComplete(String result) {
    this.status = SubTaskStatus.COMPLETE;
    this.result = result;
    this.completedAt = Instant.now();
  }

  public void markFailed() {
    this.status = SubTaskStatus.FAILED;
    this.completedAt = Instant.now();
  }

  public String getSubTaskId() {
    return subTaskId;
  }

  public String getTaskId() {
    return taskId;
  }

  public String getTargetNode() {
    return targetNode;
  }

  public void setTargetNode(String targetNode) {
    this.targetNode = targetNode;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public SubTaskStatus getStatus() {
    return status;
  }

  public String getResult() {
    return result;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }
}
