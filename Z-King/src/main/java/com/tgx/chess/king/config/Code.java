/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.tgx.chess.king.config;

import java.util.Objects;

import com.tgx.chess.king.base.inf.ICode;

/**
 * @author William.d.zk
 */
public enum Code implements
                 ICode
{

    PLAIN_UNSUPPORTED(0xFF00, "不支持明文"),
    SYMMETRIC_KEY_OK(0xFF01, "对称秘钥成功"),
    ILLEGAL_PARAM(0x0010, "非法参数 %s"),
    SUCCESS(0x00FF, "成功"),
    FORBIDDEN(0x0101, "禁止访问 %s"),
    UNAUTHORIZED(0x0102, "无权访问 %s");

    private final int    _Code;
    private final String _Formatter;

    @Override
    public int getCode()
    {
        return _Code;
    }

    @Override
    public String format(Object... args)
    {
        return Objects.isNull(args) || args.length == 0 ?
                _Formatter:
                String.format(_Formatter, args);
    }

    public String getFormatter()
    {
        return _Formatter;
    }

    Code(int code, String formatter)
    {
        _Code = code;
        _Formatter = formatter;
    }
}
