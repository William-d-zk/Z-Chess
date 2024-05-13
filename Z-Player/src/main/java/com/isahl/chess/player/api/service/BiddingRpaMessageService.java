package com.isahl.chess.player.api.service;

import com.alibaba.fastjson2.JSONObject;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.player.api.model.RpaTaskMessageDO;
import org.springframework.stereotype.Service;

/**
 * 处理拍仓rpa发送的mqtt消息
 *
 * @author xiaojiang.lxj at 2024-05-10 10:31.
 */
@Service
public class BiddingRpaMessageService {

    private final Logger log = Logger.getLogger("biz.player." + getClass().getSimpleName());


    /**
     * 处理rpa发送的结果状态消息
     *
     * @param msg
     */
    public void processRpaMessage(String msg){
        try {
            RpaTaskMessageDO rpaMessage = JSONObject.parseObject(msg,RpaTaskMessageDO.class);

        }catch (Throwable t){
            log.fetal("处理rpa发送的结果状态消息出现错误，msg = "+msg,t);
        }
    }
}
