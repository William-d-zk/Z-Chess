package com.isahl.chess.player.api.service;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.player.api.model.RpaTaskMessageDO;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * 处理拍仓rpa发送的mqtt消息
 *
 * @author xiaojiang.lxj at 2024-05-10 10:31.
 */
@Service
public class BiddingRpaMessageService
{

    private final Logger log = Logger.getLogger("biz.player." + getClass().getSimpleName());

    private final AliothApiService aliothApiService;

    private final BiddingRpaScheduleService biddingRpaScheduleService;

    public BiddingRpaMessageService(AliothApiService aliothApiService,
        BiddingRpaScheduleService biddingRpaScheduleService)
    {
        this.aliothApiService = aliothApiService;
        this.biddingRpaScheduleService = biddingRpaScheduleService;
    }

    /**
     * 处理rpa发送的结果状态消息
     *
     * @param msg
     */
    public void processRpaMessage(String msg)
    {
        try {
            RpaTaskMessageDO rpaMessage = JsonUtil.readValue(msg, RpaTaskMessageDO.class);
            log.info("收到rpa结果状态信息: " + rpaMessage);
            if(!ObjectUtils.isEmpty(rpaMessage)) {
                if("售后".equals(rpaMessage.getStatus())){
                    // 表示rpa浏览器进程已关闭，需要重新触发rpa任务
                    log.info("收到rpa发送的[售后]状态消息，对任务id="+rpaMessage.getTaskId()+" 重新触发拍舱任务");
                    biddingRpaScheduleService.queryAndBooking(rpaMessage.getTaskId());
                }else{
                    aliothApiService.updateTask(rpaMessage);
                    log.info("rpa结果状态已更新: " + rpaMessage);
                }
            }
        }
        catch(Throwable t) {
            log.fetal("处理rpa发送的结果状态消息出现错误，msg = " + msg, t);
        }
    }
}
