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

package com.isahl.chess.referee.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Objects;

@Component
public class JwtTokenUtil
        implements Serializable
{
    @Serial
    private static final long serialVersionUID = 788687777265936249L;
    private final        Key  _Key             = Keys.secretKeyFor(SignatureAlgorithm.HS512);

    public String generateToken(UserDetails userDetails)
    {
        return Jwts.builder()
                   .setSubject(userDetails.getUsername())
                   .setExpiration(Date.from(LocalDateTime.now()
                                                         .plusDays(5)
                                                         .toInstant(ZoneOffset.UTC)))
                   .signWith(_Key)
                   .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails)
    {
        Objects.requireNonNull(token);
        return Objects.requireNonNull(userDetails)
                      .getUsername()
                      .equals(getUsernameFromToken(token)) && !isTokenExpired(token);
    }

    public String getUsernameFromToken(String token)
    {
        return getClaimsFromToken(token).getSubject();
    }

    public Date getExpirationDateFromToken(String token)
    {
        return getClaimsFromToken(token).getExpiration();
    }

    public Boolean isTokenExpired(String token)
    {
        return getExpirationDateFromToken(token).before(new Date(Instant.now()
                                                                        .toEpochMilli()));
    }

    private Claims getClaimsFromToken(String token)
    {
        return Jwts.parserBuilder()
                   .requireAudience("isahl")
                   .setSigningKey(_Key)
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }
}
