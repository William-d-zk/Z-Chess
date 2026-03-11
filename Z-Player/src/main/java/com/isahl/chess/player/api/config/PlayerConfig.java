/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
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

package com.isahl.chess.player.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Player 模块配置类
 */
@PropertySource("classpath:player.properties")
@Configuration("player_config")
public class PlayerConfig {

    @Value("${bidding.rpa.api.url}")
    private String biddingRpaApiUrl;

    @Value("${bidding.cancel.api.url}")
    private String cancelRpaApiUrl;

    @Value("${bidding.task.booking.disable:true}")
    private Boolean disableBooking;

    public String getBiddingRpaApiUrl() {
        return biddingRpaApiUrl;
    }

    public String getCancelRpaApiUrl() {
        return cancelRpaApiUrl;
    }

    public Boolean getDisableBooking() {
        return disableBooking;
    }

    public static final int TIMEOUT = 5 * 1000;

    public static final int SLEEP_INTERVAL = 30 * 1000;
}
