package com.isahl.chess.player.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * @author xiaojiang.lxj at 2024-09-18 15:08.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LcApiTokenDO {

    private String cat_id;

    private String company_code;

    private String app_token;

    private String app_key;

    private Instant expire_time;

    public String getApp_token() {
        return app_token;
    }

    public void setApp_token(String app_token) {
        this.app_token = app_token;
    }

    public String getApp_key() {
        return app_key;
    }

    public void setApp_key(String app_key) {
        this.app_key = app_key;
    }

    public Instant getExpire_time() {
        return expire_time;
    }

    public void setExpire_time(Instant expire_time) {
        this.expire_time = expire_time;
    }

    public String getCat_id() {
        return cat_id;
    }

    public void setCat_id(String cat_id) {
        this.cat_id = cat_id;
    }

    public String getCompany_code() {
        return company_code;
    }

    public void setCompany_code(String company_code) {
        this.company_code = company_code;
    }

    @Override
    public String toString() {
        return "LcApiTokenDO{" +
            "cat_id='" + cat_id + '\'' +
            ", company_code='" + company_code + '\'' +
            ", app_token='" + app_token + '\'' +
            ", app_key='" + app_key + '\'' +
            ", expire_time=" + expire_time +
            '}';
    }
}
