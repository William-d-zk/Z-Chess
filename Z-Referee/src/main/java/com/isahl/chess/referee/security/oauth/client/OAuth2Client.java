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

package com.isahl.chess.referee.security.oauth.client;

import java.io.Serializable;
import java.time.Duration;

public class OAuth2Client
        implements
        Serializable
{
    private String thirdClientId;
    private String thirdClientSecret;
    private String scope;

    private Duration accessTokenValidate  = Duration.ofHours(2);
    private Duration refreshTokenValidity = Duration.ofSeconds(2592000);

    public String getThirdClientId()
    {
        return thirdClientId;
    }

    public void setThirdClientId(String thirdClientId)
    {
        this.thirdClientId = thirdClientId;
    }

    public String getThirdClientSecret()
    {
        return thirdClientSecret;
    }

    public void setThirdClientSecret(String thirdClientSecret)
    {
        this.thirdClientSecret = thirdClientSecret;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope(String scope)
    {
        this.scope = scope;
    }

    public Duration getAccessTokenValidate()
    {
        return accessTokenValidate;
    }

    public void setAccessTokenValidate(Duration accessTokenValidate)
    {
        this.accessTokenValidate = accessTokenValidate;
    }

    public Duration getRefreshTokenValidity()
    {
        return refreshTokenValidity;
    }

    public void setRefreshTokenValidity(Duration refreshTokenValidity)
    {
        this.refreshTokenValidity = refreshTokenValidity;
    }
}
