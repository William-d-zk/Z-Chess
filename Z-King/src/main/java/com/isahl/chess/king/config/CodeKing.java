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

package com.isahl.chess.king.config;

import com.isahl.chess.king.base.features.ICode;

import java.util.Objects;

/**
 * @author William.d.zk
 */
public enum CodeKing
        implements ICode
{

    PLAIN_UNSUPPORTED(KingCode.PLAIN_UNSUPPORTED, "不支持明文"),
    SYMMETRIC_KEY_OK(KingCode.SYMMETRIC_KEY_OK, "对称秘钥成功"),
    ILLEGAL_PARAM(KingCode.ILLEGAL_PARAM, "非法参数 %s"),
    SUCCESS(KingCode.SUCCESS, "成功"),
    FORBIDDEN(KingCode.FORBIDDEN, "禁止访问 %s"),
    UNAUTHORIZED(KingCode.UNAUTHORIZED, "无权访问 %s"),
    MISS(KingCode.MISS, "访问的资源不存在 %s"),
    ERROR(KingCode.ERROR, "错误[ %s ]"),
    UNKNOWN(KingCode.UNKNOWN, "未知错误[ %s ]"),
    LOCAL_FILE_SYSTEM_WRITE_FAILED(KingCode.LOCAL_FILE_SYSTEM_WRITE_FAILED, "本地文件写入错误[ %s ]");

    private final int    _Code;
    private final String _Formatter;

    @Override
    public int getCode(Object... condition)
    {
        return _Code;
    }

    @Override
    public String format(Object... args)
    {
        return Objects.isNull(args) || args.length == 0 ? _Formatter : String.format(_Formatter, args);
    }

    @Override
    public String formatter()
    {
        return _Formatter;
    }

    CodeKing(int code, String formatter)
    {
        _Code = code;
        _Formatter = formatter;
    }
}
