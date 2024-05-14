package com.isahl.chess.player.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author xiaojiang.lxj at 2024-05-10 11:26.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RpaTaskDO {

    /**
     * 任务id
     */
    private Long task_id;

    /**
     * 离港日期 e.g. 2024-05-30T16:00:00.000Z
     */
    private String depart_date;

    /**
     * 提箱日期 e.g. 2024-05-25T16:00:00.000Z
     */
    private String pickup_date;

    /**
     * 拍仓限价
     */
    private Double maxprice;

    /**
     * 集装箱个数
     */
    private Integer cabin_amount;

    /**
     * 货物重量 kg
     */
    private Integer cargo_weight;

    /**
     * 货物类型
     */
    private String cargo_type;

    /**
     * 集装箱类型
     */
    private String container_type;

    /**
     * 起运地
     */
    private String pickup_place;

    /**
     * 起运地别名title
     */
    private String pickup_place_alias_title;

    /**
     * 起运地别名
     */
    private String pickup_place_alias;

    /**
     * 目的地
     */
    private String destination_place;

    /**
     * 目的地别名title
     */
    private String destination_place_alias_title;

    /**
     * 目的地别名
     */
    private String destination_place_alias;

    /**
     * 任务状态
     */
    private String task_status;

    public String getDepart_date() {
        return depart_date;
    }

    public void setDepart_date(String depart_date) {
        this.depart_date = depart_date;
    }

    public String getPickup_date() {
        return pickup_date;
    }

    public void setPickup_date(String pickup_date) {
        this.pickup_date = pickup_date;
    }

    public Double getMaxprice() {
        return maxprice;
    }

    public void setMaxprice(Double maxprice) {
        this.maxprice = maxprice;
    }

    public Integer getCabin_amount() {
        return cabin_amount;
    }

    public void setCabin_amount(Integer cabin_amount) {
        this.cabin_amount = cabin_amount;
    }

    public Integer getCargo_weight() {
        return cargo_weight;
    }

    public void setCargo_weight(Integer cargo_weight) {
        this.cargo_weight = cargo_weight;
    }

    public String getCargo_type() {
        return cargo_type;
    }

    public void setCargo_type(String cargo_type) {
        this.cargo_type = cargo_type;
    }

    public String getContainer_type() {
        return container_type;
    }

    public void setContainer_type(String container_type) {
        this.container_type = container_type;
    }

    public String getPickup_place() {
        return pickup_place;
    }

    public void setPickup_place(String pickup_place) {
        this.pickup_place = pickup_place;
    }

    public String getPickup_place_alias_title() {
        return pickup_place_alias_title;
    }

    public void setPickup_place_alias_title(String pickup_place_alias_title) {
        this.pickup_place_alias_title = pickup_place_alias_title;
    }

    public String getPickup_place_alias() {
        return pickup_place_alias;
    }

    public void setPickup_place_alias(String pickup_place_alias) {
        this.pickup_place_alias = pickup_place_alias;
    }

    public String getDestination_place() {
        return destination_place;
    }

    public void setDestination_place(String destination_place) {
        this.destination_place = destination_place;
    }

    public String getDestination_place_alias_title() {
        return destination_place_alias_title;
    }

    public void setDestination_place_alias_title(String destination_place_alias_title) {
        this.destination_place_alias_title = destination_place_alias_title;
    }

    public String getDestination_place_alias() {
        return destination_place_alias;
    }

    public void setDestination_place_alias(String destination_place_alias) {
        this.destination_place_alias = destination_place_alias;
    }

    public String getTask_status() {
        return task_status;
    }

    public void setTask_status(String task_status) {
        this.task_status = task_status;
    }

    public Long getTask_id() {
        return task_id;
    }

    public void setTask_id(Long task_id) {
        this.task_id = task_id;
    }

    @Override
    public String toString() {
        return "RpaTaskDO{" +
            "task_id=" + task_id +
            ", depart_date=" + depart_date +
            ", pickup_date=" + pickup_date +
            ", maxprice=" + maxprice +
            ", cabin_amount=" + cabin_amount +
            ", cargo_weight=" + cargo_weight +
            ", cargo_type='" + cargo_type + '\'' +
            ", container_type='" + container_type + '\'' +
            ", pickup_place='" + pickup_place + '\'' +
            ", pickup_place_alias_title='" + pickup_place_alias_title + '\'' +
            ", pickup_place_alias='" + pickup_place_alias + '\'' +
            ", destination_place='" + destination_place + '\'' +
            ", destination_place_alias_title='" + destination_place_alias_title + '\'' +
            ", destination_place_alias='" + destination_place_alias + '\'' +
            ", task_status='" + task_status + '\'' +
            '}';
    }
}
