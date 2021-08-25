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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.isahl.chess.referee.security.jpa.model.Status;
import com.isahl.chess.rook.storage.db.model.AuditModel;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author william.d.zk
 * @date 2021/3/5
 */
@Entity(name = "oauth_approvals")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Table(schema = "z_chess_security")
public class OauthApprovals
        extends AuditModel
        implements Serializable
{
    @Serial
    private static final long serialVersionUID = 4281980171453236833L;

    public static int SERIAL_OAUTH_APPROVALS = OauthAccessToken.SERIAL_OAUTH_ACCESS_TOKEN + 1;

    @Id
    private String        clientId;
    @Column(nullable = false)
    private String        scope;
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Status        status = Status.COMMON;
    @Column(nullable = false,
            name = "invalid_at")
    private LocalDateTime invalidAt;

    @Override
    public int serial()
    {
        return SERIAL_OAUTH_APPROVALS;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public LocalDateTime getInvalidAt()
    {
        return invalidAt;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public void setInvalidAt(LocalDateTime invalidAt)
    {
        this.invalidAt = invalidAt;
    }
}
