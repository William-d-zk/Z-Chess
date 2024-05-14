package com.isahl.chess.player.api.controller;

import com.isahl.chess.player.api.model.RpaAuthDo;
import com.isahl.chess.player.api.model.RpaTaskDO;
import com.isahl.chess.player.api.service.AliothApiService;
import com.isahl.chess.player.api.service.BiddingRpaScheduleService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xiaojiang.lxj at 2024-05-10 14:54.
 */
@RestController
@RequestMapping("test")
public class TestController {

    @Autowired
    private AliothApiService aliothApiService;

    @Autowired
    private BiddingRpaScheduleService biddingRpaScheduleService;

    @GetMapping("get-auth-info")
    public List<RpaAuthDo> getAuthInfos(){
        return aliothApiService.fetchAuthInfos();
    }

    @GetMapping("get-task")
    public List<RpaTaskDO> fetchTask(){
        return aliothApiService.fetchUnfinishedTaskList();
    }

    @GetMapping("update-task-status")
    public Object updateTaskStatus(@RequestParam(name = "taskId") Long taskId, @RequestParam(name = "status") String status){
        aliothApiService.updateTask(taskId,status);
        return "OK";
    }

    @GetMapping("trigger-bidding-task")
    public Object triggerBiddingTask(){
        biddingRpaScheduleService.queryAndBooking();
        return "OK";
    }
}
