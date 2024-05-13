package com.isahl.chess.player.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author xiaojiang.lxj at 2024-05-10 11:49.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RpaAuthDo {
    private Long id;

    private String auth_username;

    private String auth_password;

    private String auth_config_title;

    private String auth_config_notice;

    private String auth_url;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuth_username() {
        return auth_username;
    }

    public void setAuth_username(String auth_username) {
        this.auth_username = auth_username;
    }

    public String getAuth_password() {
        return auth_password;
    }

    public void setAuth_password(String auth_password) {
        this.auth_password = auth_password;
    }

    public String getAuth_config_title() {
        return auth_config_title;
    }

    public void setAuth_config_title(String auth_config_title) {
        this.auth_config_title = auth_config_title;
    }

    public String getAuth_config_notice() {
        return auth_config_notice;
    }

    public void setAuth_config_notice(String auth_config_notice) {
        this.auth_config_notice = auth_config_notice;
    }

    public String getAuth_url() {
        return auth_url;
    }

    public void setAuth_url(String auth_url) {
        this.auth_url = auth_url;
    }

    @Override
    public String toString() {
        return "RpaAuthDo{" +
            "id=" + id +
            ", auth_username='" + auth_username + '\'' +
            ", auth_password='" + auth_password + '\'' +
            ", auth_config_title='" + auth_config_title + '\'' +
            ", auth_config_notice='" + auth_config_notice + '\'' +
            ", auth_url='" + auth_url + '\'' +
            '}';
    }
}
