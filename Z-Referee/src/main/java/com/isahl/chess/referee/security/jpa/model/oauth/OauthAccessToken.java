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
import com.isahl.chess.rook.storage.jpa.model.AuditModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author william.d.zk
 * @date 2021/3/5
 */
@Entity(name = "oauth_access_token")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Table(schema = "z_chess_security")
public class OauthAccessToken
        extends AuditModel
        implements Serializable

{
    @Serial
    private static final long serialVersionUID = 3470254183734664175L;

    public static int SERIAL_OAUTH_ACCESS_TOKEN = ClientDetail.SERIAL_CLIENT_DETAIL + 1;

    @Id
    @Column(name = "authentication_id")
    private String authenticationId;
    @Column(nullable = false)
    private byte[] authentication;
    @Column(nullable = false,
            name = "token_id")
    private String tokenId;
    @Column(nullable = false)
    private byte[] token;
    @Column(nullable = false)
    private String username;
    @Column(nullable = false,
            name = "client_id")
    private String clientId;
    @Column(name = "refresh_token")
    private String refreshToken;

    @Override
    public int serial()
    {
        return SERIAL_OAUTH_ACCESS_TOKEN;
    }

    public String getAuthenticationId()
    {
        return authenticationId;
    }

    public void setAuthenticationId(String authenticationId)
    {
        this.authenticationId = authenticationId;
    }
}
