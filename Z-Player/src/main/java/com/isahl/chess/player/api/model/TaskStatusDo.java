package com.isahl.chess.player.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author xiaojiang.lxj at 2024-05-10 15:19.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TaskStatusDo {

    /**
     * 状态记录id
     */
    private Long id;

    /**
     * 状态值
     */
    private String notice;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }
}
