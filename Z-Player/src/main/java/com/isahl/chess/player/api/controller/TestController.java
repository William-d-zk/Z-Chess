package com.isahl.chess.player.api.controller;

import com.isahl.chess.player.api.service.AliothApiService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @RequestMapping("get-auth-info")
    public Object getAuthInfos(){
        return aliothApiService.fetchAuthInfos();
    }

    @RequestMapping("get-task")
    public Object fetchTask(){
        return aliothApiService.fetchUnfinishedTaskList();
    }

    @RequestMapping("update-task-status")
    public Object updateTaskStatus(@RequestParam(name = "taskId") Long taskId, @RequestParam(name = "status") String status){
        aliothApiService.updateTask(taskId,status);
        return "OK";
    }
}
