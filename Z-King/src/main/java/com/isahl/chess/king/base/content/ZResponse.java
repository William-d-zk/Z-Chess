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

package com.isahl.chess.king.base.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.isahl.chess.king.base.features.ICode;
import com.isahl.chess.king.config.Code;

import java.io.Serializable;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZResponse<T>
        implements
        ICode,
        Serializable
{
    private final int           _Code;
    private final String        _Message;
    private final T             _Detail;
    private final String        _Formatter;
    private final LocalDateTime _DateTime;

    // @formatter:off
    @JsonCreator
    public ZResponse(@JsonProperty("code") int code,
                     @JsonProperty("message") String message,
                     @JsonProperty("detail") T detail,
                     @JsonProperty("formatter") String formatter,
                     @JsonProperty("date_time")
                     @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                     @JsonSerialize(using = LocalDateTimeSerializer.class) LocalDateTime dateTime)
    {
        _Code = code;
        _Message = message;
        _Detail = detail;
        _Formatter = formatter;
        _DateTime = dateTime;
    }
    // @formatter:on
    @Override
    public int getCode(Object... condition)
    {
        return _Code;
    }

    @Override
    public String format(Object... args)
    {
        return _Formatter == null ? null: String.format(_Formatter, args);
    }

    public String getMessage()
    {
        return _Message;
    }

    public T getDetail()
    {
        return _Detail;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public LocalDateTime getDateTime()
    {
        return _DateTime;
    }

    public static <E> ZResponse<E> success(E e)
    {
        return new ZResponse<>(Code.SUCCESS.getCode(),
                               Code.SUCCESS.format(),
                               e,
                               Code.SUCCESS.getFormatter(),
                               LocalDateTime.now());
    }

    public static <E> ZResponse<E> forbid(E e)
    {
        return new ZResponse<>(Code.FORBIDDEN.getCode(),
                               Code.FORBIDDEN.format(e),
                               e,
                               Code.FORBIDDEN.getFormatter(),
                               LocalDateTime.now());
    }

    public static <E> ZResponse<E> unauthorized(E e)
    {
        return new ZResponse<>(Code.UNAUTHORIZED.getCode(),
                               Code.UNAUTHORIZED.format(e),
                               e,
                               Code.UNAUTHORIZED.getFormatter(),
                               LocalDateTime.now());
    }

    public static ZResponse<Void> error(String message)
    {
        return error(Code.ERROR.getCode(), message, null, null);
    }

    public static ZResponse<Void> error(int code, String message)
    {
        return error(code, message, null, null);
    }

    public static <E> ZResponse<E> error(int code, String message, E detail, String formatter)
    {
        return new ZResponse<>(code, message, detail, formatter, LocalDateTime.now());
    }

    public int getCode()
    {
        return _Code;
    }
}
