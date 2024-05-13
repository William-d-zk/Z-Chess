package com.isahl.chess.player.api.config;

import com.isahl.chess.player.api.mqtt.MqttMessageReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * 订阅通道配置
 *
 * @author xiaojiang.lxj at 2024-05-09 15:33.
 */
@Configuration
public class MqttInboundConfiguration {

    @Autowired
    private MqttConfig mqttConfig;

    @Autowired
    private MqttPahoClientFactory factory;


    @Autowired
    private MqttMessageReceiver mqttMessageReceiver;


    @Bean
    public MessageProducer mqttInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(mqttConfig.getClientId(), factory, mqttConfig.getInputTopics().toArray(new String[0]));
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(0);
        adapter.setOutputChannel(mqttInBoundChannel());
        return adapter;
    }


    /**
     * 此处可以使用其他消息通道
     * Spring Integration默认的消息通道，它允许将消息发送给一个订阅者，然后阻碍发送直到消息被接收。
     *
     */
    @Bean
    public MessageChannel mqttInBoundChannel() {
        return  new DirectChannel();
    }


    /**
     * mqtt入站消息处理工具，对于指定消息入站通道接收到生产者生产的消息后处理消息的工具。
     *
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttInBoundChannel")
    public MessageHandler mqttMessageHandler() {
        return this.mqttMessageReceiver;
    }
}
