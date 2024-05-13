package com.isahl.chess.player.api.config;

import java.util.List;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;

/**
 * @author xiaojiang.lxj at 2024-05-09 15:22.
 */
@Configuration
@PropertySource("classpath:mqtt.properties")
@ConfigurationProperties(prefix = "spring.mqtt")
public class MqttConfig {

    /**
     * 用户名
     */
    private String username;


    /**
     * 密码
     */
    private String password;


    /**
     * mqtt server url
     */
    private String url;


    /**
     * 客户端ID
     */
    private String clientId;


    private Integer completionTimeout = 2000;

    /**
     * 订阅topic
     */
    private List<String> inputTopics;



    /**
     *  注册MQTT客户端工厂
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(0);
        options.setKeepAliveInterval(20);
        options.setAutomaticReconnect(true);
        options.setUserName(this.getUsername());
        options.setPassword(this.getPassword().toCharArray());
        options.setServerURIs(new String[]{this.getUrl()});
        factory.setConnectionOptions(options);
        return factory;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Integer getCompletionTimeout() {
        return completionTimeout;
    }

    public void setCompletionTimeout(Integer completionTimeout) {
        this.completionTimeout = completionTimeout;
    }

    public List<String> getInputTopics() {
        return inputTopics;
    }

    public void setInputTopics(List<String> inputTopics) {
        this.inputTopics = inputTopics;
    }

}
