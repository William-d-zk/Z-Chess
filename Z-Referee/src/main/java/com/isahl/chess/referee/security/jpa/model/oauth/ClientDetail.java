/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.isahl.chess.referee.security.jpa.model.oauth;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.referee.security.jpa.model.PermissionEntity;
import com.isahl.chess.rook.storage.jpa.model.AuditModel;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author william.d.zk
 * @date 2021/3/5
 */

@Entity
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ClientDetail
        extends
        AuditModel
        implements
        Serializable
{
    @Serial
    private static final long serialVersionUID = -3639493262081429501L;

    public static int SERIAL_CLIENT_DETAIL = PermissionEntity.SERIAL_PERMISSION + 1;

    @Id
    private String   appId;
    @Type(type = "string-array")
    @Column(name = "resource_ids", columnDefinition = "text[]")
    private String[] resourceIds       = {};
    @Column(nullable = false)
    private String   appSecret;
    @Column(nullable = false)
    private String   scope;
    @Type(type = "string-array")
    @Column(name = "grant_types", columnDefinition = "text[]")
    private String[] grantTypes;
    @Column(nullable = false)
    private String   redirectUrl;
    @Type(type = "string-array")
    @Column(name = "authorities", columnDefinition = "text[]")
    private String[] authorities       = {};
    private boolean  accessTokenValidity;
    private boolean  refreshTokenValidity;
    @Column(nullable = false)
    private byte[]   additionalInformation;
    @Type(type = "string-array")
    @Column(name = "auto_approve_scopes", columnDefinition = "text[]")
    private String[] autoApproveScopes = {};

    @Override
    public int serial()
    {
        return SERIAL_CLIENT_DETAIL;
    }

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String[] getResourceIds()
    {
        return resourceIds;
    }

    public void setResourceIds(String[] resourceIds)
    {
        this.resourceIds = resourceIds;
    }

    public String getAppSecret()
    {
        return appSecret;
    }

    public void setAppSecret(String appSecret)
    {
        this.appSecret = appSecret;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope(String scope)
    {
        this.scope = scope;
    }

    public String[] getGrantTypes()
    {
        return grantTypes;
    }

    public void setGrantTypes(String[] grantTypes)
    {
        this.grantTypes = grantTypes;
    }

    public String getRedirectUrl()
    {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl)
    {
        this.redirectUrl = redirectUrl;
    }

    public String[] getAuthorities()
    {
        return authorities;
    }

    public void setAuthorities(String[] authorities)
    {
        this.authorities = authorities;
    }

    public byte[] getAdditionalInformation()
    {
        return additionalInformation;
    }

    public void setAdditionalInformation(byte[] additionalInformation)
    {
        this.additionalInformation = additionalInformation;
    }

    public String[] getAutoApproveScopes()
    {
        return autoApproveScopes;
    }

    public void setAutoApproveScopes(String[] autoApproveScopes)
    {
        this.autoApproveScopes = autoApproveScopes;
    }

    public boolean isAccessTokenValidity()
    {
        return accessTokenValidity;
    }

    public void setAccessTokenValidity(boolean accessTokenValidity)
    {
        this.accessTokenValidity = accessTokenValidity;
    }

    public boolean isRefreshTokenValidity()
    {
        return refreshTokenValidity;
    }

    public void setRefreshTokenValidity(boolean refreshTokenValidity)
    {
        this.refreshTokenValidity = refreshTokenValidity;
    }
}
