package com.isahl.chess.player.api.subscribe;

import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.pawn.endpoint.device.db.central.model.MessageEntity;
import com.isahl.chess.player.api.component.BusinessPlugin.IBusinessSubscribe;
import com.isahl.chess.player.api.service.BiddingRpaMessageService;
import org.springframework.stereotype.Component;

/**
 * 美航订舱rpa消息处理
 *
 * @author xiaojiang.lxj at 2024-06-05 15:37.
 */
@Component
public class MeihangBiddingRpaSubscribe implements IBusinessSubscribe {
    private final Logger log = Logger.getLogger("biz.player." + getClass().getSimpleName());

    private static final String TOPIC_BIDDING_RPA = "bidding_rpa";

    private final BiddingRpaMessageService biddingRpaMessageService;

    public MeihangBiddingRpaSubscribe(
        BiddingRpaMessageService biddingRpaMessageService) {
        this.biddingRpaMessageService = biddingRpaMessageService;
    }

    @Override
    public void onMessage(IoSerial content) {
        if(content instanceof MessageEntity msg){
            log.info("[business process]接收到mqtt消息: "+msg);
            if(TOPIC_BIDDING_RPA.equals(msg.getTopic())){
                try{
                    biddingRpaMessageService.processRpaMessage(msg.getContent());
                }catch (Throwable t){
                    log.fetal("receive mqtt message encounter exception. topic="+TOPIC_BIDDING_RPA,t);
                }
            }
        }
    }
}
