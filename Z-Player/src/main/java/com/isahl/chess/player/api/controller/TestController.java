package com.isahl.chess.player.api.controller;

import com.isahl.chess.player.api.model.LcApiListResponse;
import com.isahl.chess.player.api.model.RpaAuthDo;
import com.isahl.chess.player.api.model.RpaTaskDO;
import com.isahl.chess.player.api.model.RpaTaskMessageDO;
import com.isahl.chess.player.api.service.AliothApiService;
import com.isahl.chess.player.api.service.BiddingRpaScheduleService;
import com.isahl.chess.player.api.service.LcApiService;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
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

    private ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Autowired
    private AliothApiService aliothApiService;

    @Autowired
    private LcApiService lcApiService;

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
    public Object updateTaskStatus(@RequestParam(name = "taskId") String taskId, @RequestParam(name = "status") String status){
        for(String tid : taskId.split(",")){
            RpaTaskMessageDO message = new RpaTaskMessageDO();
            message.setTaskId(Long.parseLong(tid));
            message.setStatus(status);
            aliothApiService.updateTask(message);
        }

        return "OK";
    }

    @GetMapping("trigger-bidding-task")
    public Object triggerBiddingTask(@RequestParam(name = "taskId",required = false) Long taskId){
        Long tid;
        if(ObjectUtils.isEmpty(taskId)){
            tid = null;
        }else{
            tid = taskId;
        }
        executorService.submit(() -> biddingRpaScheduleService.queryAndBooking(tid));
        return "OK";
    }

    @GetMapping("cancel-bidding-task")
    public Object cancelBiddingTask(@RequestParam(name = "taskId",required = false) Long taskId){
        Long tid;
        if(ObjectUtils.isEmpty(taskId)){
            tid = null;
        }else{
            tid = taskId;
        }
        executorService.submit(() -> biddingRpaScheduleService.cancelBooking(tid));
        return "OK";
    }

    @GetMapping("get-lc-api-token")
    public Object getLcApiToken(){
        return aliothApiService.fetchLcAppTokenList();
    }

    @GetMapping("get-lc-order-list")
    public Object getLcOrderList(@RequestParam(name = "appToken") String appToken,@RequestParam(name = "appKey") String appKey,@RequestParam(name = "page",defaultValue = "1")Integer page,@RequestParam(name = "pageSize",defaultValue = "1") Integer pageSize){
        return lcApiService.fetchOrderList(appToken,appKey,page,pageSize);
    }

    @GetMapping("save-lc-order-list")
    public Object getAndSaveLcOrderList(@RequestParam(name = "appToken") String appToken,@RequestParam(name = "appKey") String appKey,@RequestParam(name = "page",defaultValue = "1")Integer page,@RequestParam(name = "pageSize",defaultValue = "1") Integer pageSize){
        LcApiListResponse response = lcApiService.fetchOrderList(appToken,appKey,page,pageSize);
        lcApiService.saveOrderList(response.getData());
        return "OK";
    }

    @GetMapping("import-lc-order-list")
    public Object importLcOrderList(@RequestParam(name = "appToken") String appToken,@RequestParam(name = "appKey") String appKey){
        executorService.submit(() -> lcApiService.importOrderListFromLc(appToken,appKey));
        return "OK";
    }
}
