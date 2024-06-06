package com.isahl.chess.player.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author xiaojiang.lxj at 2024-05-10 10:46.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RpaTaskMessageDO {

    public RpaTaskMessageDO() {
    }

    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 任务状态
     */
    private String status;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "RpaTaskMessageDO{" +
            "taskId=" + taskId +
            ", status='" + status + '\'' +
            '}';
    }
}
