package com.isahl.chess.player.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author xiaojiang.lxj at 2024-05-14 10:28.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BiddingRpaDO {

    /**
     * 用于查询的账户名
     */
    private String nickname;

    /**
     * 用于查询的密码
     */
    private String password;

    /**
     * 起运地
     */
    private String sendingAddress;

    /**
     * 目的地
     */
    private String deliveryAddress;

    /**
     * 货物类型
     */
    private String productName;

    /**
     * 集装箱类型
     */
    private String box;

    /**
     * 货物重量(kg)
     */
    private Integer weight;

    /**
     * 最早离港日期 e.g. 2024-05-30
     */
    private String date;

    /**
     * 最早提箱日期 e.g. 2024-06-28
     */
    private String startDate;

    /**
     * 竞拍限价
     */
    private Double maxPrice;

    /**
     * 集装箱个数
     */
    private Integer boxNum;

    /**
     * 用于订舱的账户名
     */
    private String orderNickname;

    /**
     * 用于订舱的密码
     */
    private String orderPassword;

    private Long taskId;

    public BiddingRpaDO() {
    }

    public BiddingRpaDO(String nickname, String password, String sendingAddress, String deliveryAddress,
        String productName, String box, Integer weight, String date, String startDate, Double maxPrice,
        Integer boxNum, String orderNickname, String orderPassword, Long taskId) {
        this.nickname = nickname;
        this.password = password;
        this.sendingAddress = sendingAddress;
        this.deliveryAddress = deliveryAddress;
        this.productName = productName;
        this.box = box;
        this.weight = weight;
        this.date = date;
        this.startDate = startDate;
        this.maxPrice = maxPrice;
        this.boxNum = boxNum;
        this.orderNickname = orderNickname;
        this.orderPassword = orderPassword;
        this.taskId = taskId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSendingAddress() {
        return sendingAddress;
    }

    public void setSendingAddress(String sendingAddress) {
        this.sendingAddress = sendingAddress;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBox() {
        return box;
    }

    public void setBox(String box) {
        this.box = box;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Integer getBoxNum() {
        return boxNum;
    }

    public void setBoxNum(Integer boxNum) {
        this.boxNum = boxNum;
    }

    public String getOrderNickname() {
        return orderNickname;
    }

    public void setOrderNickname(String orderNickname) {
        this.orderNickname = orderNickname;
    }

    public String getOrderPassword() {
        return orderPassword;
    }

    public void setOrderPassword(String orderPassword) {
        this.orderPassword = orderPassword;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
}
