package com.isahl.chess.player.api.mqtt;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.player.api.service.BiddingRpaMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

/**
 * @author xiaojiang.lxj at 2024-05-09 15:37.
 */
@Component
public class MqttMessageReceiver implements MessageHandler {
    private final Logger log = Logger.getLogger("biz.player." + getClass().getSimpleName());

    private static final String TOPIC_BIDDING_RPA = "bidding_rpa";

    @Autowired
    private BiddingRpaMessageService biddingRpaMessageService;

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        try {
            MessageHeaders headers = message.getHeaders();
            //获取消息Topic
            String receivedTopic = (String) headers.get(MqttHeaders.RECEIVED_TOPIC);
            log.info("获取到的消息的topic:\n %s", receivedTopic);
            //获取消息体
            String payload = (String) message.getPayload();
            log.info("获取到的消息的payload:\n %s", payload);
            // 处理bidding_rpa消息
            if(TOPIC_BIDDING_RPA.equals(receivedTopic)){
                biddingRpaMessageService.processRpaMessage(payload);
            }
        } catch (Throwable t) {
            log.fetal("receive mqtt message encounter exception. topic="+message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC),t);
        }
    }
}
