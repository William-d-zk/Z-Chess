package com.isahl.chess.player.api.config;

import java.time.Duration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

/**
 * @author xiaojiang.lxj at 2024-05-09 14:19.
 */
@PropertySource("classpath:player.properties")
@Configuration("player_config")
public class PlayerConfig implements ApplicationContextAware {

    @Value("${noco.api.base.url}")
    private String baseApi;

    @Value("${noco.api.visit.token}")
    private String nocoApiToken;

    public String getNocoApiToken() {
        return nocoApiToken;
    }

    public String getBaseApi() {
        return baseApi;
    }


    private static final int TIMEOUT = 5 * 1000;

    private RestTemplate restTemplate;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        restTemplate = applicationContext.getBean(RestTemplateBuilder.class)
            .setConnectTimeout(Duration.ofMillis(TIMEOUT))
            .setReadTimeout(Duration.ofMillis(TIMEOUT))
            .defaultHeader(HttpHeaders.AUTHORIZATION,"Bearer "+this.nocoApiToken)
            .build();
    }

    @Bean
    public RestTemplate restTemplate(){
        return this.restTemplate;
    }
}
