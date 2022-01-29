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
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.rook.storage.db.model.AuditModel;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serial;

/**
 * @author william.d.zk
 * @date 2021/3/5
 */

@Entity(name = "client_detail")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Table(schema = "z_chess_security")
@ISerialGenerator(parent = IProtocol.STORAGE_ROOK_DB_SERIAL)
public class ClientDetail
        extends AuditModel
{
    @Serial
    private static final long serialVersionUID = -3639493262081429501L;

    @Transient
    private String   mAppId;
    @Transient
    private String[] mResourceIds       = {};
    @Transient
    private String   mAppSecret;
    @Transient
    private String   mScope;
    @Transient
    private String[] mGrantTypes;
    @Transient
    private String   mRedirectUrl;
    @Transient
    private String[] mAuthorities       = {};
    @Transient
    private boolean  mAccessTokenValidity;
    @Transient
    private boolean  mRefreshTokenValidity;
    @Transient
    private byte[]   mAdditionalInformation;
    @Transient
    private String[] mAutoApproveScopes = {};

    @Id
    public String getAppId()
    {
        return mAppId;
    }

    @Type(type = "string-array")
    @Column(name = "resource_ids",
            columnDefinition = "text[]")
    public String[] getResourceIds()
    {
        return mResourceIds;
    }

    @Column(nullable = false,
            name = "app_secret")
    public String getAppSecret()
    {
        return mAppSecret;
    }

    @Column(nullable = false)
    public String getScope()
    {
        return mScope;
    }

    @Type(type = "string-array")
    @Column(name = "grant_types",
            columnDefinition = "text[]")
    public String[] getGrantTypes()
    {
        return mGrantTypes;
    }

    @Column(nullable = false,
            name = "redirect_url")
    public String getRedirectUrl()
    {
        return mRedirectUrl;
    }

    @Type(type = "string-array")
    @Column(name = "authorities",
            columnDefinition = "text[]")
    public String[] getAuthorities()
    {
        return mAuthorities;
    }

    @Column(nullable = false,
            name = "additional_information")
    @Lob
    @Type(type = "org.hibernate.type.BinaryType")
    public byte[] getAdditionalInformation()
    {
        return mAdditionalInformation;
    }

    @Type(type = "string-array")
    @Column(name = "auto_approve_scopes",
            columnDefinition = "text[]")
    public String[] getAutoApproveScopes()
    {
        return mAutoApproveScopes;
    }

    @Column(name = "refresh_token_validity")
    public boolean isRefreshTokenValidity()
    {
        return mRefreshTokenValidity;
    }

    @Column(name = "access_token_validity")
    public boolean isAccessTokenValidity()
    {
        return mAccessTokenValidity;
    }

    public void setAppId(String appId)
    {
        mAppId = appId;
    }

    public void setResourceIds(String[] resourceIds)
    {
        mResourceIds = resourceIds;
    }

    public void setAppSecret(String appSecret)
    {
        mAppSecret = appSecret;
    }

    public void setScope(String scope)
    {
        mScope = scope;
    }

    public void setGrantTypes(String[] grantTypes)
    {
        mGrantTypes = grantTypes;
    }

    public void setRedirectUrl(String redirectUrl)
    {
        mRedirectUrl = redirectUrl;
    }

    public void setAuthorities(String[] authorities)
    {
        mAuthorities = authorities;
    }

    public void setAdditionalInformation(byte[] additionalInformation)
    {
        mAdditionalInformation = additionalInformation;
    }

    public void setAutoApproveScopes(String[] autoApproveScopes)
    {
        mAutoApproveScopes = autoApproveScopes;
    }

    public void setAccessTokenValidity(boolean accessTokenValidity)
    {
        mAccessTokenValidity = accessTokenValidity;
    }

    public void setRefreshTokenValidity(boolean refreshTokenValidity)
    {
        mRefreshTokenValidity = refreshTokenValidity;
    }

    public ClientDetail()
    {
        super();
    }

    public ClientDetail(ByteBuf input)
    {
        super(input);
    }
}
