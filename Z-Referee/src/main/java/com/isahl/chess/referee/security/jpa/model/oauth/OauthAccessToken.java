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
import java.nio.charset.StandardCharsets;

import static com.isahl.chess.king.base.content.ByteBuf.vSizeOf;

/**
 * @author william.d.zk
 * @date 2021/3/5
 */
@Entity(name = "oauth_access_token")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Table(schema = "z_chess_security")
@ISerialGenerator(parent = IProtocol.STORAGE_ROOK_DB_SERIAL)
public class OauthAccessToken
        extends AuditModel

{
    @Serial
    private static final long serialVersionUID = 3470254183734664175L;

    @Transient
    private String mAuthenticationId;
    @Transient
    private byte[] mAuthentication;
    @Transient
    private String mTokenId;
    @Transient
    private byte[] mToken;
    @Transient
    private String mUsername;
    @Transient
    private String mClientId;
    @Transient
    private String mRefreshToken;

    public OauthAccessToken(ByteBuf input)
    {
        super(input);
    }

    public OauthAccessToken()
    {
        super();
    }

    @Id
    @Column(name = "authentication_id")
    public String getAuthenticationId()
    {
        return mAuthenticationId;
    }

    @Column(nullable = false)
    @Lob
    @Type(type = "org.hibernate.type.BinaryType")
    public byte[] getAuthentication()
    {
        return mAuthentication;
    }

    @Column(nullable = false,
            name = "token_id")
    public String getTokenId()
    {
        return mTokenId;
    }

    @Column(nullable = false)
    @Lob
    @Type(type = "org.hibernate.type.BinaryType")
    public byte[] getToken()
    {
        return mToken;
    }

    @Column(nullable = false)
    public String getUsername()
    {
        return mUsername;
    }

    @Column(nullable = false,
            name = "client_id")
    public String getClientId()
    {
        return mClientId;
    }

    @Column(name = "refresh_token")
    public String getRefreshToken()
    {
        return mRefreshToken;
    }

    public void setAuthenticationId(String authenticationId)
    {
        mAuthenticationId = authenticationId;
    }

    public void setAuthentication(byte[] authentication)
    {
        mAuthentication = authentication;
    }

    public void setTokenId(String tokenId)
    {
        mTokenId = tokenId;
    }

    public void setToken(byte[] token)
    {
        mToken = token;
    }

    public void setUsername(String username)
    {
        mUsername = username;
    }

    public void setClientId(String clientId)
    {
        mClientId = clientId;
    }

    public void setRefreshToken(String refreshToken)
    {
        mRefreshToken = refreshToken;
    }

    @Override
    public int length()
    {
        return super.length() + //
               vSizeOf(mAuthenticationId.getBytes(StandardCharsets.UTF_8).length) + // authentication-id.length
               vSizeOf(mAuthentication.length) + // authentication
               vSizeOf(mTokenId.getBytes(StandardCharsets.UTF_8).length) + // token-id.length
               vSizeOf(mToken.length) + // token.length
               vSizeOf(mUsername.getBytes(StandardCharsets.UTF_8).length) + // username.length
               vSizeOf(mClientId.getBytes(StandardCharsets.UTF_8).length) +  // client.length
               vSizeOf(mRefreshToken.getBytes(StandardCharsets.UTF_8).length); // refresh-token.length
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        int adl = input.vLength();
        mAuthenticationId = input.readUTF(adl);
        remain -= vSizeOf(adl);
        int al = input.vLength();
        mAuthentication = new byte[al];
        input.get(mAuthentication);
        remain -= al;
        int tdl = input.vLength();
        mTokenId = input.readUTF(tdl);
        remain -= tdl;
        int tl = input.vLength();
        mToken = new byte[tl];
        input.get(mToken);
        remain -= tl;
        int ul = input.vLength();
        mUsername = input.readUTF(ul);
        remain -= ul;
        int cdl = input.vLength();
        mClientId = input.readUTF(cdl);
        remain -= cdl;
        int rtl = input.vLength();
        mRefreshToken = input.readUTF(rtl);
        remain -= rtl;
        return remain;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putUTF(mAuthenticationId)
                    .vPut(mAuthentication)
                    .putUTF(mTokenId)
                    .vPut(mToken)
                    .putUTF(mUsername)
                    .putUTF(mClientId)
                    .putUTF(mRefreshToken);
    }

}