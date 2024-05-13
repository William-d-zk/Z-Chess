package com.isahl.chess.player.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author xiaojiang.lxj at 2024-05-10 15:21.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class NocoSingleRecordResponse<T> {

    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
