package com.isahl.chess.player.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * @author xiaojiang.lxj at 2024-05-10 10:46.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
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

    /**
     * 订舱成功单号
     */
    private Long orderId;

    /**
     * 订舱成功时的价格
     */
    private Double orderPrice;

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

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Double getOrderPrice() {
        return orderPrice;
    }

    public void setOrderPrice(Double orderPrice) {
        this.orderPrice = orderPrice;
    }

    @Override
    public String toString() {
        return "RpaTaskMessageDO{" +
            "taskId=" + taskId +
            ", status='" + status + '\'' +
            ", orderId=" + orderId +
            ", orderPrice=" + orderPrice +
            '}';
    }
}
