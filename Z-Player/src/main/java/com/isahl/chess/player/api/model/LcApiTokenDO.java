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

    /**
     * 安全注意: 此方法用于日志输出，已排除敏感字段(app_token, app_key)
     * 如需访问这些字段，请使用相应的 getter 方法
     */
    @Override
    public String toString() {
        return "LcApiTokenDO{" +
            "cat_id='" + cat_id + '\'' +
            ", company_code='" + company_code + '\'' +
            ", app_token='***'" +  // 敏感信息脱敏
            ", app_key='***'" +    // 敏感信息脱敏
            ", expire_time=" + expire_time +
            '}';
    }
}
