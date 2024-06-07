package com.isahl.chess.player.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author xiaojiang.lxj at 2024-05-09 14:19.
 */
@PropertySource("classpath:player.properties")
@Configuration("player_config")
public class PlayerConfig {

    @Value("${noco.api.base.url}")
    private String nocoApiBaseUrl;

    @Value("${noco.api.visit.token}")
    private String nocoApiToken;

    @Value("${bidding.rpa.api.url}")
    private String biddingRpaApiUrl;

    @Value("${bidding.task.booking.disable:true}")
    private Boolean disableBooking;

    @Value("${bidding.task.schedule}")
    private Boolean biddingTaskSchedule;

    public Boolean getBiddingTaskSchedule() {
        return biddingTaskSchedule;
    }

    public String getNocoApiToken() {
        return nocoApiToken;
    }

    public String getNocoApiBaseUrl() {
        return nocoApiBaseUrl;
    }

    public String getBiddingRpaApiUrl() {
        return biddingRpaApiUrl;
    }

    public Boolean getDisableBooking() {
        return disableBooking;
    }

    public static final int TIMEOUT = 5 * 1000;


}
