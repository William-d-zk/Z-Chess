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

package com.isahl.chess.knight.cluster.config;

import com.isahl.chess.king.base.features.ICode;

import java.util.Objects;

public enum CodeKnight
        implements ICode
{
    CLUSTER_NO_IN_CONGRESS(KnightCode.CLUSTER_NO_IN_CONGRESS, " 一致性请求失败 input: [ %s ]"),
    CLUSTER_ELECTING(KnightCode.CLUSTER_ELECTING, "集群正在选举,当前不可用");

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

    CodeKnight(int code, String formatter)
    {
        this._Code = code;
        this._Formatter = formatter;
    }

}
